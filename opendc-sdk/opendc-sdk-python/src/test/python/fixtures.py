"""Specs built by constructor that mirror `Fixtures.kt` of the SDK model, tiny experiments, and the runner's traces."""

from pathlib import Path

import pandas as pd

import opendc
from opendc import ghz, gib, kwatts, kwh, minutes, watts

RESOURCES = Path(__file__).parents[4] / "opendc-sdk-runner" / "src" / "test" / "resources"
"""The directory of the small traces that the tests of the runner use."""

SINGLE_TASK = RESOURCES / "workloadTraces/single_task"
"""The runner's workload trace of one task."""

sample_host = opendc.HostSpec(
    name="compute-host",
    count=4,
    cpu=opendc.CpuSpec(core_count=8, core_speed=ghz(3), count=2, vendor="AMD", model_name="EPYC", architecture="Zen4"),
    memory=opendc.MemorySpec(size=gib(32), speed=ghz(3), vendor="Samsung"),
    cpu_power_model=opendc.SquarePowerModelSpec(max_power=watts(500), idle_power=watts(100)),
    cpu_distribution=opendc.FixedSharePolicySpec(0.5),
    gpu_distribution=opendc.EqualSharePolicySpec,
)

sample_cluster = opendc.ClusterSpec(name="cluster-a", count=2, hosts=[sample_host])

sample_data_center = opendc.DataCenterSpec(
    name="DC-a",
    clusters=[sample_cluster],
    power_source=opendc.PowerSourceSpec(
        name="grid",
        max_power=kwatts(50),
        carbon=opendc.TraceBasedCarbonSpec(opendc.NamedReference("carbon-trace")),
    ),
    battery=opendc.BatterySpec(
        name="cell",
        capacity=kwh(100),
        charging_speed=watts(1000),
        initial_charge=kwh(20),
        policy=opendc.DoubleBatteryPolicySpec(lower_threshold=100.0, upper_threshold=300.0),
        embodied_carbon=50.0,
        expected_lifetime=10.0,
    ),
)

sample_topology = opendc.TopologySpec([sample_data_center])

sample_root_task = opendc.TaskSpec(
    id=0,
    submission_time=minutes(0),
    duration=minutes(10),
    cpu_core_count=4,
    cpu_capacity=ghz(2),
    memory=gib(8),
    fragments=[
        opendc.TaskFragmentSpec(duration=minutes(5), cpu_usage=ghz(2)),
        opendc.TaskFragmentSpec(duration=minutes(5), cpu_usage=ghz(1), gpu_usage=ghz(1), gpu_memory=gib(2)),
    ],
)

sample_leaf_task = opendc.TaskSpec(
    id=1,
    submission_time=minutes(2),
    duration=minutes(20),
    cpu_core_count=8,
    cpu_capacity=ghz(3),
    memory=gib(16),
    fragments=[opendc.TaskFragmentSpec(duration=minutes(20), cpu_usage=ghz(3))],
    deferrable=True,
    deadline=minutes(60),
    parents=[0],
)

sample_workload = opendc.InlineWorkloadSpec([sample_root_task, sample_leaf_task], opendc.ScalingPolicySpec.Perfect)

tiny_topology = opendc.TopologySpec(
    [
        opendc.DataCenterSpec(
            [
                opendc.ClusterSpec(
                    hosts=[opendc.HostSpec(cpu=opendc.CpuSpec(1, ghz(2)), memory=opendc.MemorySpec(gib(4)))]
                )
            ]
        )
    ]
)
"""One host with one 2 GHz core."""

tiny_workload = opendc.InlineWorkloadSpec(
    [
        opendc.TaskSpec(
            id=0,
            submission_time=minutes(0),
            duration=minutes(10),
            cpu_core_count=1,
            cpu_capacity=ghz(1),
            memory=gib(1),
            fragments=[opendc.TaskFragmentSpec(minutes(10), ghz(1))],
        )
    ]
)
"""One task using 1 GHz for 10 minutes."""

minute_export = opendc.ExportSpec(export_interval=minutes(1), print_frequency=None)
"""Exports a sample every simulated minute."""

trace_host = opendc.HostSpec(name="H01", cpu=opendc.CpuSpec(64, opendc.mhz(2000)), memory=opendc.MemorySpec(gib(1024)))
"""A host as large as the one the runner's tests run the bundled traces on."""

tiny_scenario = opendc.ScenarioSpec(
    tiny_topology, tiny_workload, opendc.PrefabAllocationPolicySpec(), export_model=minute_export
)


def tiny_experiment(**settings: object) -> opendc.KotlinObject:
    """An experiment of the tiny workload on the tiny topology, with `settings` such as its name."""
    return opendc.ExperimentSpec(
        topologies=[tiny_topology], workloads=[tiny_workload], export_models=[minute_export], **settings
    )


def single_task(**task_columns: object) -> opendc.WorkloadTrace:
    """The runner's single-task trace as DataFrames, with `task_columns` added to its task."""
    return opendc.WorkloadTrace(
        pd.read_parquet(SINGLE_TASK / "tasks.parquet").assign(**task_columns),
        pd.read_parquet(SINGLE_TASK / "fragments.parquet"),
    )
