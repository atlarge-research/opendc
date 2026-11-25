---
description: How to move experiments, topologies, workloads and analysis scripts from OpenDC 2 to OpenDC 3.
---

# Migrating to OpenDC 3

OpenDC&nbsp;3 changes how experiments are written, how they are run, and what the output looks like. This page explains
how to keep running OpenDC&nbsp;2 experiments, how to convert them to the
[format of OpenDC&nbsp;3](../reference/experiment/index.md), and how to adapt scripts that analyse the output.

## What changed

| Area            | OpenDC&nbsp;2                                               | OpenDC&nbsp;3                                                                  |
|-----------------|-------------------------------------------------------------|--------------------------------------------------------------------------------|
| Running         | `OpenDCExperimentRunner --experiment-path experiment.json`  | [`opendc run experiment.json -o output`](../reference/cli.md)                  |
| Topologies      | separate files, referenced with `pathToFile`                | written in the experiment, or imported from another file with `importFrom`     |
| Topology layout | clusters, each with a power source and a battery            | datacenters, each with a power source and a battery, that contain clusters     |
| Units           | only topology values can have a unit                        | all physical values can have a [unit](../reference/units.md)                   |
| Trace files     | paths                                                       | trace references, such as `{ "type": "named", "name": "traces/my-trace" }`     |
| Output          | names and fixed properties in every output file             | identifiers in the output files, names and fixed properties in `meta/` files   |
| Measured values | energy, carbon, uptime and CPU time per sample              | energy, carbon, uptime and CPU time as totals since the start                  |

OpenDC&nbsp;3 also adds entry points that OpenDC&nbsp;2 lacks: the [Kotlin SDK](../kotlin-sdk/index.md), the
[Python SDK](../python-sdk/index.md) and a rebuilt [web application](../web/index.md).

## Running OpenDC 2 experiments on OpenDC 3

OpenDC&nbsp;3 runs experiment files in the format of OpenDC&nbsp;2 with the option `--legacy`:

``` bash
opendc --legacy run experiment.json -o output
```

`--legacy` is an option of `opendc` itself, not of `run`, so it comes before the command. OpenDC converts the
experiment and its topologies to the format of OpenDC&nbsp;3 before it simulates them. The behaviour differs from
OpenDC&nbsp;2 in the following points:

- Relative paths in the experiment are resolved against the directory from which `opendc` is started, as in
  OpenDC&nbsp;2. `opendc --input-root <dir>` resolves them against another directory.
- `outputFolder` is ignored. `-o` sets where the results are written.
- `-strict` is now `--strict`, and also comes before the command.
- Each cluster is created `count` times. OpenDC&nbsp;2 ignored `count` on clusters.
- The clusters of a topology are placed in a single datacenter, which takes the power source and the battery of the
  first cluster.
- `subsetSize` is no longer supported and is ignored.
- A policy that used the memorising scheduler, such as a `timeshift` policy without `"memorize": false`, now uses an
  `instanceCount` weigher with a multiplier of −1.0. It still places each task on a fitting host with the fewest
  tasks.
- The `Random` prefab no longer exists.

## Converting experiments

The tables of this section map each key of OpenDC&nbsp;2 to its counterpart. Plain numbers keep their meaning, with two
exceptions that the tables mark: the energy values of a battery and the export interval. Writing every value with a
unit, such as `"1 hours"`, avoids mistakes.

### Experiment

| OpenDC&nbsp;2                                 | OpenDC&nbsp;3                                                              |
|-----------------------------------------------|----------------------------------------------------------------------------|
| `"topologies": [{ "pathToFile": "t.json" }]`  | `"topologies": [{ "importFrom": "t.json" }]`, or the topology itself       |
| `outputFolder`                                | Removed. Use `opendc run -o`.                                              |
| an empty `name` gets a generated name         | An empty `name` writes the results directly into the output folder.        |
| `null` in `failureModels`                     | `{ "type": "none" }`                                                       |

