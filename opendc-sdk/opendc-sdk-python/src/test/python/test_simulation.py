"""`simulate()` returns every table the runner exports, from specs, DSL builders, and DataFrame or parquet traces."""

import re
import shutil
from pathlib import Path
from typing import Any

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq
import pytest

import opendc
from fixtures import (
    RESOURCES,
    minute_export,
    single_task,
    tiny_experiment,
    tiny_scenario,
    tiny_topology,
    tiny_workload,
    trace_host,
)
from opendc import ghz, gib, minutes

pytestmark = pytest.mark.usefixtures("scratch")

TEN_MINUTE_EXPORT = opendc.ExportSpec(export_interval=minutes(10), print_frequency=None)

NO_CARBON = opendc.PowerSourceSpec()


def trace_scenario(
    workload: opendc.KotlinObject, power_source: opendc.KotlinObject = NO_CARBON, **settings: object
) -> opendc.KotlinObject:
    """A scenario that runs `workload` on one host as large as the one the runner's tests use."""
    data_center = opendc.DataCenterSpec([opendc.ClusterSpec(name="C01", hosts=[trace_host])], power_source=power_source)
    return opendc.ScenarioSpec(
        opendc.TopologySpec([data_center]),
        workload,
        opendc.prefab_scheduler(),
        export_model=TEN_MINUTE_EXPORT,
        **settings,
    )


def test_every_exported_table_comes_back_with_its_run() -> None:
    results = opendc.simulate(tiny_experiment())

    assert set(results) == {"battery", "cluster", "dataCenter", "host", "meta", "powerSource", "service", "task"}
    assert set(results.meta) == {"battery", "cluster", "dataCenter", "host", "powerSource", "simulation", "task"}
    assert all(list(results[table].columns[:2]) == ["scenario", "seed"] for table in ["host", "service", "task"])
    assert results.power_source is results["powerSource"]
    assert len(results.host) == 11
    assert results.service["tasks_completed"].iloc[-1] == 1


def test_each_run_of_each_scenario_is_kept_apart() -> None:
    with opendc.experiment() as experiment:
        experiment.topology(tiny_topology)
        experiment.workload(tiny_workload)
        experiment.export_model(minute_export)
        experiment.allocation_policy(opendc.prefab_scheduler("Mem"))
        experiment.allocation_policy(opendc.prefab_scheduler("CoreMem"))
        experiment.runs = 2

    results = opendc.simulate(experiment)

    assert results.host.groupby(["scenario", "seed"]).size().to_dict() == {
        (0, 0): 11,
        (0, 1): 11,
        (1, 0): 11,
        (1, 1): 11,
    }
    assert results.scenarios.index.tolist() == [(0, 0), (0, 1), (1, 0), (1, 1)]
    assert results.scenarios.loc[1, "allocation_policy"].tolist() == [opendc.prefab_scheduler("CoreMem")] * 2


def test_runs_with_and_without_gpus_share_one_table() -> None:
    with opendc.topology() as gpu_topology:
        with gpu_topology.datacenter() as dc, dc.cluster() as cluster, cluster.host() as host:
            host.cpu(1, ghz(2))
            host.memory(gib(4))
            host.gpu(1, ghz(1))

    results = opendc.simulate(
        opendc.ExperimentSpec(
            topologies=[tiny_topology, gpu_topology], workloads=[tiny_workload], export_models=[minute_export]
        )
    )

    gpu_columns = [column for column in results.host.columns if column.startswith("gpu_")]
    assert gpu_columns
    assert results.host.loc[results.host["scenario"] == 0, gpu_columns].isna().all().all()
    assert results.host.loc[results.host["scenario"] == 1, gpu_columns].notna().all().all()


def test_a_scenario_or_a_builder_simulates_like_an_experiment() -> None:
    assert opendc.simulate(tiny_scenario).host["scenario"].unique().tolist() == [-1]

    with opendc.scenario() as scenario:
        scenario.experiment_name = "baseline"
        scenario.topology(tiny_topology)
        scenario.workload(tiny_workload)
        scenario.export_model = minute_export

    results = opendc.simulate(scenario)
    assert results.scenarios["experiment_name"].tolist() == ["baseline"]
    assert len(results.host) == 11


@pytest.mark.parametrize("name", ["../../escape", "/absolute", "C:colon"])
def test_names_never_become_paths(name: str) -> None:
    assert len(opendc.simulate(tiny_experiment(name=name)).host) == 11
    assert len(opendc.simulate(tiny_scenario.copy(experiment_name=name)).host) == 11


def carbon_scenario(carbon: opendc.KotlinObject) -> opendc.KotlinObject:
    """The runner's single-task trace on a power source with the carbon model `carbon`."""
    return trace_scenario(
        opendc.trace_workload(opendc.NamedReference("single")), power_source=opendc.PowerSourceSpec(carbon=carbon)
    )


