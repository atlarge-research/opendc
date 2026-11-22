---
description: Allocation policies in OpenDC 2, which decide on which host each task runs, and when.
status: deprecated
search:
  exclude: true
---

# Allocation Policy

An *allocation policy* decides on which [host](topology.md#host) each [task](workload.md) runs, and when. This page
describes the three kinds of allocation policy and the filters and weighers from which they are built.

The `type` field selects the kind of policy:

| Type        | Description                                                                                                     |
|-------------|-----------------------------------------------------------------------------------------------------------------|
| `prefab`    | A ready-made policy, selected by name. See [Prefabs](#prefabs).                                                 |
| `filter`    | A policy built from [filters](#filters) and [weighers](#weighers).                                              |
| `timeshift` | A filter policy that also delays tasks while the carbon intensity is high. See [Time-shifting](#time-shifting). |

## Filter policies

A *filter policy* places each task in two steps:

1. The *filters* remove the hosts that cannot run the task.
2. The *weighers* rank the remaining hosts. The score of each weigher is weighted by its `multiplier`. A negative
   multiplier prefers low values instead of high ones.

The task goes to the best host, or to a random host among the `subsetSize` best hosts. A filter policy has the following
fields:

| Field        | Type                          | Default                   | Description                                                  |
|--------------|-------------------------------|---------------------------|--------------------------------------------------------------|
| `filters`    | list of [filters](#filters)   | `[{ "type": "Compute" }]` | The filters, all of which a host must pass.                  |
| `weighers`   | list of [weighers](#weighers) | `[]`                      | The weighers that rank the hosts.                            |
| `subsetSize` | integer                       | `1`                       | The number of best hosts from which one is picked at random. |

The following filter policy keeps the hosts that are up and have enough free cores and memory, and prefers the host
with the most free memory:

``` json
{
    "type": "filter",
    "filters": [
        { "type": "Compute" },
        { "type": "VCpu", "allocationRatio": 1.0 },
        { "type": "Ram" }
    ],
    "weighers": [
        { "type": "Ram", "multiplier": 1.0 }
    ]
}
```

### Filters

The `type` field selects the filter:

| Type            | Fields                            | Behaviour                                                                                                               |
|-----------------|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `Compute`       |                                   | Keeps the hosts that are up.                                                                                            |
| `VCpu`          | `allocationRatio` (default `1.0`) | Keeps the hosts that have enough free CPU cores. An `allocationRatio` above 1 allows overcommitting cores.              |
| `VCpuCapacity`  |                                   | Keeps the hosts that have cores at least as fast as the task needs.                                                     |
| `Ram`           | `allocationRatio` (default `1.0`) | Keeps the hosts that have enough free memory. Do not change `allocationRatio`: overcommitting memory is not supported.  |
| `InstanceCount` | `limit`                           | Keeps the hosts that run fewer than `limit` tasks.                                                                      |
| `SameHost`      |                                   | Currently has no effect.                                                                                                |
| `DifferentHost` |                                   | Currently has no effect.                                                                                                |

### Weighers

Every weigher has a `multiplier`, which is `1.0` by default. The `type` field selects the weigher:

| Type            | Scores a host by                         |
|-----------------|------------------------------------------|
| `Ram`           | its free memory                          |
| `CoreRam`       | its free memory per core                 |
| `InstanceCount` | the number of tasks it runs              |
| `VCpuCapacity`  | its CPU capacity                         |
| `VCpu`          | how few CPU cores are provisioned on it  |

For the `VCpu` weigher, a higher score means fewer cores in use.

## Time-shifting

A *time-shifting policy* has the type `timeshift`. It filters and weighs hosts in the same way as a filter policy, but
also delays [deferrable](workload.md#tasks-file) tasks while the carbon intensity is high, as long as they can still
meet their deadline. The carbon intensity comes from the carbon trace of the
[power source](topology.md#power-source).

Tasks shorter than 2&nbsp;hours use `shortForecastThreshold`, and longer tasks use `longForecastThreshold`. A
time-shifting policy has the following fields:

| Field                    | Type                          | Default                   | Description                                                                                                                    |
|--------------------------|-------------------------------|---------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `filters`                | list of [filters](#filters)   | `[{ "type": "Compute" }]` | The filters, all of which a host must pass.                                                                                    |
| `weighers`               | list of [weighers](#weighers) | `[]`                      | The weighers that rank the hosts.                                                                                              |
| `subsetSize`             | integer                       | `1`                       | The number of best hosts from which one is picked at random.                                                                   |
| `forecast`               | boolean                       | `true`                    | Compare the carbon intensity with a forecast. Otherwise, compare it with the mean of the last `windowSize` values.             |
| `forecastSize`           | integer                       | `24`                      | The number of forecast values to compare with.                                                                                 |
| `shortForecastThreshold` | number                        | `0.2`                     | The carbon intensity is low for short tasks when it is below this quantile of the forecast.                                    |
| `longForecastThreshold`  | number                        | `0.35`                    | The same, for long tasks.                                                                                                      |
| `windowSize`             | integer                       | `168`                     | The number of past values in the mean.                                                                                         |
| `memorize`               | boolean                       | `true`                    | Use the faster memorising scheduler, which places each task on a fitting host with the fewest tasks and ignores the weighers.  |
| `taskStopper`            | [task stopper](#task-stopper) | none                      | Also pauses running tasks while the carbon intensity is high.                                                                  |

### Task stopper

A *task stopper* also pauses running tasks while the carbon intensity is high. It has the following fields:

| Field               | Type    | Default | Description                                                   |
|---------------------|---------|---------|---------------------------------------------------------------|
| `forecast`          | boolean | `true`  | Use a forecast instead of past values.                        |
| `forecastSize`      | integer | `24`    | The number of forecast values.                                |
| `forecastThreshold` | number  | `0.6`   | The threshold, between 0 and 1, above which tasks are paused. |
| `windowSize`        | integer | `168`   | The number of past values used.                               |

The following time-shifting policy sets both forecast thresholds and has a task stopper:

``` json
{
    "type": "timeshift",
    "filters": [{ "type": "Compute" }, { "type": "VCpu" }, { "type": "Ram" }],
    "weighers": [{ "type": "Ram" }],
    "shortForecastThreshold": 0.2,
    "longForecastThreshold": 0.35,
    "taskStopper": { "forecastThreshold": 0.6 }
}
```

## Prefabs

A *prefab* is a ready-made policy. The `policyName` field selects it by name, and the name is not case-sensitive:

``` json
{ "type": "prefab", "policyName": "Mem" }
```

Unless noted otherwise, the prefabs use the `Compute`, `VCpu` and `Ram` filters. The GPU prefabs also check that a host
has enough free GPU cores. The following table lists the prefabs, with the multiplier of each weigher in parentheses:

| Prefab                      | Weighers                         | Notes                                                                 |
|-----------------------------|----------------------------------|-----------------------------------------------------------------------|
| `Mem`                       | `Ram` (1.0)                      | The default policy. Uses only the `VCpu` and `Ram` filters.           |
| `MemInv`                    | `Ram` (-1.0)                     |                                                                       |
| `CoreMem`                   | `CoreRam` (1.0)                  |                                                                       |
| `CoreMemInv`                | `CoreRam` (-1.0)                 |                                                                       |
| `ActiveServers`             | `InstanceCount` (-1.0)           |                                                                       |
| `ActiveServersInv`          | `InstanceCount` (1.0)            |                                                                       |
| `ProvisionedCores`          | `VCpu` (1.0)                     |                                                                       |
| `ProvisionedCoresInv`       | `VCpu` (-1.0)                    |                                                                       |
| `Random`                    | none                             | Picks a random host that passes the filters.                          |
| `TaskNumMemorizing`         | none                             | Places each task on a fitting host with the fewest tasks.             |
| `Timeshift`                 | `Ram` (1.0)                      | With time-shifting, using its defaults.                               |
| `ProvisionedCpuGpuCores`    | `VCpu` (1.0), GPU cores (1.0)    | GPU prefab.                                                           |
| `ProvisionedCpuGpuCoresInv` | `VCpu` (-1.0), GPU cores (-1.0)  | GPU prefab.                                                           |
| `GpuTaskMemorizing`         | none                             | GPU prefab. Places each task on a fitting host with the fewest tasks. |