The path of `importFrom` in an experiment file follows the rule of `pathToFile`: it is relative to the directory from
which `opendc` is started. [Experiment](../reference/experiment/index.md#splitting-an-experiment-over-several-files)
gives the details.

### Topology

| OpenDC&nbsp;2                                                | OpenDC&nbsp;3                                                     |
|--------------------------------------------------------------|-------------------------------------------------------------------|
| `"clusters": [...]` at the top                               | `"datacenters": [{ "clusters": [...] }]`                          |
| `powerSource` and `battery` of a cluster                     | `powerSource` and `battery` of a datacenter                       |
| `cpuDistributionPolicy`, `gpuDistributionPolicy`             | `cpuDistribution`, `gpuDistribution`                              |
| `arch` of a CPU or memory                                    | `architecture`                                                    |
| `memorySize`, `memorySpeed` of the memory                    | `size`, `speed`                                                   |
| `memorySize` of a GPU                                        | `memory`                                                          |
| `virtualizationOverHeadModel`                                | `virtualizationOverhead`                                          |
| `NONE`, `CONSTANT`, `SHARE_BASED`                            | `none`, `constant`, `shareBased`                                  |
| `modelType` of a power model                                 | `type`                                                            |
| `MAX_MIN_FAIRNESS`, `BEST_EFFORT`, `EQUAL_SHARE`, `FIRST_FIT`, `FIXED_SHARE` | `maxMinFairness`, `bestEffort`, `equalShare`, `firstFit`, `fixedShare` |
| `updateIntervalLength` of `BEST_EFFORT`                      | `updateInterval` of `bestEffort`                                  |
| `"carbonTracePath": "carbon.parquet"`                        | `"carbon": { "type": "trace", "source": { "type": "named", "name": "carbon.parquet" } }` |
| `batteryPolicy` of a battery                                 | `policy`                                                          |
| `capacity`, `initialCharge` of a battery, in kWh             | **A plain number is now in joules.** Write `"100 kWh"`.           |

### Workload

| OpenDC&nbsp;2                                             | OpenDC&nbsp;3                                                          |
|-----------------------------------------------------------|------------------------------------------------------------------------|
| `"type": "ComputeWorkload"`, `"pathToFile": "traces/t"`   | `"type": "trace"`, `"source": { "type": "named", "name": "traces/t" }` |

The other fields, such as `sampleFraction` and `scalingPolicy`, are unchanged.

### Allocation policy

| OpenDC&nbsp;2                                  | OpenDC&nbsp;3                                                                       |
|------------------------------------------------|-------------------------------------------------------------------------------------|
| `policyName` of a prefab                       | `prefabName`                                                                        |
| the filters `Compute`, `VCpu`, `VCpuCapacity`, `Ram`, `InstanceCount`, `SameHost`, `DifferentHost` | `compute`, `vcpu`, `vcpuCapacity`, `ram`, `instanceCount`, `sameHost`, `differentHost` |
| the weighers `Ram`, `CoreRam`, `InstanceCount`, `VCpuCapacity`, `VCpu` | `ram`, `coreRam`, `instanceCount`, `vcpuCapacity`, `vcpu`   |
| `"type": "timeshift"` with its settings        | `"type": "filter"`, with the settings in a `timeshift` object                       |
| `subsetSize`, `memorize`                       | Removed.                                                                            |
| the `Random` prefab                            | Removed.                                                                            |

### Failure model

| OpenDC&nbsp;2                                        | OpenDC&nbsp;3                                                            |
|------------------------------------------------------|--------------------------------------------------------------------------|
| `"type": "no"`                                       | `"type": "none"`                                                         |
| `"type": "trace-based"`, `"pathToFile": "f.parquet"` | `"type": "trace"`, `"source": { "type": "named", "name": "f.parquet" }`  |
| `iatSampler`, `durationSampler`, `nohSampler`        | `interArrival`, `duration`, `hostFraction`                               |

### Checkpoint model

| OpenDC&nbsp;2                                                           | OpenDC&nbsp;3                             |
|-------------------------------------------------------------------------|-------------------------------------------|
| `checkpointInterval`, `checkpointDuration`, `checkpointIntervalScaling` | `interval`, `duration`, `intervalScaling` |

### Export model

| OpenDC&nbsp;2                                              | OpenDC&nbsp;3                                                                |
|------------------------------------------------------------|------------------------------------------------------------------------------|
| `exportInterval`, in seconds                               | **A plain number is now in milliseconds.** Write `"1 hours"`.                |
| `"computeExportConfig": { "hostExportColumns": [...] }`    | `"columns": { "host": { "type": "only", "columns": [...] } }`                |
| `taskExportColumns`, `powerSourceExportColumns`, `batteryExportColumns`, `serviceExportColumns` | `task`, `powerSource`, `battery`, `service` |

`filesToExport` is unchanged, and can also select the new files `cluster` and `datacenter`.

### Example

The following experiment and topology are in the format of OpenDC&nbsp;2:

``` json title="experiment.json"
{
    "name": "example",
    "topologies": [{ "pathToFile": "topologies/small.json" }],
    "workloads": [{ "pathToFile": "traces/bitbrains-small", "type": "ComputeWorkload" }],
    "allocationPolicies": [{ "type": "prefab", "policyName": "Mem" }],
    "exportModels": [{ "exportInterval": 3600 }]
}
```

``` json title="topologies/small.json"
{
    "clusters": [
        {
            "name": "C01",
            "hosts": [
                {
                    "name": "H01",
                    "count": 16,
                    "cpu": { "coreCount": 16, "coreSpeed": 2100 },
                    "memory": { "memorySize": 100000 },
                    "cpuPowerModel": { "modelType": "sqrt", "idlePower": 32.0, "maxPower": 180.0 }
                }
            ],
            "powerSource": { "carbonTracePath": "carbon_traces/NL_2021-2024.parquet" }
        }
    ]
}
```

The same experiment in the format of OpenDC&nbsp;3, with the topology in the experiment file:

``` json title="experiment.json"
{
    "name": "example",
    "topologies": [
        {
            "datacenters": [
                {
                    "powerSource": {
                        "carbon": {
                            "type": "trace",
                            "source": { "type": "named", "name": "carbon_traces/NL_2021-2024.parquet" }
                        }
                    },
                    "clusters": [
                        {
                            "name": "C01",
                            "hosts": [
                                {
                                    "name": "H01",
                                    "count": 16,
                                    "cpu": { "coreCount": 16, "coreSpeed": "2.1 GHz" },
                                    "memory": { "size": "100000 MiB" },
                                    "cpuPowerModel": { "type": "sqrt", "idlePower": "32 W", "maxPower": "180 W" }
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    "workloads": [
        { "type": "trace", "source": { "type": "named", "name": "traces/bitbrains-small" } }
    ],
    "allocationPolicies": [{ "type": "prefab", "prefabName": "Mem" }],
    "exportModels": [{ "exportInterval": "1 hours" }]
}
```

## Converting traces

The workload traces, carbon traces and failure traces of OpenDC&nbsp;2 work in OpenDC&nbsp;3 without changes. Only the
faster workload type `efficient-trace` makes a further demand: the trace needs a column `num_fragments`, as
[Workload](../reference/experiment/workload.md#trace-workloads) describes.

## Updating analysis scripts

The results are written to `<output>/<name>/raw-output/<scenario>/seed=<seed>/`, as before, but `<output>` is now set
with `opendc run -o`. Next to the output files, each run has a folder `meta/`, and there are two new files:
`cluster.parquet` and `dataCenter.parquet`. [Output](../reference/output/index.md) lists all files and columns.

Some metrics are measured differently. In OpenDC&nbsp;2, `energy_usage`, `carbon_emission`, `uptime`, `downtime` and
the columns for CPU and GPU time hold the value since the previous sample. In OpenDC&nbsp;3, they hold the total since
the start of the simulation. The difference between consecutive samples of the same host, task or power source gives
the values of OpenDC&nbsp;2.

The following columns of OpenDC&nbsp;2 moved or were removed:

| Column in OpenDC&nbsp;2                                               | In OpenDC&nbsp;3                                                              |
|-----------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `timestamp_absolute` (all files)                                      | `timestamp` plus `start_time` from `meta/simulation.parquet`                  |
| `host_name`, `cluster_name` (host)                                    | `host_id`, with the names in `meta/host.parquet` and `meta/cluster.parquet`   |
| `core_count`, `mem_capacity`, `cpu_capacity`, `gpu_capacity_i` (host) | `meta/host.parquet`                                                           |
| `tasks_terminated` (host)                                             | Removed.                                                                      |
| `task_name` (task)                                                    | Removed.                                                                      |
| `host_name` (task)                                                    | `host_id`                                                                     |
| `cpu_count`, `gpu_count`, `mem_capacity`, `submission_time` (task)    | `meta/task.parquet`                                                           |
| `cpu_limit`, `gpu_limit`, `uptime`, `downtime` (task)                 | Removed.                                                                      |
| `source_name`, `cluster_name` (power source)                          | `power_source_id`, with the name and the datacenter in `meta/powerSource.parquet` |
| `battery_name`, `cluster_name`, `capacity` (battery)                  | `battery_id`, with the name, the datacenter and the capacity in `meta/battery.parquet` |

For example, the following Python script rebuilds the host file of OpenDC&nbsp;2 from the output of OpenDC&nbsp;3:

``` python
import pandas as pd

run = "output/example/raw-output/0/seed=0"

host = pd.read_parquet(f"{run}/host.parquet")
host_meta = pd.read_parquet(f"{run}/meta/host.parquet")
cluster_meta = pd.read_parquet(f"{run}/meta/cluster.parquet")
start_time = pd.read_parquet(f"{run}/meta/simulation.parquet")["start_time"].iloc[0]

host = host.merge(host_meta, on="host_id").merge(cluster_meta[["cluster_id", "cluster_name"]], on="cluster_id")
host["timestamp_absolute"] = host["timestamp"] + start_time

# Totals since the start of the simulation, to values since the previous sample
for column in ["energy_usage", "uptime", "downtime", "cpu_time_active", "cpu_time_idle"]:
    host[column] = host.groupby("host_id")[column].diff().fillna(host[column])
```
