---
description: The experiment file of OpenDC 2, with the failure, checkpoint and export models it configures.
status: deprecated
search:
  exclude: true
---

# Experiment File

An *experiment* defines what OpenDC 2 simulates. It is a JSON file that lists one or more topologies, workloads and
policies, and OpenDC 2 runs one simulation, called a *scenario*, for every combination of them. This page describes the
fields of the experiment file and the failure, checkpoint and export models that it configures;
[Running OpenDC 2](running.md) explains how to run an experiment.

## Fields

An experiment has the fields in the following table. Three of them refer to inputs that have their own pages: a
[topology](topology.md) defines the simulated hardware, a [workload](workload.md) defines the tasks that run on it and
an [allocation policy](allocation-policy.md) decides on which host each task runs.

| Field                | Type                                                     | Default                            | Description                                                                                        |
|----------------------|----------------------------------------------------------|------------------------------------|----------------------------------------------------------------------------------------------------|
| `name`               | string                                                   | generated                          | Name of the experiment. When the name is empty, OpenDC 2 generates one, such as `unnamed-simulation-1a2b`. |
| `outputFolder`       | string                                                   | `"output"`                         | The folder for the results, which OpenDC 2 writes to `outputFolder/name/`.                         |
| `topologies`         | list of topology files                                   | required                           | The [topologies](topology.md) to simulate, each as `{ "pathToFile": "topologies/topology.json" }`. |
| `workloads`          | list of [workloads](workload.md)                         | required                           | The tasks to run.                                                                                  |
| `allocationPolicies` | list of [allocation policies](allocation-policy.md)      | the `Mem` [prefab](allocation-policy.md#prefabs) | How tasks are placed on hosts.                                                       |
| `failureModels`      | list of [failure models](#failure-model) or `null`       | `[null]`                           | When hosts fail. `null` means no failures.                                                         |
| `maxNumFailures`     | list of integers                                         | `[10]`                             | The number of failures after which a task is terminated.                                           |
| `checkpointModels`   | list of [checkpoint models](#checkpoint-model) or `null` | `[null]`                           | How tasks checkpoint their progress. `null` turns checkpointing off.                               |
| `exportModels`       | list of [export models](#export-model)                   | one export model with its defaults | Which results are written and how often.                                                           |
| `runs`               | integer                                                  | `1`                                | How often each scenario is run, each time with a different seed.                                   |
| `initialSeed`        | integer                                                  | `0`                                | The seed of the first run. Run *i* uses seed `initialSeed + i`.                                    |

## Scenarios

Each list in the experiment is one *axis*. OpenDC 2 runs one scenario for every combination of the axes, and runs each
scenario `runs` times. An experiment with two topologies, one workload and three allocation policies has six scenarios.

OpenDC 2 numbers the scenarios from 0. The numbering goes through the lists in order, with `topologies` changing
slowest and `maxNumFailures` fastest. The number of a scenario is the name of its folder in the
[output](output.md#where-the-results-are-written), and `trackr.json` lists the settings of every scenario.

## Example

The following experiment runs three topologies, two workloads and two allocation policies, so it has 12 scenarios:

``` json
{
    "name": "example",
    "topologies": [
        { "pathToFile": "topologies/topology1.json" },
        { "pathToFile": "topologies/topology2.json" },
        { "pathToFile": "topologies/topology3.json" }
    ],
    "workloads": [
        { "pathToFile": "traces/bitbrains-small", "type": "ComputeWorkload" },
        { "pathToFile": "traces/bitbrains-large", "type": "ComputeWorkload" }
    ],
    "allocationPolicies": [
        { "type": "prefab", "policyName": "Mem" },
        { "type": "prefab", "policyName": "MemInv" }
    ],
    "exportModels": [
        { "exportInterval": 3600 }
    ]
}
```

## Failure model

A *failure model* decides when hosts fail, how long a failure lasts and how many hosts it affects. A failed host stops
all of its tasks and cannot be used until the failure ends. OpenDC 2 terminates a task that has failed
`maxNumFailures` times, which is a [field of the experiment](#fields).

Without a failure model, or with `null` in its place, OpenDC 2 injects no failures. Otherwise, the `type` field selects
one of four models:

| Type          | Description                                                    |
|---------------|----------------------------------------------------------------|
| `no`          | No failures. Useful to compare runs with and without failures. |
| `prefab`      | A built-in model based on measured failures.                   |
| `custom`      | Samples each failure from three distributions.                 |
| `trace-based` | Replays the failures in a failure trace.                       |

### Prefab models

A *prefab* is a built-in failure model, which the `prefabName` field selects:

``` json
{ "type": "prefab", "prefabName": "G5k06Exp" }
```

The prefabs are fitted to the traces of
[The Failure Trace Archive](https://www.sciencedirect.com/science/article/pii/S0743731513000634). The name of a prefab
combines a trace with a distribution:

| Part         | Values                                                                                              |
|--------------|-----------------------------------------------------------------------------------------------------|
| Trace        | `G5k06`, `Lanl05`, `Ldns04`, `Microsoft99`, `Nd07cpu`, `Overnet03`, `Pl05`, `Skype06`, `Websites02` |
| Distribution | `Exp` (exponential), `Wbl` (Weibull), `LogN` (log-normal), `Gam` (gamma)                            |

For example, `Lanl05Wbl` uses a Weibull distribution fitted to the LANL 2005 trace. The parameters of the prefabs did
not change in OpenDC 3. The table under [Prefab models](../reference/experiment/failure-model.md#prefab-models) of its
failure-model page lists, for each prefab, the distribution of the time between two failures and of the duration of a
failure, both in hours.

### Custom models

A custom model samples every failure from three distributions, one per field:

| Field             | Description                                       |
|-------------------|---------------------------------------------------|
| `iatSampler`      | The time between two failures, in hours.          |
| `durationSampler` | How long a failure lasts, in hours.               |
| `nohSampler`      | The fraction of hosts that fail, between 0 and 1. |

Each field holds one of the following distributions:

| Type          | Fields           |
|---------------|------------------|
| `constant`    | `value`          |
| `exponential` | `mean`           |
| `gamma`       | `shape`, `scale` |
| `log-normal`  | `scale`, `shape` |
| `normal`      | `mean`, `std`    |
| `pareto`      | `scale`, `shape` |
| `uniform`     | `lower`, `upper` |
| `weibull`     | `alpha`, `beta`  |

Custom models are random, so their results depend on the seed of the run. The following model uses a different
distribution for each field:

``` json
{
    "type": "custom",
    "iatSampler": { "type": "exponential", "mean": 24.0 },
    "durationSampler": { "type": "weibull", "alpha": 0.5, "beta": 1.2 },
    "nohSampler": { "type": "constant", "value": 0.5 }
}
```

### Trace-based models

A trace-based model replays the failures in a *failure trace*. It has the following fields:

| Field        | Type    | Default  | Description                                                                                                 |
|--------------|---------|----------|-------------------------------------------------------------------------------------------------------------|
| `pathToFile` | string  | required | The path to the failure trace.                                                                              |
| `startPoint` | number  | `0.0`    | Where in the trace to start, as a fraction between 0 and 1. The failures before it are replayed at the end. |
| `repeat`     | boolean | `true`   | Start again from the beginning when the trace ends.                                                         |

A failure trace is a Parquet file with one row per failure:

| Column              | Type   | Description                                           |
|---------------------|--------|-------------------------------------------------------|
| `failure_interval`  | int64  | The time since the previous failure, in milliseconds. |
| `failure_duration`  | int64  | How long the failure lasts, in milliseconds.          |
| `failure_intensity` | double | The fraction of hosts that fail.                      |

The following model replays a failure trace and does not start again when the trace ends:

``` json
{
    "type": "trace-based",
    "pathToFile": "failure_traces/my_failures.parquet",
    "repeat": false
}
```

## Checkpoint model

A *checkpoint model* makes tasks save their progress at regular intervals, in a *checkpoint*. When a host fails, its
tasks restart from their last checkpoint instead of from the beginning. Checkpointing is off by default.

A checkpoint model has the following fields:

| Field                       | Type    | Default                    | Description                                                                                            |
|-----------------------------|---------|----------------------------|--------------------------------------------------------------------------------------------------------|
| `checkpointInterval`        | integer | `3600000` (1&nbsp;hour)    | The time between two checkpoints, in milliseconds.                                                     |
| `checkpointDuration`        | integer | `300000` (5&nbsp;minutes)  | How long it takes to create a checkpoint, in milliseconds.                                             |
| `checkpointIntervalScaling` | number  | `1.0`                      | The factor by which `checkpointInterval` is multiplied after each checkpoint. `1.0` keeps it the same. |

The following `checkpointModels` axis has two values, so OpenDC 2 runs every combination of the other axes once without
and once with checkpointing:

``` json
"checkpointModels": [
    null,
    { "checkpointInterval": 3600000, "checkpointDuration": 300000, "checkpointIntervalScaling": 1.5 }
]
```

With this checkpoint model, the time between two checkpoints grows after each checkpoint: 1&nbsp;hour, 1.5&nbsp;hours,
2.25&nbsp;hours and so on.

## Export model

During a simulation, OpenDC 2 regularly takes a *sample* of the state of the datacenter and writes it to the
[output files](output.md). An *export model* decides which output files OpenDC 2 writes, which columns they have and how
often it takes a sample.

An export model has the following fields:

| Field                 | Type                              | Default     | Description                                                                              |
|-----------------------|-----------------------------------|-------------|------------------------------------------------------------------------------------------|
| `exportInterval`      | integer                           | `300`       | The time between two samples, in seconds.                                                |
| `printFrequency`      | integer or `null`                 | `24`        | The number of samples between two progress lines. `null` turns progress lines off.       |
| `filesToExport`       | list of strings                   | all files   | The files to write, chosen from `host`, `task`, `powerSource`, `battery` and `service`.  |
| `computeExportConfig` | [compute export config](#columns) | all columns | The columns to write in each file.                                                       |

### Columns

`computeExportConfig` has one list of column names per file, and a file without a list gets all of its columns. OpenDC 2
always writes some columns of a file, whether or not the list names them:

| Field                      | Always written                                                    |
|----------------------------|-------------------------------------------------------------------|
| `hostExportColumns`        | `timestamp`, `timestamp_absolute`, `host_name`, `cluster_name`    |
| `taskExportColumns`        | `timestamp`, `timestamp_absolute`, `task_id`, `task_name`         |
| `powerSourceExportColumns` | `timestamp`, `timestamp_absolute`, `source_name`, `cluster_name`  |
| `batteryExportColumns`     | `timestamp`, `timestamp_absolute`, `battery_name`, `cluster_name` |
| `serviceExportColumns`     | `timestamp`, `timestamp_absolute`                                 |

Column names are not case-sensitive. OpenDC 2 reports an unknown column name with an error message and ignores it. A
list without any valid column name writes all columns.

The following export model takes a sample every hour, prints progress every week of simulated time and writes only the
host, task and service files, each with a few columns:

``` json
"exportModels": [
    {
        "exportInterval": 3600,
        "printFrequency": 168,
        "filesToExport": ["host", "task", "service"],
        "computeExportConfig": {
            "hostExportColumns": ["power_draw", "energy_usage", "cpu_usage", "cpu_utilization"],
            "taskExportColumns": ["submission_time", "schedule_time", "finish_time", "task_state"],
            "serviceExportColumns": ["tasks_total", "tasks_pending", "tasks_active", "tasks_completed", "tasks_terminated", "hosts_up"]
        }
    }
]
```