def test_dataframe_traces_drive_the_simulation() -> None:
    scenario = carbon_scenario(opendc.TraceBasedCarbonSpec(opendc.NamedReference("carbon")))
    carbon = opendc.CarbonTrace(pd.DataFrame({"timestamp": [pd.Timestamp("2022-01-01")], "carbon_intensity": [100.0]}))

    results = opendc.simulate(scenario, traces={"single": single_task(), "carbon": carbon})

    assert results.task["task_state"].iloc[-1] == "COMPLETED"
    assert results.power_source["carbon_intensity"].unique().tolist() == [100.0]


def test_an_inline_carbon_trace_needs_no_file() -> None:
    intensity = opendc.CarbonIntensitySpec(opendc.Timestamp.ofEpochMs(1_640_995_200_000), 80.0)
    scenario = carbon_scenario(opendc.InlineTraceBasedCarbonSpec([intensity]))

    results = opendc.simulate(scenario, traces={"single": single_task()})

    assert results.power_source["carbon_intensity"].unique().tolist() == [80.0]


def test_a_failure_trace_fails_the_tasks() -> None:
    scenario = trace_scenario(
        opendc.trace_workload(opendc.NamedReference("single")),
        failure_model=opendc.TraceBasedFailureSpec(opendc.NamedReference("failures")),
    )
    failures = opendc.FailureTrace(pd.read_parquet(RESOURCES / "failureTraces/single_failure.parquet"))

    results = opendc.simulate(scenario, traces={"single": single_task(), "failures": failures})

    assert results.task["num_failures"].max() > 0


def test_parquet_traces_simulate_like_dataframes() -> None:
    scenario = trace_scenario(opendc.trace_workload(opendc.NamedReference("single")))
    from_files = opendc.WorkloadTrace.from_parquet(RESOURCES / "workloadTraces/single_task")

    results = opendc.simulate(scenario, traces={"single": from_files})

    pd.testing.assert_frame_equal(results.task, opendc.simulate(scenario, traces={"single": single_task()}).task)


def test_a_table_split_into_part_files_is_read_where_it_is(tmp_path: Path) -> None:
    split = tmp_path / "split"
    write_split_trace(split, pa.int32())
    trace = opendc.WorkloadTrace.from_parquet(split)

    results = opendc.simulate(
        trace_scenario(opendc.trace_workload(opendc.NamedReference("split"))), traces={"split": trace}
    )

    assert trace.stage(tmp_path / "unused") == split
    assert results.service["tasks_completed"].iloc[-1] == 2


def test_a_table_split_into_part_files_that_must_be_cast_is_staged_and_removed(tmp_path: Path) -> None:
    split = tmp_path / "split"
    write_split_trace(split, pa.int64())
    trace = opendc.WorkloadTrace.from_parquet(split)

    # The scratch fixture then sees the staged copy removed, which Windows refuses while the JVM holds it open
    results = opendc.simulate(
        trace_scenario(opendc.trace_workload(opendc.NamedReference("split"))), traces={"split": trace}
    )

    assert trace.stage(tmp_path / "cast") == tmp_path / "cast"
    assert results.service["tasks_completed"].iloc[-1] == 2


def write_split_trace(target: Path, id_type: pa.DataType) -> None:
    """Writes the runner's single task as tasks 0 and 1 to `target`, with a part file per task in `tasks.parquet`."""
    source = RESOURCES / "workloadTraces/single_task"
    tasks = pq.read_table(source / "tasks.parquet")
    fragments = pq.read_table(source / "fragments.parquet")
    (target / "tasks.parquet").mkdir(parents=True)

    for task_id in (0, 1):
        pq.write_table(with_task_id(tasks, task_id, id_type), target / "tasks.parquet" / f"part-{task_id}.parquet")
    both_fragments = pa.concat_tables([with_task_id(fragments, 0, id_type), with_task_id(fragments, 1, id_type)])
    pq.write_table(both_fragments, target / "fragments.parquet")


def with_task_id(table: pa.Table, task_id: int, id_type: pa.DataType) -> pa.Table:
    index = table.schema.get_field_index("id")
    field = table.schema.field(index).with_type(id_type)
    return table.set_column(index, field, pa.array([task_id] * len(table), id_type))


def test_a_file_with_columns_the_sdk_does_not_read_is_read_where_it_is(tmp_path: Path) -> None:
    source = RESOURCES / "workloadTraces/single_task"
    target = tmp_path / "trace"
    target.mkdir()
    shutil.copy(source / "tasks.parquet", target / "tasks.parquet")
    # OpenDC's trace format 2.1 holds a cpu_count in its fragments, which the SDK does not read
    fragments = pq.read_table(source / "fragments.parquet")
    pq.write_table(fragments.append_column("cpu_count", pa.array([1], pa.int32())), target / "fragments.parquet")
    trace = opendc.WorkloadTrace.from_parquet(target)

    results = opendc.simulate(
        trace_scenario(opendc.trace_workload(opendc.NamedReference("trace"))), traces={"trace": trace}
    )

    assert trace.stage(tmp_path / "unused") == target
    assert results.service["tasks_completed"].iloc[-1] == 1


