OpenDC works with two types of traces that describe the servers that need to be run. Both traces have to be provided as
parquet files.

#### Task
The meta trace provides an overview of the servers:

| Metric          | Datatype | Unit     | Summary                                          |
|-----------------|----------|----------|--------------------------------------------------|
| id              | string   |          | The id of the server                             |
| submission_time | int64    | datetime | The submission time of the server                |
| duration        | int64    | datetime | The finish time of the submission                |
| cpu_count       | int32    | count    | The number of CPUs required to run this server   |
| cpu_capacity    | float64  | MHz      | The amount of CPU required to run this server    |
| mem_capacity    | int64    | MB       | The amount of memory required to run this server |

#### Fragment
The Fragment file provides information about the computational demand of each server over time:

| Metric    | Datatype   | Unit          | Summary                                     |
|-----------|------------|---------------|---------------------------------------------|
| id        | string     |               | The id of the task                          |
| duration  | int64      | milli seconds | The duration since the last sample          |
| cpu_count | int32      | count         | The number of cpus required                 |
| cpu_usage | float64    | MHz           | The amount of computational power required. |
