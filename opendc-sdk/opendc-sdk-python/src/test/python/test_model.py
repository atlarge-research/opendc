"""The SDK classes are reachable from Python as Kotlin declares them: by name, with Kotlin's defaults and types."""

import copy
import inspect
import subprocess
import sys
from pathlib import Path

import numpy as np
import pytest

import opendc
from fixtures import sample_topology, sample_workload, tiny_scenario
from opendc import ghz, gib, minutes, watts


def test_constructor_applies_the_kotlin_defaults() -> None:
    cpu = opendc.CpuSpec(core_count=8, core_speed=ghz(3))

    assert (cpu.count, cpu.vendor, cpu.model_name) == (1, "unknown", "unknown")
    assert cpu == opendc.CpuSpec(8, ghz(3))


def test_arguments_take_kotlin_or_snake_case_names() -> None:
    assert opendc.FixedSharePolicySpec(share_ratio=0.5) == opendc.FixedSharePolicySpec(shareRatio=0.5)
    assert opendc.FixedSharePolicySpec(0.5).share_ratio == 0.5


def test_names_that_differ_only_in_case_must_be_exact() -> None:
    with pytest.raises(AttributeError, match="toMiB"):
        gib(4).to_mib()

    assert gib(4).toMiB() == 4096.0
    assert ghz(2).to_mhz() == 2000.0


def test_copy_changes_only_the_given_properties() -> None:
    cpu = opendc.CpuSpec(core_count=8, core_speed=ghz(3))

    assert cpu.copy(count=2) == opendc.CpuSpec(core_count=8, core_speed=ghz(3), count=2)


def test_sdk_objects_do_not_change() -> None:
    cpu = opendc.CpuSpec(core_count=8, core_speed=ghz(3))

    with pytest.raises(AttributeError, match="copy"):
        cpu.count = 2


def test_unknown_or_missing_arguments_raise_type_errors() -> None:
    with pytest.raises(TypeError, match="has no 'cores'"):
        opendc.CpuSpec(cores=8, core_speed=ghz(3))
    with pytest.raises(TypeError, match="missing 'coreSpeed'"):
        opendc.CpuSpec(core_count=8)


def test_units_are_type_safe_and_never_none() -> None:
    with pytest.raises(TypeError, match="Frequency, not a java.lang.Double"):
        opendc.CpuSpec(core_count=8, core_speed=3000.0)
    with pytest.raises(TypeError, match="Frequency, not a org.opendc.common.units.DataSize"):
        opendc.CpuSpec(core_count=8, core_speed=gib(3))
    with pytest.raises(TypeError, match="not None"):
        opendc.CpuSpec(core_count=8, core_speed=None)


def test_kotlin_objects_enums_and_companions_are_values() -> None:
    assert repr(opendc.EqualSharePolicySpec) == "EqualSharePolicySpec"
    assert opendc.PrefabAllocationPolicySpec("CoreMem") == opendc.PrefabAllocationPolicySpec(
        opendc.SchedulerPrefabSpec.CoreMem
    )
    assert opendc.PowerModelSpec.DEFAULT == opendc.LinearPowerModelSpec(max_power=watts(400), idle_power=watts(200))
    assert opendc.Frequency.ofGHz(2) == ghz(2)


def test_a_companion_function_named_of_reaches_kotlin() -> None:
    with pytest.raises(TypeError, match="'host' must be a .*SimHost"):
        opendc.HostMeta.of(1)


def test_numpy_values_pass_as_the_python_values_they_hold() -> None:
    from_numpy = opendc.TaskSpec(
        np.int64(1),
        minutes(0),
        minutes(10),
        np.int16(2),
        ghz(np.float64(1.5)),
        gib(1),
        [],
        deferrable=np.bool_(True),
        parents=np.array([0]),
    )

    assert from_numpy == opendc.TaskSpec(
        1, minutes(0), minutes(10), 2, ghz(1.5), gib(1), [], deferrable=True, parents=[0]
    )


def test_java_paths_come_back_as_pathlib_paths() -> None:
    assert opendc.ParquetOutput("results/run-0").path == Path("results/run-0")


