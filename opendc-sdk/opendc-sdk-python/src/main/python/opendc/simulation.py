"""Simulates SDK specs locally, and returns every table the runner exports as a pandas DataFrame."""

from __future__ import annotations

import inspect
import os
import shutil
import tempfile
from collections.abc import Iterator, Mapping
from pathlib import Path
from types import MappingProxyType
from typing import Any

import pandas as pd
import pyarrow.parquet as pq

from opendc.bridge import Builder, KotlinObject, sdk
from opendc.traces import Trace, check_roles, check_traces, stage

__all__ = ["NO_TRACES", "SimulationResults", "Tables", "expand", "read_tables", "scenario_frame", "simulate"]

NO_TRACES: Mapping[str, Trace] = MappingProxyType({})


def simulate(
    spec: KotlinObject | Builder,
    *,
    traces: Mapping[str, Trace] = NO_TRACES,
    input_root: str | os.PathLike[str] = ".",
    parallelism: int = 1,
) -> SimulationResults:
    """Simulates an `ExperimentSpec` or a `ScenarioSpec`, or the DSL builder of one, and returns what it exports."""
    if isinstance(spec, Builder):
        spec = spec.build()
    scenarios = expand(spec)
    check_traces(traces)
    check_roles(traces, spec.references())

    # Resolved here, as the working directory of the JVM is fixed when it starts
    root_on_disk = Path(input_root).absolute()

    with tempfile.TemporaryDirectory(prefix="opendc-") as directory:
        root = Path(directory)
        provisioner = stage(traces, root / "traces", root_on_disk)
        runner = sdk.OpenDC.builder().provisioner(provisioner).output(root / "output").parallelism(parallelism).build()
        # Every run writes under one blank experiment name, so that no name in the spec becomes a path
        runner.simulate("", scenarios)

        shutil.rmtree(root / "traces")
        tables = read_tables(root / "output" / "raw-output")

    return SimulationResults(tables, scenario_frame(scenarios))


def expand(spec: KotlinObject) -> list[KotlinObject]:
    """The scenarios a valid `ExperimentSpec` or `ScenarioSpec` runs, one per seed."""
    if not isinstance(spec, (sdk.ExperimentSpec, sdk.ScenarioSpec)):
        raise TypeError(f"Expected an ExperimentSpec or a ScenarioSpec, or the builder of one, not {spec!r}")

    issues = spec.validate()
    if issues:
        raise ValueError("The spec is invalid:\n" + "\n".join(f"  {issue.path}: {issue.message}" for issue in issues))

    return spec.expand() if isinstance(spec, sdk.ExperimentSpec) else [spec]


def read_tables(raw_output: Path) -> dict[str, pd.DataFrame]:
    """Reads the tables of the runs under `raw_output/<scenario>/seed=<seed>/`, deleting each file once read."""
    frames: dict[str, list[pd.DataFrame]] = {}
    for scenario, seed, run in runs_under(raw_output):
        for file in sorted(run.rglob("*.parquet")):
            table = file.relative_to(run).with_suffix("").as_posix()
            frames.setdefault(table, []).append(run_frame(file, scenario, seed))
            file.unlink()

    # Runs may differ in their columns, such as the per-GPU columns of their topologies, so they join on the union.
    # The runs of a table are let go once joined, so that no table is held twice.
    return {table: pd.concat(frames.pop(table), ignore_index=True) for table in list(frames)}


def runs_under(raw_output: Path) -> list[tuple[int, int, Path]]:
    """The scenario, seed and directory of each run under `raw_output`, in that order."""
    return sorted(
        (int(run.parent.name), int(run.name.removeprefix("seed=")), run) for run in raw_output.glob("*/seed=*")
    )


def run_frame(file: Path, scenario: int, seed: int) -> pd.DataFrame:
    """The table in the parquet `file`, led by the `scenario` and `seed` columns of its run."""
    frame = pq.read_table(file).to_pandas()
    frame.insert(0, "scenario", scenario)
    frame.insert(1, "seed", seed)
    return frame


def scenario_frame(scenarios: list[KotlinObject]) -> pd.DataFrame:
    """One row per run, with a column per property of `ScenarioSpec`, indexed by the `scenario` id and `seed`."""
    columns = [name for name in inspect.signature(sdk.ScenarioSpec).parameters if name not in ("id", "seed")]
    rows = [[scenario.id, scenario.seed, *(getattr(scenario, column) for column in columns)] for scenario in scenarios]
    return pd.DataFrame(rows, columns=["scenario", "seed", *columns]).set_index(["scenario", "seed"])


def fold(name: str) -> str:
    """`name` in lower case without underscores, so that `power_source` and `powerSource` match."""
    return name.replace("_", "").lower()


class Tables(Mapping[str, Any]):
    """Exported tables by name, with a directory of tables as nested `Tables`; attributes reach them in snake_case."""

    def __init__(self, frames: dict[str, pd.DataFrame]) -> None:
        self._frames = frames

    def __getitem__(self, name: str) -> Any:
        if name in self._frames:
            return self._frames[name]

        prefix = f"{name}/"
        nested = {key.removeprefix(prefix): frame for key, frame in self._frames.items() if key.startswith(prefix)}
        if not nested:
            raise KeyError(name)
        return Tables(nested)

    def __iter__(self) -> Iterator[str]:
        return iter(dict.fromkeys(key.split("/")[0] for key in self._frames))

    def __len__(self) -> int:
        return sum(1 for _ in self)

    def __getattr__(self, name: str) -> Any:
        if name.startswith("_"):
            raise AttributeError(name)

        for key in self:
            if fold(key) == fold(name):
                return self[key]
        raise AttributeError(f"No table '{name}'; the tables are {list(self)}")

    def __dir__(self) -> list[str]:
        return sorted(set(super().__dir__()) | set(self))

    def __repr__(self) -> str:
        return f"{type(self).__name__}({list(self)})"


class SimulationResults(Tables):
    """The tables a simulation exported, each led by `scenario` and `seed`, and `scenarios`, the spec of each run."""

    def __init__(self, frames: dict[str, pd.DataFrame], scenarios: pd.DataFrame) -> None:
        super().__init__(frames)
        self.scenarios = scenarios