def test_the_efficient_loader_reads_a_dataframe_trace() -> None:
    results = opendc.simulate(
        trace_scenario(opendc.EfficientTraceWorkloadSpec(opendc.NamedReference("single"))),
        traces={"single": single_task(num_fragments=1)},
    )

    assert results.service["tasks_completed"].iloc[-1] == 1


def test_a_uri_reference_resolves_to_the_trace_passed_under_it() -> None:
    results = opendc.simulate(
        trace_scenario(opendc.trace_workload(opendc.UriReference("file:///data/single"))),
        traces={"file:///data/single": single_task()},
    )

    assert results.service["tasks_completed"].iloc[-1] == 1


def test_a_reference_that_was_not_passed_is_read_under_the_input_root() -> None:
    scenario = trace_scenario(opendc.trace_workload(opendc.NamedReference("workloadTraces/single_task")))

    results = opendc.simulate(scenario, input_root=RESOURCES)

    assert results.service["tasks_completed"].iloc[-1] == 1


def test_the_input_root_defaults_to_the_working_directory(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.chdir(RESOURCES / "workloadTraces")

    results = opendc.simulate(trace_scenario(opendc.trace_workload(opendc.NamedReference("single_task"))))

    assert results.service["tasks_completed"].iloc[-1] == 1


def test_a_file_uri_reads_the_trace_it_names() -> None:
    uri = (RESOURCES / "workloadTraces/single_task").absolute().as_uri()

    results = opendc.simulate(trace_scenario(opendc.trace_workload(opendc.UriReference(uri))))

    assert results.service["tasks_completed"].iloc[-1] == 1


def test_a_passed_trace_wins_over_the_file_of_its_name() -> None:
    scenario = trace_scenario(opendc.trace_workload(opendc.NamedReference("workloadTraces/single_task")))
    tasks = pd.read_parquet(RESOURCES / "workloadTraces/single_task/tasks.parquet")
    fragments = pd.read_parquet(RESOURCES / "workloadTraces/single_task/fragments.parquet")
    twice = opendc.WorkloadTrace(pd.concat([tasks, tasks.assign(id=1)]), pd.concat([fragments, fragments.assign(id=1)]))

    results = opendc.simulate(scenario, traces={"workloadTraces/single_task": twice}, input_root=RESOURCES)

    assert results.service["tasks_completed"].iloc[-1] == 2


def test_a_trace_that_is_neither_passed_nor_on_disk_is_named(tmp_path: Path) -> None:
    scenario = trace_scenario(opendc.trace_workload(opendc.NamedReference("single")))
    missing = re.escape(str(tmp_path / "single"))

    with pytest.raises(Exception, match=rf"{missing}; the traces passed are \[\]"):
        opendc.simulate(scenario, input_root=tmp_path)


def test_an_experiment_read_from_json_simulates_like_the_spec(tmp_path: Path) -> None:
    document = tmp_path / "experiment.json"
    document.write_text(opendc.SdkJson.encode_to_string(tiny_experiment()))

    experiment = opendc.SdkJson.decode_experiment(document.read_text())

    assert experiment == tiny_experiment()
    pd.testing.assert_frame_equal(opendc.simulate(experiment).host, opendc.simulate(tiny_experiment()).host)


def test_an_invalid_spec_is_refused_with_every_issue() -> None:
    with pytest.raises(ValueError, match="topologies: must not be empty"):
        opendc.simulate(opendc.ExperimentSpec(topologies=[], workloads=[tiny_workload]))


def test_only_experiments_and_scenarios_simulate() -> None:
    with pytest.raises(TypeError, match="an ExperimentSpec or a ScenarioSpec"):
        opendc.simulate(opendc.topology())
    with pytest.raises(TypeError, match="an ExperimentSpec or a ScenarioSpec"):
        opendc.simulate(tiny_topology)


def test_traces_are_passed_wrapped_in_their_kind() -> None:
    # Typed loosely, as an untyped caller passes it, since the type checker refuses a DataFrame here
    traces: dict[str, Any] = {"carbon": pd.DataFrame({"timestamp": [0], "carbon_intensity": [100.0]})}

    with pytest.raises(TypeError, match="not 'carbon' to a DataFrame"):
        opendc.simulate(tiny_scenario, traces=traces)


def test_a_trace_of_another_kind_than_its_reference_is_refused() -> None:
    carbon = opendc.CarbonTrace(pd.DataFrame({"timestamp": [pd.Timestamp("2022-01-01")], "carbon_intensity": [100.0]}))
    experiment = opendc.ExperimentSpec(
        topologies=[tiny_topology], workloads=[opendc.trace_workload(opendc.NamedReference("nightly"))]
    )
    expected = r"traces\['nightly'\] is a carbon trace, but workloads\[0\]\.source references a workload trace"

    with pytest.raises(ValueError, match=expected):
        opendc.simulate(experiment, traces={"nightly": carbon})
