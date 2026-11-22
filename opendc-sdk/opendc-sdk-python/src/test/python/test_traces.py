"""Traces pass as DataFrames or parquet files, cast to the schemas the SDK reads unless a file already fits them."""

from datetime import UTC, datetime
from pathlib import Path

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq
import pytest

import opendc
from fixtures import RESOURCES


def test_schemas_are_those_the_sdk_reads() -> None:
    tasks = opendc.WorkloadTrace.task_schema()

    assert [field.name for field in tasks if not field.nullable] == [
        "id",
        "submission_time",
        "duration",
        "cpu_count",
        "cpu_capacity",
        "mem_capacity",
    ]
    assert tasks.field("submission_time").type == pa.timestamp("ms", tz="UTC")
    assert opendc.WorkloadTrace.fragment_schema().names == ["id", "duration", "cpu_usage", "gpu_usage"]
    # The readers of carbon and failure traces read every column
    assert opendc.CarbonTrace.schema().names == ["timestamp", "carbon_intensity"]
    assert opendc.FailureTrace.schema().names == ["failure_interval", "failure_duration", "failure_intensity"]
    assert not any(field.nullable for field in [*opendc.CarbonTrace.schema(), *opendc.FailureTrace.schema()])


def test_pandas_types_are_cast_to_those_of_the_schema(tmp_path: Path) -> None:
    tasks = pd.DataFrame(
        {
            "parents": [[], [0]],
            "id": [0, 1],
            "submission_time": pd.to_datetime(["2024-02-01 10:00", "2024-02-01 10:05"]),
            "duration": pd.to_timedelta(["10min", "5min"]),
            "cpu_count": [1, 2],
            "cpu_capacity": [1000.0, 2000.0],
            "mem_capacity": [1024, 2048],
        }
    )
    fragments = pd.DataFrame({"id": [0, 1], "duration": [600_000, 300_000], "cpu_usage": [1000.0, 2000.0]})

    opendc.WorkloadTrace(tasks, fragments).stage(tmp_path / "trace")

    written = pq.read_table(tmp_path / "trace" / "tasks.parquet")
    assert written.schema.names == [
        "id",
        "submission_time",
        "duration",
        "cpu_count",
        "cpu_capacity",
        "mem_capacity",
        "parents",
    ]
    assert written.schema.field("cpu_count").type == pa.int32()
    assert written.column("submission_time").to_pylist()[1] == datetime(2024, 2, 1, 10, 5, tzinfo=UTC)
    assert written.column("duration").to_pylist() == [600_000, 300_000]
    assert written.column("parents").to_pylist() == [[], [0]]


def test_epoch_milliseconds_are_taken_as_timestamps(tmp_path: Path) -> None:
    carbon = pd.DataFrame({"timestamp": [1_640_995_200_000], "carbon_intensity": [100.0]})

    opendc.CarbonTrace(carbon).stage(tmp_path / "carbon.parquet")

    written = pq.read_table(tmp_path / "carbon.parquet")
    assert written.column("timestamp").to_pylist() == [datetime(2022, 1, 1, tzinfo=UTC)]


def test_timedeltas_are_taken_as_times_since_the_epoch(tmp_path: Path) -> None:
    tasks = one_task().assign(submission_time=pd.to_timedelta(["5min"]))
    fragments = pd.DataFrame({"id": [0], "duration": [600_000], "cpu_usage": [1000.0]})

    opendc.WorkloadTrace(tasks, fragments).stage(tmp_path / "trace")

    written = pq.read_table(tmp_path / "trace" / "tasks.parquet")
    assert written.column("submission_time").to_pylist() == [datetime(1970, 1, 1, 0, 5, tzinfo=UTC)]


def test_every_column_of_a_carbon_trace_is_required(tmp_path: Path) -> None:
    timestamps = pd.to_datetime(["2022-01-01 00:00", "2022-01-01 01:00"], utc=True)
    no_intensity = pd.DataFrame({"timestamp": timestamps})
    gaps = pd.DataFrame({"timestamp": timestamps, "carbon_intensity": [100.0, float("nan")]})

    with pytest.raises(ValueError, match=r"lacks the columns \['carbon_intensity'\]"):
        opendc.CarbonTrace(no_intensity).stage(tmp_path / "carbon.parquet")
    with pytest.raises(ValueError, match=r"missing values in the required columns \['carbon_intensity'\]"):
        opendc.CarbonTrace(gaps).stage(tmp_path / "carbon.parquet")


