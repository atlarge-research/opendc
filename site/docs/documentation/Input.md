
OpenDC requires three files to run an experiment. First is the topology of the data center that will be simulated. 
Second, is a meta trace providing an overview of the servers that need to be executed. Third is the trace describing the 
computational demand of each job over time. 

### Topology
The topology of a datacenter is described by a csv file. Each row in the csv is a cluster 
of in the data center. Below is an example of a topology file consisting of three clusters:

| ClusterID | ClusterName | Cores | Speed | Memory | numberOfHosts | memoryCapacityPerHost | coreCountPerHost |
|-----------|-------------|-------|-------|--------|---------------|-----------------------|------------------|
| A01       | A01         | 32    | 3.2   | 2048   | 1             | 256                   | 32               |
| B01       | B01         | 48    | 2.93  | 1256   | 6             | 64                    | 8                |
| C01       | C01         | 32    | 3.2   | 2048   | 2             | 128                   | 16               |


### Traces
OpenDC works with two types of traces that describe the servers that need to be run. Both traces have to be provided as 
parquet files.

#### Meta
The meta trace provides an overview of the servers:

| Metric       | Datatype   | Unit     | Summary                                          |
|--------------|------------|----------|--------------------------------------------------|
| id           | string     |          | The id of the server                             |
| start_time   | datetime64 | datetime | The submission time of the server                |
| stop_time    | datetime64 | datetime | The finish time of the submission                |
| cpu_count    | int32      | count    | The number of CPUs required to run this server   |
| cpu_capacity | float64    | MHz      | The amount of CPU required to run this server    |
| mem_capacity | int64      | MB       | The amount of memory required to run this server |

#### Trace
The Trace file provides information about the computational demand of each server over time:

| Metric    | Datatype   | Unit          | Summary                                     |
|-----------|------------|---------------|---------------------------------------------|
| id        | string     |               | The id of the server                        |
| timestamp | datetime64 | datetime      | The timestamp of the sample                 |
| duration  | int64      | milli seconds | The duration since the last sample          |
| cpu_count | int32      | count         | The number of cpus required                 |
| cpu_usage | float64    | MHz           | The amount of computational power required. |
