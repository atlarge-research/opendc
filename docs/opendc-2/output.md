---
description: The files OpenDC 2 writes during a simulation, their columns and how to read them.
status: deprecated
search:
  exclude: true
---

# Output

During a simulation, OpenDC 2 regularly takes a *sample* of the state of the datacenter and writes it to Parquet files.
The [export model](experiment.md#export-model) decides which files OpenDC 2 writes, which columns they have and how
often it takes a sample. This page describes where OpenDC 2 writes the files and lists the columns of each file.

## Overview

All output files of a run share a folder and a notion of time. This section describes both, lists the files and shows
how to read one.

### Where the results are written

OpenDC 2 writes the results of an experiment to `outputFolder/name/`, where `outputFolder` and `name` are
[fields of the experiment](experiment.md#fields). Each run of a scenario gets its own folder:

``` text
output/
+-- example/                  the name of the experiment
    +-- trackr.json           the settings of every scenario
    +-- simulation-analysis/
    +-- raw-output/
        +-- 0/                scenario 0
        |   +-- seed=0/       the run with seed 0
        |   |   +-- host.parquet
        |   |   +-- task.parquet
        |   |   +-- service.parquet
        |   |   +-- powerSource.parquet
        |   |   +-- battery.parquet
        |   +-- seed=1/
        +-- 1/                scenario 1
```

The analysis tool [M3SA](m3sa.md) fills `simulation-analysis/` when it is used.

### Output files

A run writes up to five output files:

| File                                       | One row per              |
|--------------------------------------------|--------------------------|
| [`host.parquet`](#host-file)               | host, per sample         |
| [`task.parquet`](#task-file)               | task, per sample         |
| [`service.parquet`](#service-file)         | sample                   |
| [`powerSource.parquet`](#power-source-file) | power source, per sample |
| [`battery.parquet`](#battery-file)         | battery, per sample      |

### Time

Every file has two times: `timestamp` is in milliseconds since the start of the simulation, and `timestamp_absolute` is
in milliseconds since the Unix epoch.

Each sample measures energy use, carbon emissions, uptime, downtime and CPU and GPU times over the time since the
previous sample. To get the total, add up the values of all samples.

### Example

The following Python code reads the host file of a run, converts its absolute timestamps and adds up the energy use of
each host:

``` python
import pandas as pd

host = pd.read_parquet("output/example/raw-output/0/seed=0/host.parquet")
host["time"] = pd.to_datetime(host["timestamp_absolute"], unit="ms")
total_energy = host.groupby("host_name")["energy_usage"].sum()
```

## Host file

`host.parquet` has one row per host per sample. It describes the state, utilisation and power draw of each host.

| Column               | Type   | Unit  | Description                                                                                |
|----------------------|--------|-------|--------------------------------------------------------------------------------------------|
| `timestamp`          | int64  | ms    | The time of the sample, since the start of the simulation.                                 |
| `timestamp_absolute` | int64  | ms    | The time of the sample, since the Unix epoch.                                              |
| `host_name`          | string |       | The name of the host.                                                                      |
| `cluster_name`       | string |       | The name of the cluster of the host.                                                       |
| `core_count`         | int32  | count | The number of CPU cores of the host.                                                       |
| `mem_capacity`       | int64  | MiB   | The memory of the host.                                                                    |
| `tasks_terminated`   | int32  | count | The number of terminated tasks on the host.                                                |
| `tasks_running`      | int32  | count | The number of tasks running on the host.                                                   |
| `tasks_error`        | int32  | count | The number of tasks on the host that failed.                                               |
| `tasks_invalid`      | int32  | count | The number of tasks on the host in an unexpected state.                                    |
| `cpu_capacity`       | float  | MHz   | The total CPU capacity of the host.                                                        |
| `cpu_usage`          | float  | MHz   | The CPU capacity used by the tasks.                                                        |
| `cpu_demand`         | float  | MHz   | The CPU capacity the tasks ask for.                                                        |
| `cpu_utilization`    | float  | ratio | `cpu_usage` divided by `cpu_capacity`.                                                     |
| `cpu_time_active`    | int64  | ms    | The time the CPU was active, since the previous sample.                                    |
| `cpu_time_idle`      | int64  | ms    | The time the CPU was idle, since the previous sample.                                      |
| `cpu_time_steal`     | int64  | ms    | The time tasks wanted to run but no CPU capacity was available, since the previous sample. |
| `cpu_time_lost`      | int64  | ms    | The CPU time lost to interference, since the previous sample.                              |
| `power_draw`         | float  | W     | The power draw of the host.                                                                |
| `energy_usage`       | float  | J     | The energy the host used since the previous sample.                                        |
| `embodied_carbon`    | float  | g     | The embodied carbon of the host, for the time since the previous sample.                   |
| `uptime`             | int64  | ms    | The time the host was up, since the previous sample.                                       |
| `downtime`           | int64  | ms    | The time the host was down, since the previous sample.                                     |
| `boot_time`          | int64  | ms    | When the host booted.                                                                      |

### GPU columns

When the topology has GPUs, the file has a set of columns for each GPU of a host. The columns of the first GPU end in
`_0`, those of the second in `_1` and so on.

| Column              | Type  | Unit  | Description                                                                                |
|---------------------|-------|-------|--------------------------------------------------------------------------------------------|
| `gpu_capacity_i`    | float | MHz   | The capacity of the GPU.                                                                   |
| `gpu_usage_i`       | float | MHz   | The GPU capacity used by the tasks.                                                        |
| `gpu_demand_i`      | float | MHz   | The GPU capacity the tasks ask for.                                                        |
| `gpu_utilization_i` | float | ratio | `gpu_usage_i` divided by `gpu_capacity_i`.                                                 |
| `gpu_time_active_i` | int64 | ms    | The time the GPU was active, since the previous sample.                                    |
| `gpu_time_idle_i`   | int64 | ms    | The time the GPU was idle, since the previous sample.                                      |
| `gpu_time_steal_i`  | int64 | ms    | The time tasks wanted to run but no GPU capacity was available, since the previous sample. |
| `gpu_time_lost_i`   | int64 | ms    | The GPU time lost to interference, since the previous sample.                              |
| `gpu_power_draw_i`  | float | W     | The power draw of the GPU.                                                                 |

## Task file

`task.parquet` has one row per task per sample. It describes the state and resource use of each task.

| Column               | Type   | Unit  | Description                                                                                   |
|----------------------|--------|-------|-----------------------------------------------------------------------------------------------|
| `timestamp`          | int64  | ms    | The time of the sample, since the start of the simulation.                                    |
| `timestamp_absolute` | int64  | ms    | The time of the sample, since the Unix epoch.                                                 |
| `task_id`            | int32  |       | The identifier of the task.                                                                   |
| `task_name`          | string |       | The name of the task, from the workload trace.                                                |
| `host_name`          | string |       | The host the task runs on, or null if it has no host.                                         |
| `mem_capacity`       | int64  | MiB   | The memory the task needs.                                                                    |
| `cpu_count`          | int32  | count | The number of CPU cores the task needs.                                                       |
| `cpu_limit`          | float  | MHz   | The CPU capacity of the host available to the task.                                           |
| `cpu_usage`          | float  | MHz   | The CPU capacity the task uses.                                                               |
| `cpu_demand`         | float  | MHz   | The CPU capacity the task asks for.                                                           |
| `cpu_time_active`    | int64  | ms    | The time the CPU was active for the task, since the previous sample.                          |
| `cpu_time_idle`      | int64  | ms    | The time the CPU was idle for the task, since the previous sample.                            |
| `cpu_time_steal`     | int64  | ms    | The time the task wanted to run but no CPU capacity was available, since the previous sample. |
| `cpu_time_lost`      | int64  | ms    | The CPU time lost to interference, since the previous sample.                                 |
| `gpu_count`          | int32  | count | The number of GPU cores the task needs.                                                       |
| `gpu_limit`          | float  | MHz   | The GPU capacity of the host available to the task.                                           |
| `gpu_usage`          | float  | MHz   | The GPU capacity the task uses.                                                               |
| `gpu_demand`         | float  | MHz   | The GPU capacity the task asks for.                                                           |
| `gpu_time_active`    | int64  | ms    | The time the GPU was active for the task, since the previous sample.                          |
| `gpu_time_idle`      | int64  | ms    | The time the GPU was idle for the task, since the previous sample.                            |
| `gpu_time_steal`     | int64  | ms    | The time the task wanted to run but no GPU capacity was available, since the previous sample. |
| `gpu_time_lost`      | int64  | ms    | The GPU time lost to interference, since the previous sample.                                 |
| `uptime`             | int64  | ms    | The time the task was running, since the previous sample.                                     |
| `downtime`           | int64  | ms    | The time the task was not running, since the previous sample.                                 |
| `num_failures`       | int64  | count | How often the task was interrupted by a host failure.                                         |
| `num_pauses`         | int64  | count | How often the task was paused by a [task stopper](allocation-policy.md#task-stopper).         |
| `submission_time`    | int64  | ms    | When the task was submitted.                                                                  |
| `schedule_time`      | int64  | ms    | When the task was last placed on a host.                                                      |
| `finish_time`        | int64  | ms    | When the task completed or was terminated.                                                    |
| `task_state`         | string |       | The state of the task.                                                                        |
| `scheduling_delay`   | int64  | ms    | The total time the task waited to be scheduled.                                               |
| `failure_delay`      | int64  | ms    | The time the task lost because of failures.                                                   |
| `checkpoint_delay`   | int64  | ms    | The time the task spent creating checkpoints.                                                 |

## Service file

`service.parquet` has one row per sample. It gives an overview of the whole simulation: how many hosts are available and
how many tasks are waiting, running and finished.

| Column               | Type  | Unit  | Description                                                |
|----------------------|-------|-------|------------------------------------------------------------|
| `timestamp`          | int64 | ms    | The time of the sample, since the start of the simulation. |
| `timestamp_absolute` | int64 | ms    | The time of the sample, since the Unix epoch.              |
| `hosts_up`           | int32 | count | The number of hosts that are up.                           |
| `hosts_down`         | int32 | count | The number of hosts that are down.                         |
| `tasks_total`        | int32 | count | The number of tasks submitted so far.                      |
| `tasks_pending`      | int32 | count | The number of tasks waiting to be scheduled.               |
| `tasks_active`       | int32 | count | The number of tasks running.                               |
| `tasks_completed`    | int32 | count | The number of tasks that completed.                        |
| `tasks_terminated`   | int32 | count | The number of tasks that were terminated.                  |

## Power source file

`powerSource.parquet` has one row per [power source](topology.md#power-source) per sample. It describes the power
supplied by each power source and the carbon emitted to produce it.

| Column               | Type   | Unit     | Description                                                |
|----------------------|--------|----------|------------------------------------------------------------|
| `timestamp`          | int64  | ms       | The time of the sample, since the start of the simulation. |
| `timestamp_absolute` | int64  | ms       | The time of the sample, since the Unix epoch.              |
| `source_name`        | string |          | The name of the power source.                              |
| `cluster_name`       | string |          | The name of the cluster of the power source.               |
| `power_draw`         | float  | W        | The power supplied by the power source.                    |
| `energy_usage`       | float  | J        | The energy supplied since the previous sample.             |
| `carbon_intensity`   | float  | gCO2/kWh | The carbon intensity of the energy, from the carbon trace. |
| `carbon_emission`    | float  | g        | The carbon emitted since the previous sample.              |

## Battery file

`battery.parquet` has one row per [battery](topology.md#battery) per sample. It describes the charge and state of each
battery.

| Column                     | Type   | Unit | Description                                                    |
|----------------------------|--------|------|----------------------------------------------------------------|
| `timestamp`                | int64  | ms   | The time of the sample, since the start of the simulation.     |
| `timestamp_absolute`       | int64  | ms   | The time of the sample, since the Unix epoch.                  |
| `battery_name`             | string |      | The name of the battery.                                       |
| `cluster_name`             | string |      | The name of the cluster of the battery.                        |
| `power_draw`               | float  | W    | The power the battery supplies.                                |
| `energy_usage`             | float  | J    | The energy the battery supplied since the previous sample.     |
| `embodied_carbon_emission` | float  | g    | The embodied carbon of the battery, since the previous sample. |
| `charge`                   | float  | J    | The energy stored in the battery.                              |
| `capacity`                 | float  | J    | The amount of energy the battery can store.                    |
| `battery_state`            | string |      | `CHARGING`, `IDLE` or `DISCHARGING`.                           |
