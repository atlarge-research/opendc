Allocation policies define how, when and where a task is executed.

There are two types of allocation policies:
1. **[Filter](#filter-policy)** - The basic allocation policy that selects a host for each task based on filters and weighters
2. **[TimeShift](#timeshift-policy)** - Extends the Filter scheduler allowing tasks to be delayed to better align with the availability of low-carbon power. 

In the following section we discuss the different allocation policies, and how to define them in an Experiment file. 

## Filter policy
To use a filter scheduler, the user has to set the type of the policy to "filter". 
A filter policy requires a list of filters and weighters which characterize the policy.

A filter policy consists of two main components:
1. **[Filters](#filters)** - Filters select all hosts that are eligible to execute the given task.
2. **[Weighters](#weighters)** - Weighters are used to rank the eligible hosts. The host with the highest weight is selected to execute the task.

:::info Code
All code related to reading Allocation policies can be found [here](https://github.com/atlarge-research/opendc/blob/master/opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/experiment/specs/allocation/AllocationPolicySpec.kt)
:::

### Filters
Filters select all hosts that are eligible to execute the given task.
Filters are defined as JSON objects in the experiment file.

The user defines which filter to use by setting the "type".
OpenDC currently supports the following 7 filters:

#### ComputeFilter
Returns host if it is running. 
Does not require any more parameters.

```json
{
    "type": "Compute"
}
```

#### SameHostHostFilter
Ensures that after failure, a task is executed on the same host again.
Does not require any more parameters.

```json
{
    "type": "DifferentHost"
}
```

#### DifferentHostFilter
Ensures that after failure, a task is *not* executed on the same host again.
Does not require any more parameters.

```json
{
    "type": "DifferentHost"
}
```

#### InstanceCountHostFilter
Returns host if the number of instances running on the host is less than the maximum number of instances allowed.
The User needs to provide the maximum number of instances that can be run on a host.
```json
{
    "type": "InstanceCount",
    "limit": 1
}
```

#### RamHostFilter
Returns hosts if the amount of RAM available on the host is greater than the amount of RAM required by the task.
The user can provide an allocationRatio which is multiplied with the amount of RAM available on the host.
This can be used to allow for over subscription.
```json
{
    "type": "Ram",
    "allocationRatio": 2.5
}
```

#### VCpuCapacityHostFilter
Returns hosts if CPU capacity available on the host is greater than the CPU capacity required by the task.

```json
{
    "type": "VCpuCapacity"
}
```

#### VCpuHostFilter
Returns host if the number of cores available on the host is greater than the number of cores required by the task.
The user can provide an allocationRatio which is multiplied with the amount of RAM available on the host.
This can be used to allow for over subscription.

```json
{
    "type": "VCpu",
    "allocationRatio": 2.5
}
```

:::info Code
All code related to reading Filters can be found [here](https://github.com/atlarge-research/opendc/blob/master/opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/experiment/specs/allocation/HostFilterSpec.kt)
:::

### Weighters
Weighters are used to rank the eligible hosts. The host with the highest weight is selected to execute the task.
Weighters are defined as JSON objects in the experiment file.

The user defines which filter to use by setting the "type".
The user can also provide a multiplying that is multiplied with the weight of the host.
This can be used to increase or decrease the importance of the host.
Negative multipliers are also allowed, and can be used to invert the ranking of the host.
OpenDC currently supports the following 5 weighters:

#### RamWeigherSpec
Order the hosts by the amount of RAM available on the host.

```json
{
    "type": "Ram",
    "multiplier": 2.0
}
```

#### CoreRamWeighter
Order the hosts by the amount of RAM available per core on the host.

```json
{
    "type": "CoreRam",
    "multiplier": 0.5
}
```

#### InstanceCountWeigherSpec
Order the hosts by the number of instances running on the host.

```json
{
    "type": "InstanceCount",
    "multiplier": -1.0
}
```

#### VCpuCapacityWeigherSpec
Order the hosts by the capacity per core on the host.

```json
{
    "type": "VCpuCapacity",
    "multiplier": 0.5
}
```

#### VCpuWeigherSpec
Order the hosts by the number of cores available on the host.

```json
{
    "type": "VCpu",
    "multiplier": 2.5
}
```

:::info Code
All code related to reading Weighters can be found [here](https://github.com/atlarge-research/opendc/blob/master/opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/experiment/specs/allocation/HostWeigherSpec.kt)
:::

### Examples
Following is an example of a Filter policy:
```json
{
   "type": "filter",
   "filters": [
        {
           "type": "Compute"
         },
         {
            "type": "VCpu",
             "allocationRatio": 1.0
         },
         {
             "type": "Ram",
             "allocationRatio": 1.5
         }
   ],
   "weighers": [
      {
         "type": "Ram",
         "multiplier": 1.0
      }
   ]
}
```

## TimeShift policy
Timeshift extends the Filter policy by allowing tasks to be delayed to better align with the availability of low-carbon power.
A user can define a timeshift policy by setting the type to "timeshift".

task is scheduled when the current carbon intensity is below the carbon threshold. Otherwise, they are delayed. The
carbon threshold is determined by taking the 35 percentile of next week’s carbon forecast. When used, tasks can be interrupted 
when the carbon intensity exceeds the threshold during execution. All tasks have a maximum delay time defined in the workload. When the maximum delay is reached,
tasks cannot be delayed anymore.


Similar to the filter policy, the user can define a list of filters and weighters.
However, in addittion, the user can provide parameters that influence how tasks are delayed:

| Variable               | Type                        | Required? | Default         | Description                                                                       |
|------------------------|-----------------------------|-----------|-----------------|-----------------------------------------------------------------------------------|
| filters                | List[Filter]                | no        | [ComputeFilter] | Filters used to select eligible hosts.                                            |
| weighters              | List[Weighter]              | no        | []              | Weighters used to rank hosts.                                                     |
| windowSize             | integer                     | no        | 168             | How far back does the scheduler look to determine the Carbon Intensity threshold? |
| forecast               | boolean                     | no        | true            | Does the the policy use carbon forecasts?                                         |
| shortForecastThreshold | double                      | no        | 0.2             | Threshold is used for short tasks (<2hours)                                       |
| longForecastThreshold  | double                      | no        | 0.35            | Threshold is used for long tasks (>2hours)                                        |
| forecastSize           | integer                     | no        | 24              | The number of hours of forecasts that is taken into account                       |
| taskStopper            | [TaskStopper](#taskstopper) | no        | null            | Policy for interrupting tasks. If not provided, tasks are never interrupted       |

### TaskStopper

Aside from delaying tasks, users might want to interrupt tasks that are running.
For example, if a tasks is running when only high-carbon energy is available, the task can be interrupted and rescheduled to a later time.

A TaskStopper is defined as a JSON object in the Timeshift policy.
A TasksStopper consists of the following components:

| Variable              | Type                        | Required? | Default | Description                                                                       |
|-----------------------|-----------------------------|-----------|---------|-----------------------------------------------------------------------------------|
| windowSize            | integer                     | no        | 168     | How far back does the scheduler look to determine the Carbon Intensity threshold? |
| forecast              | boolean                     | no        | true    | Does the the policy use carbon forecasts?                                         |
| forecastThreshold     | double                      | no        | 0.6     | Threshold is used for short tasks (<2hours)                                       |
| forecastSize          | integer                     | no        | 24      | The number of hours of forecasts that is taken into account                       |


## Prefabs
Aside from custom policies, OpenDC also provides a set of pre-defined policies that can be used.
A prefab can be defined by setting the type to "prefab" and providing the name of the prefab.

Example:
```json
{
   "type": "prefab",
   "policyName": "Mem"
}
```

The following prefabs are available:

| Name                | Filters                                      | Weighters                  | Timeshifting |
|---------------------|----------------------------------------------|----------------------------|--------------|
| Mem                 | ComputeFilter <br/>VCpuFilter<br/> RamFilter | RamWeigher(1.0)            | No           |
| MemInv              | ComputeFilter <br/>VCpuFilter<br/> RamFilter | RamWeigher(-1.0)           | No           |
| CoreMem             | ComputeFilter <br/>VCpuFilter<br/> RamFilter | CoreRamWeigher(1.0)        | No           |
| CoreMemInv          | ComputeFilter <br/>VCpuFilter<br/> RamFilter | CoreRamWeigher(-1.0)       | No           |
| ActiveServers       | ComputeFilter <br/>VCpuFilter<br/> RamFilter | InstanceCountWeigher(1.0)  | No           |
| ActiveServersInv    | ComputeFilter <br/>VCpuFilter<br/> RamFilter | InstanceCountWeigher(-1.0) | No           |
| ProvisionedCores    | ComputeFilter <br/>VCpuFilter<br/> RamFilter | VCpuWeigher(1.0)           | No           |
| ProvisionedCoresInv | ComputeFilter <br/>VCpuFilter<br/> RamFilter | VCpuWeigher(-1.0)          | No           |
| Random              | ComputeFilter <br/>VCpuFilter<br/> RamFilter | []                         | No           |
| TimeShift           | ComputeFilter <br/>VCpuFilter<br/> RamFilter | RamWeigher(1.0)            | Yes          |

:::info Code
All code related to prefab schedulers can be found [here](https://github.com/atlarge-research/opendc/blob/master/opendc-compute/opendc-compute-simulator/src/main/kotlin/org/opendc/compute/simulator/scheduler/ComputeSchedulers.kt)
:::