def test_sdk_objects_are_their_own_copies() -> None:
    cpu = opendc.CpuSpec(core_count=8, core_speed=ghz(3))

    assert copy.copy(cpu) is cpu
    assert copy.deepcopy({"cpu": cpu})["cpu"] is cpu


def test_isinstance_checks_the_kotlin_type() -> None:
    policy = opendc.PrefabAllocationPolicySpec()

    assert isinstance(policy, opendc.AllocationPolicySpec)
    assert not isinstance(policy, opendc.HostFilterSpec)


def test_operators_call_the_kotlin_operator_functions() -> None:
    assert minutes(5) * 2 == minutes(10)
    assert 2 * minutes(5) == minutes(10)
    assert ghz(1) + ghz(2) == ghz(3)
    assert ghz(1) < ghz(2) <= ghz(2)
    assert sorted([watts(3), watts(1), watts(2)]) == [watts(1), watts(2), watts(3)]
    assert -minutes(5) == minutes(-5)


def test_operators_without_a_kotlin_operator_function_raise_type_errors() -> None:
    cpu = opendc.CpuSpec(core_count=8, core_speed=ghz(3))

    with pytest.raises(TypeError, match="unary -: 'CpuSpec'"):
        _ = -cpu
    with pytest.raises(TypeError, match="unsupported operand"):
        _ = cpu + cpu


def test_top_level_functions_values_and_extensions_are_reachable() -> None:
    experiment = opendc.ExperimentSpec(
        topologies=[sample_topology],
        workloads=[sample_workload],
        allocation_policies=[opendc.PrefabAllocationPolicySpec(), opendc.PrefabAllocationPolicySpec("CoreMem")],
    )

    assert [scenario.id for scenario in experiment.expand()] == [0, 1]
    assert opendc.NO_DEADLINE == opendc.TimeDelta.ofMs(-1)
    assert opendc.generate_topology(3).datacenters[0].clusters[0].hosts[0].count == 3


def test_sdk_json_round_trips_an_experiment() -> None:
    experiment = opendc.ExperimentSpec(topologies=[sample_topology], workloads=[sample_workload], name="sweep")

    assert opendc.SdkJson.decode_experiment(opendc.SdkJson.encode_to_string(experiment)) == experiment


def test_signature_shows_the_constructor_in_snake_case() -> None:
    signature = inspect.signature(opendc.CpuSpec)

    assert list(signature.parameters)[:3] == ["core_count", "core_speed", "count"]
    assert signature.parameters["core_count"].default is inspect.Parameter.empty
    assert signature.parameters["count"].kind is inspect.Parameter.KEYWORD_ONLY


def test_dir_lists_sdk_classes_and_members() -> None:
    assert {"HostSpec", "OpenDC", "topology"} <= set(dir(opendc))
    assert {"copy", "coreSpeed"} <= set(dir(opendc.CpuSpec(8, ghz(3))))


def test_kotlin_exceptions_reach_python_with_their_message() -> None:
    with pytest.raises(Exception, match="parallelism must be between"):
        opendc.OpenDC.builder().parallelism(0)


def test_jvm_options_cannot_change_once_it_runs() -> None:
    with pytest.raises(RuntimeError, match="already runs"):
        opendc.start_jvm("-Xmx2g")


def test_listing_the_sdk_leaves_the_jvm_to_be_started_with_options() -> None:
    script = "import jpype, opendc; dir(opendc); print(jpype.isJVMStarted())"

    listed = subprocess.run([sys.executable, "-c", script], capture_output=True, text=True, check=True)

    assert listed.stdout.strip() == "False"


def test_runner_classes_simulate_a_scenario(tmp_path: Path) -> None:
    report = (
        opendc.OpenDC.builder()
        .provisioner(opendc.FileSystemResourceProvisioner(tmp_path))
        .sink(opendc.InMemorySink())
        .build()
        .simulate(tiny_scenario)
    )

    host_samples = report.scenarios[0].runs[0].metrics.host
    # A sample every simulated minute, and a last one when the run ends
    expected = [f"1970-01-01T00:{minute:02d}:00Z" for minute in range(1, 11)] + ["1970-01-01T00:10:00Z"]
    assert [repr(sample.timestamp) for sample in host_samples] == expected
    assert {sample.host_id for sample in host_samples} == {0}
