Workloads define what tasks in the simulation, when they were submitted, and their computational requirements.
Workload are defined using two files: 

- **[Tasks](#tasks)**: The Tasks file contains the metadata of the tasks
- **[Fragments](#fragments)**: The Fragments file contains the computational demand of each task over time

Both files are provided using the parquet format. 

#### Tasks
The Tasks file provides an overview of the tasks:

| Metric           | Required? | Datatype | Unit                         | Summary                                                             |
|------------------|-----------|----------|------------------------------|---------------------------------------------------------------------|
| id               | Yes       | string   |                              | The id of the server                                                |
| submission_time  | Yes       | int64    | datetime                     | The submission time of the server                                   |
| nature           | No        | string   | [deferrable, non-deferrable] | Defines if a task can be delayed                                    |
| deadline         | No        | string   | datetime                     | The latest the scheduling of a task can be delayed to.              |
| duration         | Yes       | int64    | datetime                     | The finish time of the submission                                   |
| cpu_count        | Yes       | int32    | count                        | The number of CPUs required to run this task                        |
| cpu_capacity     | Yes       | float64  | MHz                          | The amount of CPU required to run this task                         |
| mem_capacity     | Yes       | int64    | MB                           | The amount of memory required to run this task                      |
| gpu_count        | No        | int32    | count                        | The number of GPUs required to run this task                        |
| gpu_capacity     | No        | float64  | MHz                          | The amount of GPU required to run this task                         |
| gpu_mem_capacity | No        | int64    | MB                           | The amount of memory required to run this task. (Currently ignored) |

#### Fragments
The Fragments file provides information about the computational demand of each task over time:

| Metric    | Required? | Datatype | Unit          | Summary                                         |
|-----------|-----------|----------|---------------|-------------------------------------------------|
| id        | Yes       | string   |               | The id of the task                              |
| duration  | Yes       | int64    | milli seconds | The duration since the last sample              |
| cpu_count | Yes       | int32    | count         | The number of cpus required                     |
| cpu_usage | Yes       | float64  | MHz           | The amount of computational CPU power required. |
| gpu_count | No        | int32    | count         | The number of gpus required                     |
| gpu_usage | No        | float64  | MHz           | The amount of computational GPU power required. |