def one_task(task_id: int = 0) -> pd.DataFrame:
    return pd.DataFrame(
        {
            "id": [task_id],
            "submission_time": [0],
            "duration": [600_000],
            "cpu_count": [1],
            "cpu_capacity": [1000.0],
            "mem_capacity": [1024],
        }
    )


def test_missing_and_unknown_columns_are_named(tmp_path: Path) -> None:
    fragments = pd.DataFrame({"id": [0], "duration": [600_000], "cpu": [1000.0]})

    with pytest.raises(ValueError, match=r"lacks the columns \['cpu_usage'\] and has the unknown columns \['cpu'\]"):
        opendc.WorkloadTrace(one_task(), fragments).stage(tmp_path / "trace")


def test_values_that_do_not_fit_their_column_are_named(tmp_path: Path) -> None:
    fragments = pd.DataFrame({"id": [0], "duration": [600_000], "cpu_usage": [1000.0]})

    with pytest.raises(ValueError, match="'id' cannot be a int32"):
        opendc.WorkloadTrace(one_task(2**40), fragments).stage(tmp_path / "trace")


def fragments_of(*task_ids: int) -> pd.DataFrame:
    return pd.DataFrame(
        {"id": list(task_ids), "duration": [600_000] * len(task_ids), "cpu_usage": [1000.0] * len(task_ids)}
    )


def test_files_the_sdk_reads_as_they_are_are_read_where_they_are(tmp_path: Path) -> None:
    workload = RESOURCES / "workloadTraces" / "single_task"
    carbon = RESOURCES / "carbonTraces" / "2022-01-01_2022-12-31_NL.parquet"

    assert opendc.WorkloadTrace.from_parquet(workload).stage(tmp_path / "workload") == workload.absolute()
    assert opendc.CarbonTrace.from_parquet(str(carbon)).stage(tmp_path / "carbon") == carbon.absolute()
    assert list(tmp_path.iterdir()) == []


def test_files_holding_other_types_are_written_cast(tmp_path: Path) -> None:
    source = tmp_path / "source"
    source.mkdir()
    # pandas writes durations in microseconds, which the SDK would read as milliseconds
    one_task().assign(duration=pd.to_timedelta(["10min"])).to_parquet(source / "tasks.parquet")
    fragments_of(0).assign(duration=pd.to_timedelta(["10min"])).to_parquet(source / "fragments.parquet")

    staged = opendc.WorkloadTrace.from_parquet(source).stage(tmp_path / "trace")

    assert staged == tmp_path / "trace"
    fragments = pq.read_table(staged / "fragments.parquet")
    assert fragments.schema.field("id").type == pa.int32()
    assert fragments.column("duration").to_pylist() == [600_000]


def test_lists_nested_otherwise_than_the_sdk_reads_them_are_written_cast(tmp_path: Path) -> None:
    tasks = pd.concat([one_task(0), one_task(1)]).assign(parents=[[], [0]])
    written = opendc.WorkloadTrace(tasks, fragments_of(0, 1)).stage(tmp_path / "written")
    legacy = tmp_path / "legacy"
    write_with_legacy_nesting(written, legacy)
    assert pq.read_schema(legacy / "tasks.parquet") == pq.read_schema(written / "tasks.parquet")

    assert opendc.WorkloadTrace.from_parquet(written).stage(tmp_path / "again") == written
    assert opendc.WorkloadTrace.from_parquet(legacy).stage(tmp_path / "cast") == tmp_path / "cast"


def write_with_legacy_nesting(trace: Path, target: Path) -> None:
    """Copies the workload trace in `trace` to `target`, its lists nested the legacy way under the same arrow schema."""
    # The legacy nesting names a list's element as its arrow field is
    target.mkdir()
    rows = pq.read_table(trace / "tasks.parquet")
    parents = rows.schema.field("parents")
    legacy_parents = parents.with_type(pa.list_(parents.type.value_field.with_name("item")))
    rows = rows.set_column(rows.schema.get_field_index("parents"), legacy_parents, rows.column("parents"))

    pq.write_table(rows, target / "tasks.parquet", use_compliant_nested_type=False)
    pq.write_table(pq.read_table(trace / "fragments.parquet"), target / "fragments.parquet")


