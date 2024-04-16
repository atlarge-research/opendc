# Documentation for the configuration input file

- the structure must be stricly respected, otherwise the simulation will fail
- for now, the input error handling is not implemented, therefore, the simulation will fail without indicating the error is related to the input

- Topology,Workload,Scheduler,FailureFrequency,MetricsCount,Metrics,FolderOutputName

## Topology
- Possible values: string that matches file name with the topology configuration
- <b>Current possible values</b>: multi / single
- Description: We have the topology configured in a file. We take from this column, a topology name. Based on that name, the simulator knows what pre-built topology to select.
- Example: multi => will take from multi.txt

## Workload
- Possible values (for now, only bitbrains-small)

## Scheduler
- Possible values:
    - "mem"
    - "mem-inv"
    - "core-mem"
    - "core-mem-inv"
    - "active-servers"
    - "active-servers-inv"
    - "random"
- Description: This represents the allocation policy / scheduler that will be used in the simulator.
- Example: mem => will use the mem allocation policy

## Failure frequency
- Possible values: any integer between 0.0 - 1.0
- <b>Current possible values</b>: any - we don't handle yet
- Description: This represents the frequency of failures in the simulator.
- Example: 0.5 => 50% of the time, a failure will occur


## Metrics count
- Possible values: any integer greater than 0
- Description: This represents how many metrics will we have in the simulator. The number of metrics, represents how many columns will we further read.
- Example: MetricsCount = 3 means we'll have 3 metrics, and we will read the following 3 columns

## Metrics to analyze
- Possible Values
    - cpu_limit (performance metric)
    - cpu_usage (performance metric)
    - cpu_demand (performance metric)
    - cpu_utilization (performance metric)
    - cpu_time_active (performance metric)
    - cpu_time_idle (performance metric)
    - cpu_time_steal (performance metric)
    - cpu_time_lost (performance metric)
    - power_total (energy metric)

## Folder output name
- Possible values: [OPTIONAL] if desired, any string, without spaces and special characters (only letters, numbers, - and _)
- Description: can be left empty, this way the simulator will generate inside a folder that has the current date: yyyy-mm-dd-hh-mm-ss
- !IMPORTANT: the folder output name should be the same for all the configs
- Example: my-simulation => will generate a folder with the name my-simulation
- Example2: nothing => will generate a folder with the name e.g., 2024-01-07-13-47-23

