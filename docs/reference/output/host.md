---
description: The columns of host.parquet, which describes the state, utilisation and power draw of each host.
---

# Host File

`host.parquet` has one row per host and sample. It describes the state, the utilisation and the power draw of each
host.

## Columns

| Column             | Type   | Unit   | Description                                                                 |
|--------------------|--------|--------|-----------------------------------------------------------------------------|
| `timestamp`        | int64  | ms     | The time of the sample.                                                     |
| `host_id`          | int32  |        | The identifier of the host. [`meta/host.parquet`](meta.md) holds its name.  |
| `host_state`       | string |        | `UP`, `DOWN` or `ERROR`.                                                    |
| `tasks_running`    | int32  | count  | The number of tasks that run on the host.                                   |
| `tasks_error`      | int32  | count  | The number of tasks on the host that failed.                                |
| `tasks_invalid`    | int32  | count  | The number of tasks on the host in an unexpected state.                     |
| `cpu_usage`        | float  | MHz    | The CPU capacity that the tasks use.                                        |
| `cpu_demand`       | float  | MHz    | The CPU capacity that the tasks ask for.                                    |
| `cpu_utilization`  | float  | ratio  | `cpu_usage` divided by the CPU capacity of the host.                        |
| `cpu_time_active`  | int64  | ms     | The total time for which the CPU was active.                                |
| `cpu_time_idle`    | int64  | ms     | The total time for which the CPU was idle.                                  |
| `cpu_time_steal`   | int64  | ms     | The total time for which tasks wanted to run, but no CPU capacity was available. |
| `cpu_time_lost`    | int64  | ms     | The total CPU time lost to interference.                                    |
| `power_draw`       | float  | W      | The power draw of the host.                                                 |
| `energy_usage`     | float  | J      | The energy that the host used since the start of the simulation.            |
| `carbon_intensity` | float  | g/kWh  | The carbon intensity of the energy.                                         |
| `carbon_emission`  | float  | g      | The carbon emitted since the start of the simulation.                       |
| `embodied_carbon`  | float  | g      | The embodied carbon of the host, for the time since the previous sample.    |
| `uptime`           | int64  | ms     | The total time for which the host was up.                                   |
| `downtime`         | int64  | ms     | The total time for which the host was down because of a failure.            |
| `boot_time`        | int64  | ms     | When the host booted, or null if it has not booted.                         |

## GPU columns

When the topology has GPUs, the file has a set of columns for each GPU of a host. The columns of the first GPU end in
`_0`, those of the second in `_1`, and so on. A host with fewer GPUs than the host with the most GPUs has null values
in the columns of the GPUs that it lacks. In the following table, *i* stands for the number of the GPU.

| Column              | Type  | Unit  | Description                                                                |
|---------------------|-------|-------|----------------------------------------------------------------------------|
| `gpu_usage_i`       | float | MHz   | The GPU capacity that the tasks use.                                       |
| `gpu_demand_i`      | float | MHz   | The GPU capacity that the tasks ask for.                                   |
| `gpu_utilization_i` | float | ratio | `gpu_usage_i` divided by the capacity of the GPU.                          |
| `gpu_time_active_i` | int64 | ms    | The total time for which the GPU was active.                               |
| `gpu_time_idle_i`   | int64 | ms    | The total time for which the GPU was idle.                                 |
| `gpu_time_steal_i`  | int64 | ms    | The total time for which tasks wanted to run, but no GPU capacity was available. |
| `gpu_time_lost_i`   | int64 | ms    | The total GPU time lost to interference.                                   |
| `gpu_power_draw_i`  | float | W     | The power draw of the GPU.                                                 |