def test_a_table_split_into_part_files_is_written_part_by_part(tmp_path: Path) -> None:
    source = tmp_path / "source"
    (source / "tasks.parquet").mkdir(parents=True)
    for task_id in (0, 1):
        one_task(task_id).to_parquet(source / "tasks.parquet" / f"part-{task_id}.parquet")
    fragments_of(0, 1).to_parquet(source / "fragments.parquet")

    staged = opendc.WorkloadTrace.from_parquet(source).stage(tmp_path / "trace")

    parts = sorted((staged / "tasks.parquet").iterdir())
    assert [part.name for part in parts] == ["part-0.parquet", "part-1.parquet"]
    assert [pq.read_table(part).column("id").to_pylist() for part in parts] == [[0], [1]]
    assert pq.read_schema(parts[0]).field("id").type == pa.int32()


def test_columns_the_sdk_does_not_read_are_dropped_when_cast(tmp_path: Path) -> None:
    source = tmp_path / "source"
    (source / "fragments.parquet").mkdir(parents=True)
    # pandas writes int64 ids, which have to be cast
    pd.concat([one_task(0), one_task(1)]).to_parquet(source / "tasks.parquet")
    for task_id in (0, 1):
        fragments_of(task_id).assign(cpu_count=[1]).to_parquet(source / "fragments.parquet" / f"part-{task_id}.parquet")
    trace = opendc.WorkloadTrace.from_parquet(source)

    staged = trace.stage(tmp_path / "staged")
    [_, written] = trace.write_tables(tmp_path)

    read = ["id", "duration", "cpu_usage"]
    assert [pq.read_schema(part).names for part in sorted((staged / "fragments.parquet").iterdir())] == [read, read]
    assert pq.read_table(written).to_pydict() == {"id": [0, 1], "duration": [600_000] * 2, "cpu_usage": [1000.0] * 2}


def test_a_file_names_itself_in_its_errors(tmp_path: Path) -> None:
    pd.DataFrame({"timestamp": [0], "intensity": [100.0]}).to_parquet(tmp_path / "renamed.parquet")
    pd.DataFrame({"timestamp": [0, 1], "carbon_intensity": [100.0, None]}).to_parquet(tmp_path / "gaps.parquet")

    with pytest.raises(FileNotFoundError, match="no parquet file or directory at"):
        opendc.CarbonTrace.from_parquet(tmp_path / "missing.parquet").stage(tmp_path / "staged")
    with pytest.raises(ValueError, match=r"in .*renamed\.parquet lacks the columns \['carbon_intensity'\]"):
        opendc.CarbonTrace.from_parquet(tmp_path / "renamed.parquet").stage(tmp_path / "staged")
    with pytest.raises(ValueError, match=r"in .*gaps\.parquet has missing values in the required columns"):
        opendc.CarbonTrace.from_parquet(tmp_path / "gaps.parquet").stage(tmp_path / "staged")


def test_a_file_that_fails_to_cast_is_closed_while_the_failure_is_held(tmp_path: Path) -> None:
    single = tmp_path / "single.parquet"
    split = tmp_path / "split"
    split.mkdir()
    for file in (single, split / "part-0.parquet"):
        pd.DataFrame({"timestamp": ["noon"], "carbon_intensity": [100.0]}).to_parquet(file)

    with pytest.raises(ValueError, match="'timestamp' cannot be a timestamp") as staging:
        opendc.CarbonTrace.from_parquet(single).stage(tmp_path / "staged")
    with pytest.raises(ValueError, match="'timestamp' cannot be a timestamp") as writing:
        opendc.CarbonTrace.from_parquet(split).write_tables(tmp_path)

    # Windows refuses to delete a file that is still open
    single.unlink()
    (split / "part-0.parquet").unlink()
    assert str(single) in str(staging.value)
    assert str(split) in str(writing.value)
