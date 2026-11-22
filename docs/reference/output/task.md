---
description: The columns of task.parquet, which describes the state and resource use of each task over time.
---

# Task File

`task.parquet` describes the state and the resource use of each task over time. Unlike the other files, it does not
have exactly one row per entity and sample, so this page first explains when a task has a row.

## Rows

A task has a row at every change of its state, and at every sample while it runs. A task that does not run, for
example because it waits to be scheduled, has no rows for samples.

Several changes can happen at the same moment, such as a task that fails and is placed on another host. Each change
has its own row with the same `timestamp`, and the rows are in the order in which the changes happened.

!!! warning "Keep the order of the file"

    Sorting the file by `task_id` and `timestamp` loses the order of changes that share a timestamp. Read the rows in
    the order of the file wherever the sequence of states matters.

## Columns

| Column             | Type   | Unit  | Description                                                                          |
|--------------------|--------|-------|--------------------------------------------------------------------------------------|
| `timestamp`        | int64  | ms    | The time of the row.                                                                 |
| `task_id`          | int32  |       | The identifier of the task. See [`meta/task.parquet`](meta.md).                      |
| `host_id`          | int32  |       | The host on which the task last ran, or null if it has not been placed yet.          |
| `task_state`       | string |       | `PROVISIONING`, `RUNNING`, `PAUSED`, `FAILED`, `COMPLETED` or `TERMINATED`.          |
| `cpu_usage`        | float  | MHz   | The CPU capacity that the task uses. 0 when it does not run.                         |
| `cpu_demand`       | float  | MHz   | The CPU capacity that the task asks for. 0 when it does not run.                     |
| `cpu_time_active`  | int64  | ms    | The total time for which the CPU was active for the task.                            |
| `cpu_time_idle`    | int64  | ms    | The total time for which the CPU was idle for the task.                              |
| `cpu_time_steal`   | int64  | ms    | The total time for which the task wanted to run, but no CPU capacity was available.  |
| `cpu_time_lost`    | int64  | ms    | The total CPU time lost to interference.                                             |
| `gpu_usage`        | float  | MHz   | The GPU capacity that the task uses.                                                 |
| `gpu_demand`       | float  | MHz   | The GPU capacity that the task asks for.                                             |
| `gpu_time_active`  | int64  | ms    | The total time for which the GPU was active for the task.                            |
| `gpu_time_idle`    | int64  | ms    | The total time for which the GPU was idle for the task.                              |
| `gpu_time_steal`   | int64  | ms    | The total time for which the task wanted to run, but no GPU capacity was available.  |
| `gpu_time_lost`    | int64  | ms    | The total GPU time lost to interference.                                             |
| `num_failures`     | int64  | count | How often a host failure interrupted the task.                                       |
| `num_pauses`       | int64  | count | How often a [task stopper](../experiment/allocation-policy.md#task-stopper) paused the task. |
| `schedule_time`    | int64  | ms    | The last time at which the task was placed on a host, or null if it has never been placed. |
| `finish_time`      | int64  | ms    | The last time at which the task completed, failed or was terminated, or null.        |
| `scheduling_delay` | int64  | ms    | The total time for which the task waited to be scheduled.                            |
| `failure_delay`    | int64  | ms    | The time that the task lost because of failures.                                     |
| `checkpoint_delay` | int64  | ms    | The time that the task spent on creating checkpoints.                                |

The GPU columns are written only when the topology has GPUs.

## Task states

The column `task_state` takes six values:

| State          | The task                                                                              |
|----------------|---------------------------------------------------------------------------------------|
| `PROVISIONING` | is submitted and waits for a host                                                     |
| `RUNNING`      | runs on a host                                                                        |
| `PAUSED`       | is paused by a task stopper                                                           |
| `FAILED`       | was interrupted by the failure of its host, and waits to run again                    |
| `COMPLETED`    | finished its work                                                                     |
| `TERMINATED`   | was ended by OpenDC, for example after it failed [`maxNumFailures`](../experiment/index.md#fields) times |
