"""Builds and simulates OpenDC experiments; the declarations of the SDK model and runner are attributes of `opendc`."""

from typing import Any

from opendc.bridge import KotlinClass, KotlinObject, sdk_member, sdk_names, start_jvm
from opendc.dsl import bytes as bytes
from opendc.dsl import (
    experiment,
    filter_scheduler,
    gbps,
    ghz,
    gib,
    hours,
    inline_workload,
    kibps,
    kwatts,
    kwh,
    mhz,
    mib,
    minutes,
    ms,
    prefab_scheduler,
    scenario,
    seconds,
    topology,
    trace_workload,
    watts,
)
from opendc.server import ApiFailure, RemoteRunFailed, Server
from opendc.simulation import SimulationResults, Tables, simulate
from opendc.traces import CarbonTrace, FailureTrace, WorkloadTrace

# bytes stays out of __all__, so that a star import keeps the builtin
__all__ = [
    "ApiFailure",
    "CarbonTrace",
    "FailureTrace",
    "KotlinClass",
    "KotlinObject",
    "RemoteRunFailed",
    "Server",
    "SimulationResults",
    "Tables",
    "WorkloadTrace",
    "experiment",
    "filter_scheduler",
    "gbps",
    "ghz",
    "gib",
    "hours",
    "inline_workload",
    "kibps",
    "kwatts",
    "kwh",
    "mhz",
    "mib",
    "minutes",
    "ms",
    "prefab_scheduler",
    "scenario",
    "seconds",
    "simulate",
    "start_jvm",
    "topology",
    "trace_workload",
    "watts",
]


def __getattr__(name: str) -> Any:
    if name.startswith("_"):
        raise AttributeError(name)
    return sdk_member(name)


def __dir__() -> list[str]:
    return sorted(set(globals()) | set(sdk_names()))
