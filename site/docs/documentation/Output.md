
Running OpenDC results in three output files. The first file ([Server]) contains metrics related to the jobs being executed. 
The second file ([Host]) contains all metrics related to the hosts on which jobs can be executed. Finally, the third file ([Service])
contains metrics describing the overall performance. An experiment in OpenDC has 

### Server
The first parquet file created by OpenDC is Server.parquet. Server.parquet reports all metrics related to to the different 
jobs. 

| Metric          | Datatype | Summary | Unit |
|-----------------|----------|---------|------|
| timestamp       | datetime |         |      |
| server_id       | string   |         |      |
| server_name     | string   |         |      |
| host_id         | string   |         |      |
| mem_capacity    | int      |         |      |
| cpu_count       | int      |         |      |
| cpu_limit       | float    |         |      |
| cpu_time_active | int      |         |      |
| cpu_time_idle   | int      |         |      |
| cpu_time_steal  | int      |         |      |
| cpu_time_lost   | int      |         |      |
| uptime          | int      |         |      |
| downtime        | int      |         |      |
| provision_time  | datetime |         |      |
| boot_time       | datetime |         |      |

### Host

| Metric            | DataType | Summary | Unit |
|-------------------|----------|---------|------|
| timestamp         | datetime |         |      |
| host_id           | string   |         |      |
| cpu_count         | int      |         |      |
| mem_capacity      | int      |         |      |
| guests_terminated | int      |         |      |
| guests_running    | int      |         |      |
| guests_error      | int      |         |      |
| guests_invalid    | int      |         |      |
| cpu_limit         | float    |         |      |
| cpu_usage         | float    |         |      |
| cpu_demand        | float    |         |      |
| cpu_utilization   | float    |         |      |
| cpu_time_active   | int      |         |      |
| cpu_time_idle     | int      |         |      |
| cpu_time_steal    | int      |         |      |
| cpu_time_lost     | int      |         |      |
| power_draw        | float    |         |      |
| energy_usage      | float    |         |      |
| uptime            | int      |         |      |
| downtime          | int      |         |      |
| boot_time         | datetime |         |      |

### Service

| Metric           | DataType | Summary | Unit |
|------------------|----------|---------|------|
| timestamp        | datetime |         |      |
| hosts_up         | int      |         |      |
| hosts_down       | int      |         |      |
| servers_pending  | int      |         |      |
| servers_active   | int      |         |      |
| attempts_success | int      |         |      |
| attempts_failure | int      |         |      |
| attempts_error   | int      |         |      |
