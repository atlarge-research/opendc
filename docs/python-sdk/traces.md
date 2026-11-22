---
description: How to pass workload, carbon and failure traces to the Python SDK as DataFrames or Parquet files.
---

# Traces

A trace holds data that is too large for an experiment: the tasks of a workload, the carbon intensity of a power source
over time, or a sequence of failures. An experiment refers to a trace by name, and the Python SDK accepts the trace
itself as DataFrames or as Parquet files. This page explains how to pass traces, which columns they have, and how the
SDK checks them.

## Passing traces

A spec names a trace with a [reference](../kotlin-sdk/model.md#trace-references): a `NamedReference` with a name, or a
`UriReference` with a URI. `simulate` takes the traces in the argument `traces`, a dictionary from these names and URIs
to traces:

``` python
import pandas as pd

import opendc
from opendc import ghz, gib, minutes

tasks = pd.DataFrame(
    {
        "id": [0, 1],
        "submission_time": pd.to_datetime(["2024-01-01T00:00:00Z", "2024-01-01T00:10:00Z"]),
        "duration": [pd.Timedelta(minutes=30), pd.Timedelta(minutes=20)],
        "cpu_count": [4, 8],
        "cpu_capacity": [9600.0, 19200.0],
        "mem_capacity": [8192, 16384],
    }
)
fragments = pd.DataFrame(
    {
        "id": [0, 1],
        "duration": [pd.Timedelta(minutes=30), pd.Timedelta(minutes=20)],
        "cpu_usage": [4800.0, 9600.0],
    }
)
carbon = pd.DataFrame(
    {
        "timestamp": pd.to_datetime(["2024-01-01T00:00:00Z", "2024-01-01T00:15:00Z"]),
        "carbon_intensity": [300.0, 100.0],
    }
)

with opendc.experiment() as experiment:
    with experiment.topology() as topology:
        with topology.datacenter() as datacenter:
            datacenter.power_source(carbon=opendc.NamedReference("grid"))  # (1)!
            with datacenter.cluster() as cluster:
                with cluster.host(count=2) as host:
                    host.cpu(core_count=16, core_speed=ghz(2.4))
                    host.memory(size=gib(64))
    experiment.workload(opendc.trace_workload(source=opendc.NamedReference("jobs")))  # (2)!
    experiment.export_model(opendc.ExportSpec(export_interval=minutes(5)))

results = opendc.simulate(
    experiment,
    traces={
        "jobs": opendc.WorkloadTrace(tasks=tasks, fragments=fragments),
        "grid": opendc.CarbonTrace(carbon),
    },
)
```

1.  The power source refers to a carbon trace called `grid`.
2.  The workload refers to a workload trace called `jobs`.

The SDK has one class per kind of trace:

| Class                  | From DataFrames                              | From Parquet                                                                 |
|------------------------|----------------------------------------------|------------------------------------------------------------------------------|
| `opendc.WorkloadTrace` | `WorkloadTrace(tasks=..., fragments=...)`    | `WorkloadTrace.from_parquet(directory)`, a directory with `tasks.parquet` and `fragments.parquet` |
| `opendc.CarbonTrace`   | `CarbonTrace(frame)`                         | `CarbonTrace.from_parquet(file)`                                             |
| `opendc.FailureTrace`  | `FailureTrace(frame)`                        | `FailureTrace.from_parquet(file)`                                            |

Inline workloads, [inline carbon models](../reference/experiment/topology/power-source.md#carbon-models) and
[inline failure models](../reference/experiment/failure-model.md#inline-trace-models) need no trace.

## References without a trace

A reference that is missing from `traces` resolves to a file. A name, or a URI without a scheme, is a path relative
to the argument `input_root` of `simulate`, which is the working directory by default. This is the rule of the
[`opendc`](../reference/experiment/index.md#referencing-trace-files) command, so an experiment that runs with the
command also runs with the SDK:

``` python
results = opendc.simulate(experiment, input_root="studies/big")
```

## Columns

The traces have the columns that the reference pages describe:
[workload traces](../reference/experiment/workload.md#workload-traces),
[carbon traces](../reference/experiment/topology/power-source.md#carbon-traces) and
[failure traces](../reference/experiment/failure-model.md#failure-traces). Four functions return the same information
as Arrow schemas, in which a column that is not nullable is required:

``` python
opendc.WorkloadTrace.task_schema()
opendc.WorkloadTrace.fragment_schema()
opendc.CarbonTrace.schema()
opendc.FailureTrace.schema()
```

The units of the columns are fixed. Durations are in milliseconds, `cpu_capacity`, `gpu_capacity`, `cpu_usage` and
`gpu_usage` in megahertz, and `mem_capacity` in mebibytes.

## Conversion and checks

The SDK casts every table to its schema before the simulation starts. This spares a program the details of the Parquet
format, such as which columns must not be nullable:

- A pandas datetime becomes a timestamp in milliseconds, and a pandas timedelta a number of milliseconds.
- A DataFrame with a column that the schema lacks raises `ValueError`, as does a missing required column or a missing
  value in a required column. The message lists the columns of the table.
- A Parquet file may have further columns, which the SDK ignores.
- A Parquet file that already matches the schema is read in place, without a copy. The traces that OpenDC writes match.

The SDK also checks the kind of each trace against the reference that uses it. A `CarbonTrace` under the name of a
workload raises `ValueError` before any simulation runs:

``` text
ValueError: traces['jobs'] is a carbon trace, but workloads[0].source references a workload trace
```
