
Running OpenDC results in three output files. The first file ([Server](#server)) contains metrics related to the jobs being executed. 
The second file ([Host](#host)) contains all metrics related to the hosts on which jobs can be executed. Finally, the third file ([Service](#service))
contains metrics describing the overall performance. An experiment in OpenDC has 

### Server
The server output file, contains all metrics of related to the servers run.  

| Metric          | Datatype | Unit          | Summary                                                                       |
|-----------------|----------|---------------|-------------------------------------------------------------------------------|
| timestamp       | int      | datetime      | Timestamp of the sample                                                       |
| server_id       | string   |               | The id of the server determined during runtime                                |
| server_name     | string   |               | The name of the server provided by the Trace                                  |
| host_id         | string   |               | The id of the host on which the server is hosted or `null` if it has no host. |
| mem_capacity    | int      | Mb            |                                                                               |
| cpu_count       | int      | count         |                                                                               |
| cpu_limit       | float    | MHz           | The capacity of the CPUs of Host on which the server is running.              |
| cpu_time_active | int      | seconds       | The duration that a CPU was active in the server.                             |
| cpu_time_idle   | int      | seconds       | The duration that a CPU was idle in the server.                               |
| cpu_time_steal  | int      | seconds       | The duration that a vCPU wanted to run, but no capacity was available.        |
| cpu_time_lost   | int      | seconds       | The duration of CPU time that was lost due to interference.                   |
| uptime          | int      | milli seconds | The uptime of the host since last sample.                                     |
| downtime        | int      | milli seconds | The downtime of the host since last sample.                                   |
| provision_time  | int      | datetime      | The time at which the server was enqueued for the scheduler.                  |
| boot_time       | int      | datetime      | The time at which the server booted.                                          |

### Host
The host output file, contains all metrics of related to the host run.

| Metric            | DataType | Unit          | Summary                                                                                         |
|-------------------|----------|---------------|-------------------------------------------------------------------------------------------------|
| timestamp         | int      | datetime      | Timestamp of the sample                                                                         |
| host_id           | string   |               | The id of the host given by OpenDC                                                              |
| cpu_count         | int      | count         | The number of available cpu cores                                                               |
| mem_capacity      | int      | Mb            | The amount of available memory                                                                  |
| guests_terminated | int      | count         | The number of guests that are in a terminated state.                                            |
| guests_running    | int      | count         | The number of guests that are in a running state.                                               |
| guests_error      | int      | count         | The number of guests that are in an error state.                                                |
| guests_invalid    | int      | count         | The number of guests that are in an unknown state.                                              |
| cpu_limit         | float    | MHz           | The capacity of the CPUs in the host.                                                           |
| cpu_usage         | float    | MHz           | The usage of all CPUs in the host.                                                              |
| cpu_demand        | float    | MHz           | The demand of all vCPUs of the guests                                                           |
| cpu_utilization   | float    | ratio         | The CPU utilization of the host. This is calculated by dividing the cpu_usage, by the cpu_limit |
| cpu_time_active   | int      | seconds       | The duration that a CPU was active in the host.                                                 |
| cpu_time_idle     | int      | seconds       | The duration that a CPU was idle in the host.                                                   |
| cpu_time_steal    | int      | seconds       | The duration that a vCPU wanted to run, but no capacity was available.                          |
| cpu_time_lost     | int      | seconds       | The duration of CPU time that was lost due to interference.                                     |
| power_draw        | float    | Watt          | The current power draw of the host.                                                             |
| energy_usage      | float    | Joule (Ws)    | he total energy consumption of the host since last sample.                                      |
| uptime            | int      | milli seconds | The uptime of the host since last sample.                                                       |
| downtime          | int      | milli seconds | The downtime of the host since last sample.                                                     |
| boot_time         | int      | datetime      | The timestamp at which the host booted.                                                         |

### Service
The service output file, contains metrics providing an overview of the performance.

| Metric           | DataType | Unit     | Summary                                                                |
|------------------|----------|----------|------------------------------------------------------------------------|
| timestamp        | int      | datetime | Timestamp of the sample                                                |
| hosts_up         | int      | count    | The number of hosts that are up at this instant.                       |
| hosts_down       | int      | count    | The number of hosts that are down at this instant.                     |
| servers_pending  | int      | count    | The number of servers that are pending to be scheduled.                |
| servers_active   | int      | count    | The number of servers that are currently active.                       |
| attempts_success | int      | count    | The scheduling attempts that were successful.                          |
| attempts_failure | int      | count    | The scheduling attempts that were unsuccessful due to client error.    |
| attempts_error   | int      | count    | The scheduling attempts that were unsuccessful due to scheduler error. |
