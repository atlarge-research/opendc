
Running OpenDC results in five output files: 
1. [Task](#task) contains metrics related to the jobs being executed.
2. [Host](#host) contains all metrics related to the hosts on which jobs can be executed.
3. [Power Source](#power-source) contains all metrics related to the power sources that power the hosts.
4. [Battery](#battery) contains all metrics related to the batteries that power the hosts.
5. [Service](#service) contains metrics describing the overall performance.

User can define which files, and features are to be included in the output in the experiment file (see [ExportModel](/docs/documentation/Input/ExportModel.md)).

### Task
The task output file, contains all metrics of related to the tasks that are being executed.

| Metric             | Datatype | Unit      | Summary                                                                     |
|--------------------|----------|-----------|-----------------------------------------------------------------------------|
| timestamp          | int64    | ms        | Timestamp of the sample since the start of the workload.                    |
| timestamp_absolute | int64    | ms        | The absolute timestamp based on the given workload.                         |
| task_id            | binary   | string    | The id of the task determined during runtime.                               |
| task_name          | binary   | string    | The name of the task provided by the Trace.                                 |
| host_name          | binary   | string    | The id of the host on which the task is hosted or `null` if it has no host. |
| mem_capacity       | int64    | Mb        | The memory required by the task.                                            |
| cpu_count          | int32    | count     | The number of CPUs required by the task.                                    |
| cpu_limit          | double   | MHz       | The capacity of the CPUs of Host on which the task is running.              |
| cpu_usage          | double   | MHz       | The cpu capacity provided by the CPU to the task.                           |
| cpu_demand         | double   | MHz       | The cpu capacity demanded of the CPU by the task.                           |
| cpu_time_active    | int64    | ms        | The duration that a CPU was active in the task.                             |
| cpu_time_idle      | int64    | ms        | The duration that a CPU was idle in the task.                               |
| cpu_time_steal     | int64    | ms        | The duration that a vCPU wanted to run, but no capacity was available.      |
| cpu_time_lost      | int64    | ms        | The duration of CPU time that was lost due to interference.                 |
| gpu_limit          | double   | MHz       | The capacity of the GPUs of Host on which the task is running.              |
| gpu_usage          | double   | MHz       | The gpu capacity provided by the GPU to the task.                           |
| gpu_demand         | double   | MHz       | The gpu capacity demanded of the GPU by the task.                           |
| gpu_time_active    | int64    | ms        | The duration that a GPU was active in the task.                             |
| gpu_time_idle      | int64    | ms        | The duration that a GPU was idle in the task.                               |
| gpu_time_steal     | int64    | ms        | The duration that a vGPU wanted to run, but no capacity was available.      |
| gpu_time_lost      | int64    | ms        | The duration of GPU time that was lost due to interference.                 |
| uptime             | int64    | ms        | The uptime of the host since last sample.                                   |
| downtime           | int64    | ms        | The downtime of the host since last sample.                                 |
| num_failures       | int64    | count     | How many times was a task interrupted due to machine failure.               |
| num_pauses         | int64    | ms        | How many times was a task interrupted due to the TaskStopper.               |
| submission_time    | int64    | ms        | The time for which the task was enqueued for the scheduler.                 |
| schedule_time      | int64    | ms        | The time at which task got booted.                                          |
| finish_time        | int64    | ms        | The time at which the task was finished (either completed or terminated).   |
| task_state         | String   | TaskState | The current state of the Task.                                              |

### Host
The host output file, contains all metrics of related to the hosts that are running. 
For each GPU attached to the host, there is a separate metric for each GPU. 
The metrics are named `gpu_capacity_n`, `gpu_usage_n`, `gpu_demand_n`, etc., where `n` is the index of the GPU starting from 0.

| Metric             | DataType | Unit       | Summary                                                                                                      |
|--------------------|----------|------------|--------------------------------------------------------------------------------------------------------------|
| timestamp          | int64    | ms         | Timestamp of the sample.                                                                                     |
| timestamp_absolute | int64    | ms         | The absolute timestamp based on the given workload.                                                          |
| host_name          | binary   | string     | The name of the host.                                                                                        |
| cluster_name       | binary   | string     | The name of the cluster that this host is part of.                                                           |
| cpu_count          | int32    | count      | The number of cores in this host.                                                                            |
| mem_capacity       | int64    | Mb         | The amount of available memory.                                                                              |
| tasks_terminated   | int32    | count      | The number of tasks that are in a terminated state.                                                          |
| tasks_running      | int32    | count      | The number of tasks that are in a running state.                                                             |
| tasks_error        | int32    | count      | The number of tasks that are in an error state.                                                              |
| tasks_invalid      | int32    | count      | The number of tasks that are in an unknown state.                                                            |
| cpu_capacity       | double   | MHz        | The total capacity of the CPUs in the host.                                                                  |
| cpu_usage          | double   | MHz        | The total CPU capacity provided to all tasks on this host.                                                   |
| cpu_demand         | double   | MHz        | The total CPU capacity demanded by all tasks on this host.                                                   |
| cpu_utilization    | double   | ratio      | The CPU utilization of the host. This is calculated by dividing the cpu_usage, by the cpu_capacity.          |
| cpu_time_active    | int64    | ms         | The duration that a CPU was active in the host.                                                              |
| cpu_time_idle      | int64    | ms         | The duration that a CPU was idle in the host.                                                                |
| cpu_time_steal     | int64    | ms         | The duration that a vCPU wanted to run, but no capacity was available.                                       |
| cpu_time_lost      | int64    | ms         | The duration of CPU time that was lost due to interference.                                                  |
| gpu_capacity_n     | double   | MHz        | The total capacity of the the n-th GPU in the host.                                                          |
| gpu_usage_n        | double   | MHz        | The total of the n-th GPU capacity provided to all tasks on this host.                                       |
| gpu_demand_n       | double   | MHz        | The total of the n-th GPU capacity demanded by all tasks on this host.                                       |
| gpu_utilization_n  | double   | ratio      | The the n-th GPU utilization of the host. This is calculated by dividing the gpu_usage, by the gpu_capacity. |
| gpu_time_active_n  | int64    | ms         | The duration that the n-th GPU was active in the host.                                                       |
| gpu_time_idle_n    | int64    | ms         | The duration that the n-th GPU was idle in the host.                                                         |
| gpu_time_steal_n   | int64    | ms         | The duration that the n-th vGPU wanted to run, but no capacity was available.                                |
| gpu_time_lost_n    | int64    | ms         | The duration of the n-th GPU time that was lost due to interference.                                         |
| power_draw         | double   | Watt       | The current power draw of the host.                                                                          |
| energy_usage       | double   | Joule (Ws) | The total energy consumption of the host since last sample.                                                  |
| embodied_carbon    | double   | gram       | The total embodied carbon emitted since the last sample.                                                     |
| uptime             | int64    | ms         | The uptime of the host since last sample.                                                                    |
| downtime           | int64    | ms         | The downtime of the host since last sample.                                                                  |
| boot_time          | int64    | ms         | The time a host got booted.                                                                                  |
| boot_time_absolute | int64    | ms         | The absolute time a host got booted.                                                                         |

### Power Source
The power source output file, contains all metrics of related to the power sources.

| Metric             | DataType | Unit       | Summary                                                           |
|--------------------|----------|------------|-------------------------------------------------------------------|
| timestamp          | int64    | ms         | Timestamp of the sample.                                          |
| timestamp_absolute | int64    | ms         | The absolute timestamp based on the given workload.               |
| source_name        | binary   | string     | The name of the power source.                                     |
| cluster_name       | binary   | string     | The name of the cluster that this power source is part of.        |
| power_draw         | double   | Watt       | The current power draw of the host.                               |
| energy_usage       | double   | Joule (Ws) | The total energy consumption of the host since last sample.       |
| carbon_intensity   | double   | gCO2/kW    | The amount of carbon that is emitted when using a unit of energy. |
| carbon_emission    | double   | gram       | The amount of carbon emitted since the previous sample.           |

### Battery
The host output file, contains all metrics of related batteries.

| Metric             | DataType | Unit         | Summary                                                           |
|--------------------|----------|--------------|-------------------------------------------------------------------|
| timestamp          | int64    | ms           | Timestamp of the sample.                                          |
| timestamp_absolute | int64    | ms           | The absolute timestamp based on the given workload.               |
| battery_name       | binary   | string       | The name of the battery.                                          |
| cluster_name       | binary   | string       | The name of the cluster that this battery is part of.             |
| power_draw         | double   | Watt         | The current power draw of the host.                               |
| energy_usage       | double   | Joule (Ws)   | The total energy consumption of the host since last sample.       |
| carbon_intensity   | double   | gCO2/kW      | The amount of carbon that is emitted when using a unit of energy. |
| embodied_carbon    | double   | gram         | The total embodied carbon emitted since the last sample.          |
| charge             | double   | Joule        | The current charge of the battery.                                |
| capacity           | double   | Joule        | The total capacity of the battery.                                |
| battery_state      | String   | BatteryState | The current state of the battery.                                 |

### Service
The service output file, contains metrics providing an overview of the performance.

| Metric             | DataType | Unit  | Summary                                               |
|--------------------|----------|-------|-------------------------------------------------------|
| timestamp          | int64    | ms    | Timestamp of the sample                               |
| timestamp_absolute | int64    | ms    | The absolute timestamp based on the given workload    |
| hosts_up           | int32    | count | The number of hosts that are up at this instant.      |
| hosts_down         | int32    | count | The number of hosts that are down at this instant.    |
| tasks_total        | int32    | count | The number of tasks seen by the service.              |
| tasks_pending      | int32    | count | The number of tasks that are pending to be scheduled. |
| tasks_active       | int32    | count | The number of tasks that are currently active.        |
| tasks_terminated   | int32    | count | The number of tasks that were terminated.             |
| tasks_completed    | int32    | count | The number of tasks that finished successfully        |
