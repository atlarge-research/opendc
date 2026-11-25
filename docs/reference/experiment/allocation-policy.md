---
description: Allocation policies, which decide on which host each task runs, and when.
---

# Allocation Policy

An *allocation policy* decides on which host each task runs, and when. A policy is either a *filter policy*, which is
built from filters and weighers, or a *prefab*, which is a filter policy with a name. This page describes filter
policies first, because prefabs are defined in terms of them.

## Filter policies

A filter policy places each task in two steps:

1. The *filters* remove the hosts that cannot run the task.
2. The *weighers* score the remaining hosts. OpenDC scales the scores of each weigher to the range from 0 to its
   `multiplier` and adds them up. The task goes to the host with the highest total. A negative multiplier makes a
   weigher prefer low values over high ones.

A filter policy has the type `filter`:

| Field       | Type                              | Default                    | Description                                              |
|-------------|-----------------------------------|----------------------------|----------------------------------------------------------|
| `filters`   | list of [filters](#filters)       | `[{ "type": "compute" }]`  | The filters, all of which a host has to pass.            |
| `weighers`  | list of [weighers](#weighers)     | `[]`                       | The weighers that rank the hosts.                        |
| `timeshift` | [time-shifting](#time-shifting)   | none                       | Delays tasks while the carbon intensity is high.         |

``` json
{
    "type": "filter",
    "filters": [
        { "type": "compute" },
        { "type": "vcpu", "allocationRatio": 1.0 },
        { "type": "ram" }
    ],
    "weighers": [
        { "type": "ram", "multiplier": 1.0 }
    ]
}
```

### Filters

| Type            | Further fields                     | Keeps the hosts that                                                                 |
|-----------------|------------------------------------|--------------------------------------------------------------------------------------|
| `compute`       |                                    | are up                                                                               |
| `vcpu`          | `allocationRatio` (default `1.0`)  | have enough free CPU cores. An `allocationRatio` above 1 allows overcommitting cores. |
| `vcpuCapacity`  |                                    | have cores at least as fast as the task needs                                        |
| `ram`           | `allocationRatio` (default `1.0`)  | have enough free memory                                                              |
| `vgpu`          | `allocationRatio` (default `1.0`)  | have enough free GPU cores                                                           |
| `instanceCount` | `limit`                            | run fewer than `limit` tasks                                                         |
| `sameHost`      |                                    | all hosts. This filter currently has no effect.                                      |
| `differentHost` |                                    | all hosts. This filter currently has no effect.                                      |

!!! warning "Do not overcommit memory"

    Leave the `allocationRatio` of the `ram` filter at 1. OpenDC does not support overcommitting memory yet.

### Weighers

Every weigher has a `multiplier`, which is `1.0` by default.

| Type            | Scores a host by                                                                    |
|-----------------|-------------------------------------------------------------------------------------|
| `ram`           | its free memory                                                                     |
| `coreRam`       | its free memory per core                                                            |
| `instanceCount` | the number of tasks it runs                                                         |
| `vcpuCapacity`  | its total CPU capacity                                                              |
| `vcpu`          | how few of its CPU cores are provisioned. A higher score means fewer cores in use.  |
| `vgpu`          | how few of its GPU cores are provisioned                                            |

### Time-shifting

*Time-shifting* delays tasks to moments at which energy is cleaner. With `timeshift`, the policy holds back
[deferrable](workload.md#trace-workloads) tasks while the carbon intensity of the power source is high, as long as they
can still meet their deadline. The carbon intensity comes from the
[carbon model](topology/power-source.md#carbon-models) of the power source.

The policy treats short and long tasks differently: tasks shorter than two hours use `shortForecastThreshold`, and
longer tasks use `longForecastThreshold`.

| Field                    | Type                            | Default | Description                                                                                        |
|--------------------------|---------------------------------|---------|----------------------------------------------------------------------------------------------------|
| `forecast`               | boolean                         | `true`  | Compares the carbon intensity with a forecast. Otherwise, the policy compares it with the mean of the last `windowSize` values. |
| `forecastSize`           | integer                         | `24`    | The number of forecast values to compare with.                                                     |
| `shortForecastThreshold` | number                          | `0.2`   | For short tasks, the carbon intensity is low when it is below this quantile of the forecast.       |
| `longForecastThreshold`  | number                          | `0.35`  | The same quantile for long tasks.                                                                  |
| `windowSize`             | integer                         | `168`   | The number of past values in the mean.                                                             |
| `taskStopper`            | [task stopper](#task-stopper)   | none    | Also pauses running tasks while the carbon intensity is high.                                      |

#### Task stopper

A *task stopper* pauses tasks that already run, so that they too wait for a better moment.

| Field               | Type    | Default | Description                                                         |
|---------------------|---------|---------|---------------------------------------------------------------------|
| `forecast`          | boolean | `true`  | Uses a forecast instead of past values.                             |
| `forecastSize`      | integer | `24`    | The number of forecast values.                                      |
| `forecastThreshold` | number  | `0.6`   | The threshold, between 0 and 1, above which tasks are paused.       |
| `windowSize`        | integer | `168`   | The number of past values used.                                     |

``` json
{
    "type": "filter",
    "filters": [{ "type": "compute" }, { "type": "vcpu" }, { "type": "ram" }],
    "weighers": [{ "type": "ram" }],
    "timeshift": {
        "shortForecastThreshold": 0.2,
        "longForecastThreshold": 0.35,
        "taskStopper": { "forecastThreshold": 0.6 }
    }
}
```

## Prefabs

A prefab is a ready-made filter policy, selected by name:

``` json
{ "type": "prefab", "prefabName": "Mem" }
```

All prefabs use the filters `compute`, `vcpu` and `ram`. The three prefabs for GPUs add the filter `vgpu`. Table 1 lists
the weighers of each prefab, with their multipliers in parentheses.

| Prefab                      | Weighers                          | Places a task on the host with                                |
|-----------------------------|-----------------------------------|---------------------------------------------------------------|
| `Mem`                       | `ram` (1.0)                       | the most free memory. This is the default policy.             |
| `MemInv`                    | `ram` (−1.0)                      | the least free memory                                         |
| `CoreMem`                   | `coreRam` (1.0)                   | the most free memory per core                                 |
| `CoreMemInv`                | `coreRam` (−1.0)                  | the least free memory per core                                |
| `ActiveServers`             | `instanceCount` (−1.0)            | the fewest tasks                                              |
| `ActiveServersInv`          | `instanceCount` (1.0)             | the most tasks                                                |
| `ProvisionedCores`          | `vcpu` (1.0)                      | the fewest provisioned CPU cores                              |
| `ProvisionedCoresInv`       | `vcpu` (−1.0)                     | the most provisioned CPU cores                                |
| `TaskNumMemorizing`         | `instanceCount` (−1.0)            | the fewest tasks                                              |
| `Timeshift`                 | `ram` (1.0)                       | the most free memory, with time-shifting at its defaults      |
| `ProvisionedCpuGpuCores`    | `vcpu` (1.0), `vgpu` (1.0)        | the fewest provisioned CPU and GPU cores                      |
| `ProvisionedCpuGpuCoresInv` | `vcpu` (−1.0), `vgpu` (−1.0)      | the most provisioned CPU and GPU cores                        |
| `GpuTaskMemorizing`         | `instanceCount` (−1.0)            | the fewest tasks                                              |

/// table-caption
The prefabs. The last three are the prefabs for GPUs.
///
