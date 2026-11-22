---
description: Workloads, which define the tasks that a datacenter receives, inline or from a trace.
---

# Workload

A *workload* defines the tasks that a datacenter receives: when each task is submitted, what it requires, and how much
CPU and GPU capacity it uses over time. This page describes the three kinds of workload and the format of workload
traces.

The field `type` selects the kind of workload:

| Type              | The tasks are                                                                         |
|-------------------|---------------------------------------------------------------------------------------|
| `trace`           | read from a [workload trace](#workload-traces)                                        |
| `efficient-trace` | read from a workload trace in a single pass, which is faster and uses less memory     |
| `inline`          | listed in the experiment                                                              |

## Trace workloads

A trace workload replays a workload trace, which is typically recorded from a real system.

| Field            | Type                                                    | Default   | Description                                                                                  |
|------------------|---------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------|
| `source`         | [trace reference](index.md#referencing-trace-files)     | required  | The directory of the workload trace.                                                         |
| `sampleFraction` | number                                                  | `1.0`     | Simulates a random sample of the tasks that together have this fraction of the work of the trace. |
| `submissionTime` | string                                                  | unchanged | Moves the workload in time so that its first task is submitted at this moment, such as `"2024-01-01T00:00:00"` in UTC. |
| `deferAll`       | boolean                                                 | `false`   | Makes every task deferrable, with a deadline of its submission time plus three times its duration. |
| `scalingPolicy`  | [scaling policy](#scaling-policies)                     | `NoDelay` | What happens when a task receives less capacity than it demands.                             |

``` json
{
    "type": "trace",
    "source": { "type": "named", "name": "traces/bitbrains-small" },
    "submissionTime": "2024-01-01T00:00:00"
}
```

The `efficient-trace` type takes the same fields. It reads the trace once, from start to end, and therefore makes two
demands on the trace: the tasks have to be ordered by `submission_time`, and each task has to state its number of
fragments in the column `num_fragments`.

A *deferrable* task is one that a [time-shifting](allocation-policy.md#time-shifting) allocation policy may delay. A
trace marks individual tasks as deferrable with its `deferrable` and `deadline` columns, and `deferAll` marks all of
them.

## Scaling policies

A task may receive less CPU capacity than it demands, for example because the tasks on its host together demand more
than the host has. The *scaling policy* decides what this means for the task:

| Policy    | Behaviour                                                                              |
|-----------|----------------------------------------------------------------------------------------|
| `NoDelay` | The task keeps its duration. The work that was not done is lost. This is the default.  |
| `Perfect` | The task runs longer, until all of its work is done.                                   |

## Inline workloads

An inline workload lists its tasks in the experiment. It suits small tests, because it is quick to write and change.

| Field           | Type                                | Default   | Description                                                      |
|-----------------|-------------------------------------|-----------|------------------------------------------------------------------|
| `tasks`         | list of [tasks](#tasks)             | required  | The tasks of the workload.                                       |
| `scalingPolicy` | [scaling policy](#scaling-policies) | `NoDelay` | What happens when a task receives less capacity than it demands. |

### Tasks

| Field            | Type                         | Default     | Description                                                                    |
|------------------|------------------------------|-------------|--------------------------------------------------------------------------------|
| `id`             | integer                      | required    | The identifier of the task, unique within the workload.                        |
| `submissionTime` | [duration](../units.md)      | required    | When the task is submitted, counted from the start of the workload.            |
| `duration`       | [duration](../units.md)      | required    | How long the task runs.                                                        |
| `cpuCoreCount`   | integer                      | required    | The number of CPU cores that the task needs.                                   |
| `cpuCapacity`    | [frequency](../units.md)     | required    | The CPU capacity that the task needs.                                          |
| `memory`         | [data size](../units.md)     | required    | The memory that the task needs.                                                |
| `fragments`      | list of [fragments](#fragments) | required | The demand of the task over time.                                              |
| `gpuCoreCount`   | integer                      | `0`         | The number of GPU cores that the task needs.                                   |
| `gpuCapacity`    | [frequency](../units.md)     | `0`         | The GPU capacity that the task needs.                                          |
| `gpuMemory`      | [data size](../units.md)     | `0`         | The GPU memory that the task needs.                                            |
| `deferrable`     | boolean                      | `false`     | Whether a time-shifting allocation policy may delay the task.                  |
| `deadline`       | [duration](../units.md)      | no deadline | The latest moment at which the task may finish, counted from the start of the workload. |
| `parents`        | list of integers             | `[]`        | The tasks that have to finish before this task can start.                      |
| `children`       | list of integers             | `[]`        | The tasks that depend on this task.                                            |

### Fragments

A *fragment* is a period during which the demand of a task is constant. The fragments of a task follow each other in
the order of the list.

| Field       | Type                      | Default  | Description                              |
|-------------|---------------------------|----------|------------------------------------------|
| `duration`  | [duration](../units.md)   | required | How long the fragment lasts.             |
| `cpuUsage`  | [frequency](../units.md)  | required | The CPU demand during the fragment.      |
| `gpuUsage`  | [frequency](../units.md)  | `0`      | The GPU demand during the fragment.      |
| `gpuMemory` | [data size](../units.md)  | `0`      | The GPU memory used during the fragment. |

``` json
{
    "type": "inline",
    "tasks": [
        {
            "id": 0,
            "submissionTime": "0 minutes",
            "duration": "10 minutes",
            "cpuCoreCount": 2,
            "cpuCapacity": "3.0 GHz",
            "memory": "2 GiB",
            "fragments": [
                { "duration": "5 minutes", "cpuUsage": "3.0 GHz" },
                { "duration": "5 minutes", "cpuUsage": "1.5 GHz" }
            ]
        }
    ]
}
```

## Workload traces

A workload trace is a directory with two Parquet files. `tasks.parquet` describes each task, and `fragments.parquet`
describes the demand of each task over time.

### tasks.parquet

| Column            | Type                     | Required | Description                                                               |
|-------------------|--------------------------|----------|---------------------------------------------------------------------------|
| `id`              | int32                    | yes      | The identifier of the task.                                               |
| `name`            | string                   | no       | The name of the task.                                                     |
| `submission_time` | timestamp (milliseconds) | yes      | When the task is submitted.                                               |
| `duration`        | int64                    | yes      | How long the task runs, in milliseconds.                                  |
| `cpu_count`       | int32                    | yes      | The number of CPU cores that the task needs.                              |
| `cpu_capacity`    | double                   | yes      | The CPU capacity that the task needs, in MHz.                             |
| `mem_capacity`    | int64                    | yes      | The memory that the task needs, in MiB.                                   |
| `gpu_count`       | int32                    | no       | The number of GPU cores that the task needs.                              |
| `gpu_capacity`    | double                   | no       | The GPU capacity that the task needs, in MHz.                             |
| `parents`         | list of int32            | no       | The tasks that have to finish before this task can start.                 |
| `children`        | list of int32            | no       | The tasks that depend on this task.                                       |
| `deferrable`      | boolean                  | no       | Whether a time-shifting allocation policy may delay the task.             |
| `deadline`        | int64                    | no       | The latest moment at which the task may finish, in milliseconds since the Unix epoch. It bounds how long a time-shifting allocation policy may delay the task. |
| `num_fragments`   | int32                    | no       | The number of fragments of the task. The `efficient-trace` type requires it. |

### fragments.parquet

Each row is one fragment. The fragments of a task appear in the order in which they occur.

| Column      | Type   | Required | Description                                    |
|-------------|--------|----------|------------------------------------------------|
| `id`        | int32  | yes      | The identifier of the task.                    |
| `duration`  | int64  | yes      | How long the fragment lasts, in milliseconds.  |
| `cpu_usage` | double | yes      | The CPU demand during the fragment, in MHz.    |
| `gpu_usage` | double | no       | The GPU demand during the fragment, in MHz.    |

!!! warning "Required columns must not be nullable"

    OpenDC reads a required column only if the Parquet file declares it as *required*, that is, not nullable. Many
    tools declare every column as nullable by default, among them `DataFrame.to_parquet` of pandas, and OpenDC then
    fails with the error `Can not read value at 0 in block -1`. Pass a schema with non-nullable fields when writing a
    trace, or let the [Python SDK](../../python-sdk/traces.md) write it, which casts every table to the schema that
    OpenDC expects.
