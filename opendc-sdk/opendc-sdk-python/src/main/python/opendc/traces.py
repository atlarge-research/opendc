"""The traces passed to `simulate()`, as pandas DataFrames or parquet files."""

from __future__ import annotations

import os
from collections.abc import Mapping
from dataclasses import dataclass
from pathlib import Path
from typing import Any, ClassVar, Self

import jpype
import pandas as pd
import pyarrow as pa

from opendc.bridge import KotlinObject, sdk, to_java
from opendc.trace_tables import Table, contract_schema, reads_in_place, stage_table, write_file, write_parts

__all__ = ["CarbonTrace", "FailureTrace", "Table", "Trace", "WorkloadTrace", "check_roles", "check_traces", "stage"]


@dataclass(frozen=True, eq=False)
class WorkloadTrace:
    """A workload trace: its tasks, and the execution fragments of those tasks."""

    kind: ClassVar[str] = "workload"

    tasks: Table
    fragments: Table

    @classmethod
    def from_parquet(cls, directory: str | os.PathLike[str]) -> WorkloadTrace:
        """The workload trace in `directory`: `tasks.parquet` and `fragments.parquet`, each a file or a directory."""
        return cls(Path(directory) / "tasks.parquet", Path(directory) / "fragments.parquet")

    @staticmethod
    def task_schema() -> pa.Schema:
        """The columns of the tasks, with their types; a non-nullable column is required."""
        return contract_schema("TASK")

    @staticmethod
    def fragment_schema() -> pa.Schema:
        """The columns of the fragments, with their types; a non-nullable column is required."""
        return contract_schema("FRAGMENT")

    def stage(self, target: Path) -> Path:
        """Returns where the SDK reads the trace: its own directory, or `target`, where it is written cast."""
        if not isinstance(self.tasks, pd.DataFrame) and not isinstance(self.fragments, pd.DataFrame):
            tasks, fragments = Path(self.tasks), Path(self.fragments)
            laid_out = tasks.name == "tasks.parquet" and fragments == tasks.parent / "fragments.parquet"
            if laid_out and reads_in_place(tasks, "TASK") and reads_in_place(fragments, "FRAGMENT"):
                return tasks.parent.absolute()

        target.mkdir()
        write_parts(self.tasks, "TASK", target / "tasks.parquet")
        write_parts(self.fragments, "FRAGMENT", target / "fragments.parquet")
        return target

    def write_tables(self, directory: Path) -> list[Path]:
        """Writes each table, cast, to one file in `directory` named for it: `tasks.parquet`, `fragments.parquet`."""
        tasks, fragments = directory / "tasks.parquet", directory / "fragments.parquet"
        write_file(self.tasks, "TASK", tasks)
        write_file(self.fragments, "FRAGMENT", fragments)
        return [tasks, fragments]


@dataclass(frozen=True, eq=False)
class SingleTableTrace:
    """A trace held in one table, which the SDK reads as the `TraceTable` named `trace_table`."""

    kind: ClassVar[str]
    trace_table: ClassVar[str]
    file_name: ClassVar[str]

    table: Table

    @classmethod
    def from_parquet(cls, path: str | os.PathLike[str]) -> Self:
        """The trace in the parquet file, or directory of parquet files, at `path`."""
        return cls(Path(path))

    @classmethod
    def schema(cls) -> pa.Schema:
        """The columns of the trace, with their types; a non-nullable column is required."""
        return contract_schema(cls.trace_table)

    def stage(self, target: Path) -> Path:
        """Returns where the SDK reads the trace: where it is, or `target`, where it is written cast."""
        return stage_table(self.table, self.trace_table, target)

    def write_tables(self, directory: Path) -> list[Path]:
        """Writes the table, cast, to the one file in `directory` named for the trace, such as `carbon.parquet`."""
        file = directory / self.file_name
        write_file(self.table, self.trace_table, file)
        return [file]


class CarbonTrace(SingleTableTrace):
    """A carbon intensity trace."""

    kind = "carbon"
    trace_table = "CARBON"
    file_name = "carbon.parquet"


class FailureTrace(SingleTableTrace):
    """A failure trace."""

    kind = "failure"
    trace_table = "FAILURE"
    file_name = "failures.parquet"


Trace = WorkloadTrace | CarbonTrace | FailureTrace
"""A trace that a spec references, by the name of a `NamedReference` or the URI of a `UriReference`."""


def check_traces(traces: Mapping[str, Trace]) -> None:
    """Raises unless `traces` maps names or URIs to traces."""
    for key, trace in traces.items():
        if not isinstance(key, str) or not isinstance(trace, Trace):
            raise TypeError(
                f"traces maps the name or URI a spec references to a WorkloadTrace, CarbonTrace or FailureTrace, "
                f"not {key!r} to a {type(trace).__name__}"
            )


def check_roles(traces: Mapping[str, Trace], uses: list[KotlinObject]) -> None:
    """Raises if a trace in `traces` is of another kind than the role of a `ResourceUse` in `uses` of its key."""
    for use in uses:
        reference = use.reference
        key = reference.name if isinstance(reference, sdk.NamedReference) else reference.uri
        if key not in traces:
            continue

        kind, role = traces[key].kind, use.role.name.lower()
        if kind != role:
            raise ValueError(f"traces[{key!r}] is a {kind} trace, but {use.path} references a {role} trace")


def stage(traces: Mapping[str, Trace], directory: Path, input_root: Path) -> Any:
    """Stages `traces` under `directory`, returning a provisioner that reads any other reference under `input_root`."""
    directory.mkdir()
    locations = {
        key: str(trace.stage(directory / f"trace-{index}")) for index, (key, trace) in enumerate(traces.items())
    }

    on_disk = sdk.FileSystemResourceProvisioner(input_root)
    return jpype.JClass("org.opendc.sdk.python.StagedTraces")(to_java(locations), on_disk._jvm)
