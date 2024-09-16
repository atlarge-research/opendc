
Running OpenDC results in three output files. The first file ([Server](#server)) contains metrics related to the jobs being executed. 
The second file ([Host](#host)) contains all metrics related to the hosts on which jobs can be executed. Finally, the third file ([Service](#service))
contains metrics describing the overall performance. An experiment in OpenDC has 

### Server
The server output file, contains all metrics of related to the servers run.  

| Metric             | Datatype | Unit   | Summary                                                                       |
|--------------------|----------|--------|-------------------------------------------------------------------------------|
| timestamp          | int64    | ms     | Timestamp of the sample since the start of the workload                       |
| absolute timestamp | int64    | ms     | The absolute timestamp based on the given workload                            |
| server_id          | binary   | string | The id of the server determined during runtime                                |
| server_name        | binary   | string | The name of the server provided by the Trace                                  |
| host_id            | binary   | string | The id of the host on which the server is hosted or `null` if it has no host. |
| mem_capacity       | int64    | Mb     |                                                                               |
| cpu_count          | int32    | count  |                                                                               |
| cpu_limit          | double   | MHz    | The capacity of the CPUs of Host on which the server is running.              |
| cpu_time_active    | int64    | ms     | The duration that a CPU was active in the server.                             |
| cpu_time_idle      | int64    | ms     | The duration that a CPU was idle in the server.                               |
| cpu_time_steal     | int64    | ms     | The duration that a vCPU wanted to run, but no capacity was available.        |
| cpu_time_lost      | int64    | ms     | The duration of CPU time that was lost due to interference.                   |
| uptime             | int64    | ms     | The uptime of the host since last sample.                                     |
| downtime           | int64    | ms     | The downtime of the host since last sample.                                   |
| provision_time     | int64    | ms     | The time for which the server was enqueued for the scheduler.                 |
| boot_time          | int64    | ms     | The time the server took booting.                                             |

### Host
The host output file, contains all metrics of related to the host run.

| Metric             | DataType | Unit       | Summary                                                                                         |
|--------------------|----------|------------|-------------------------------------------------------------------------------------------------|
| timestamp          | int64    | ms         | Timestamp of the sample                                                                         |
| absolute timestamp | int64    | ms         | The absolute timestamp based on the given workload                                              |
| host_id            | binary   | string     | The id of the host given by OpenDC                                                              |
| cpu_count          | int32    | count      | The number of available cpuModel cores                                                               |
| mem_capacity       | int64    | Mb         | The amount of available memory                                                                  |
| guests_terminated  | int32    | count      | The number of guests that are in a terminated state.                                            |
| guests_running     | int32    | count      | The number of guests that are in a running state.                                               |
| guests_error       | int32    | count      | The number of guests that are in an error state.                                                |
| guests_invalid     | int32    | count      | The number of guests that are in an unknown state.                                              |
| cpu_limit          | double   | MHz        | The capacity of the CPUs in the host.                                                           |
| cpu_usage          | double   | MHz        | The usage of all CPUs in the host.                                                              |
| cpu_demand         | double   | MHz        | The demand of all vCPUs of the guests                                                           |
| cpu_utilization    | double   | ratio      | The CPU utilization of the host. This is calculated by dividing the cpu_usage, by the cpu_limit |
| cpu_time_active    | int64    | ms         | The duration that a CPU was active in the host.                                                 |
| cpu_time_idle      | int64    | ms         | The duration that a CPU was idle in the host.                                                   |
| cpu_time_steal     | int64    | ms         | The duration that a vCPU wanted to run, but no capacity was available.                          |
| cpu_time_lost      | int64    | ms         | The duration of CPU time that was lost due to interference.                                     |
| power_draw         | double   | Watt       | The current power draw of the host.                                                             |
| energy_usage       | double   | Joule (Ws) | The total energy consumption of the host since last sample.                                     |
| carbon_intensity   | double   | gCO2/kW    | The amount of carbon that is emitted when using a unit of energy                                |
| carbon_emission    | double   | gram       | The amount of carbon emitted since the previous sample                                          |
| uptime             | int64    | ms         | The uptime of the host since last sample.                                                       |
| downtime           | int64    | ms         | The downtime of the host since last sample.                                                     |
| boot_time          | int64    | ms         | The time the host took to boot.                                                                 |

### Service
The service output file, contains metrics providing an overview of the performance.

| Metric             | DataType | Unit  | Summary                                                                |
|--------------------|----------|-------|------------------------------------------------------------------------|
| timestamp          | int64    | ms    | Timestamp of the sample                                                |
| absolute timestamp | int64    | ms    | The absolute timestamp based on the given workload                     |
| hosts_up           | int32    | count | The number of hosts that are up at this instant.                       |
| hosts_down         | int32    | count | The number of hosts that are down at this instant.                     |
| servers_pending    | int32    | count | The number of servers that are pending to be scheduled.                |
| servers_active     | int32    | count | The number of servers that are currently active.                       |
| attempts_success   | int32    | count | The scheduling attempts that were successful.                          |
| attempts_failure   | int32    | count | The scheduling attempts that were unsuccessful due to client error.    |
| attempts_error     | int32    | count | The scheduling attempts that were unsuccessful due to scheduler error. |
