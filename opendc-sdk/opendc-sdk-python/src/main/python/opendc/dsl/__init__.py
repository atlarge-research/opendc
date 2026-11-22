"""The DSL that builds SDK specs, one Python builder per builder of the Kotlin DSL in `org.opendc.sdk.model.dsl`."""

from opendc.bridge import UNSET
from opendc.dsl.experiment import ExperimentBuilder, ScenarioBuilder, experiment, scenario
from opendc.dsl.scheduler import FilterSchedulerBuilder, TimeshiftBuilder, filter_scheduler, prefab_scheduler
from opendc.dsl.topology import ClusterBuilder, DataCenterBuilder, HostBuilder, TopologyBuilder, topology
from opendc.dsl.units import bytes as bytes
from opendc.dsl.units import gbps, ghz, gib, hours, kibps, kwatts, kwh, mhz, mib, minutes, ms, seconds, watts
from opendc.dsl.workload import InlineWorkloadBuilder, TaskBuilder, inline_workload, trace_workload

# bytes stays out of __all__, so that a star import keeps the builtin
__all__ = [
    "UNSET",
    "ClusterBuilder",
    "DataCenterBuilder",
    "ExperimentBuilder",
    "FilterSchedulerBuilder",
    "HostBuilder",
    "InlineWorkloadBuilder",
    "ScenarioBuilder",
    "TaskBuilder",
    "TimeshiftBuilder",
    "TopologyBuilder",
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
    "topology",
    "trace_workload",
    "watts",
]
