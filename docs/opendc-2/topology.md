---
description: The topology file of OpenDC 2, which defines the clusters and hosts that are simulated.
status: deprecated
search:
  exclude: true
---

# Topology

A *topology* defines the hardware that OpenDC 2 simulates. It is a JSON file that an [experiment](experiment.md#fields)
refers to with `pathToFile`. This page describes the structure of a topology and each of its parts: hosts, power models,
distribution policies, virtualisation overhead models, power sources and batteries.

## Structure

A topology consists of one or more *clusters*. Each cluster has hosts, a power source and an optional battery. The
following tree shows how the fields of a topology nest:

``` text
topology
+-- clusters
    +-- powerSource
    +-- battery
    +-- hosts
        +-- cpu, memory, gpu
        +-- cpuPowerModel, gpuPowerModel
        +-- cpuDistributionPolicy, gpuDistributionPolicy
```

The topology itself has one field:

| Field      | Type                         | Default  | Description                   |
|------------|------------------------------|----------|-------------------------------|
| `clusters` | list of [clusters](#cluster) | required | The clusters in the topology. |

### Cluster

A cluster has the following fields:

| Field         | Type                           | Default                          | Description                                   |
|---------------|--------------------------------|----------------------------------|-----------------------------------------------|
| `name`        | string                         | `"Cluster"`                      | Name of the cluster.                          |
| `count`       | integer                        | `1`                              | The number of identical clusters to create.   |
| `hosts`       | list of [hosts](#host)         | required                         | The hosts in the cluster.                     |
| `powerSource` | [power source](#power-source)  | unlimited power, no carbon trace | The power source of the cluster.              |
| `battery`     | [battery](#battery)            | no battery                       | A battery that stores energy for the cluster. |

!!! note "`count` of a cluster has no effect in OpenDC 2"

    OpenDC 2 reads the `count` of a cluster, but creates every cluster once. OpenDC 3 creates a cluster `count` times,
    also when it runs a file of OpenDC 2 with `--legacy`. The `count` of a host, a CPU and a GPU works in both versions.

### Example topology

The following topology describes two identical clusters of 16 hosts, which use the carbon trace of the Netherlands:

``` json
{
    "clusters": [
        {
            "name": "C01",
            "count": 2,
            "hosts": [
                {
                    "name": "H01",
                    "count": 16,
                    "cpu": { "coreCount": 32, "coreSpeed": "2.1 GHz" },
                    "memory": { "memorySize": "256 GiB" },
                    "cpuPowerModel": { "modelType": "linear", "idlePower": "100 W", "maxPower": "350 W" }
                }
            ],
            "powerSource": {
                "carbonTracePath": "carbon_traces/NL_2021-2024.parquet"
            }
        }
    ]
}
```

## Host

A *host* is a machine on which [tasks](workload.md) run. It has a CPU and memory, and can have a GPU. A host has the
following fields:

| Field                   | Type                                        | Default                                   | Description                              |
|-------------------------|---------------------------------------------|-------------------------------------------|------------------------------------------|
| `name`                  | string                                      | `"Host"`                                  | Name of the host.                        |
| `count`                 | integer                                     | `1`                                       | The number of identical hosts to create. |
| `cpu`                   | [CPU](#cpu)                                 | required                                  | The CPU of the host.                     |
| `memory`                | [memory](#memory)                           | required                                  | The memory of the host.                  |
| `gpu`                   | [GPU](#gpu)                                 | no GPU                                    | The GPU of the host.                     |
| `cpuPowerModel`         | [power model](#power-model)                 | linear, 200&nbsp;W idle, 400&nbsp;W max   | Power draw of the CPU.                   |
| `gpuPowerModel`         | [power model](#power-model)                 | linear, 200&nbsp;W idle, 400&nbsp;W max   | Power draw of the GPU.                   |
| `cpuDistributionPolicy` | [distribution policy](#distribution-policy) | `MAX_MIN_FAIRNESS`                        | How the CPU is shared between tasks.     |
| `gpuDistributionPolicy` | [distribution policy](#distribution-policy) | `MAX_MIN_FAIRNESS`                        | How the GPU is shared between tasks.     |

### CPU

The `cpu` field of a host has the following fields:

| Field       | Type                                         | Default     | Description                        |
|-------------|----------------------------------------------|-------------|------------------------------------|
| `coreCount` | integer                                      | required    | The number of cores per CPU.       |
| `coreSpeed` | [frequency](units.md#values-with-a-unit)     | required    | The speed of each core.            |
| `count`     | integer                                      | `1`         | The number of CPUs in the host.    |
| `vendor`    | string                                       | `"unknown"` | The vendor of the CPU.             |
| `modelName` | string                                       | `"unknown"` | The model of the CPU.              |
| `arch`      | string                                       | `"unknown"` | The micro-architecture of the CPU. |

### Memory

The `memory` field of a host has the following fields:

| Field         | Type                                     | Default     | Description               |
|---------------|------------------------------------------|-------------|---------------------------|
| `memorySize`  | [data size](units.md#values-with-a-unit) | required    | The amount of memory.     |
| `memorySpeed` | [frequency](units.md#values-with-a-unit) | unspecified | The speed of the memory.  |
| `vendor`      | string                                   | `"unknown"` | The vendor of the memory. |
| `modelName`   | string                                   | `"unknown"` | The model of the memory.  |
| `arch`        | string                                   | `"unknown"` | The memory technology.    |

### GPU

The `gpu` field of a host has the following fields:

| Field                         | Type                                                 | Default     | Description                                  |
|-------------------------------|------------------------------------------------------|-------------|----------------------------------------------|
| `coreCount`                   | integer                                              | required    | The number of cores per GPU.                 |
| `coreSpeed`                   | [frequency](units.md#values-with-a-unit)             | required    | The speed of each core.                      |
| `count`                       | integer                                              | `1`         | The number of GPUs in the host.              |
| `memorySize`                  | [data size](units.md#values-with-a-unit)             | unspecified | The memory of each GPU.                      |
| `memoryBandwidth`             | [data rate](units.md#values-with-a-unit)             | unspecified | The memory bandwidth of each GPU.            |
| `vendor`                      | string                                               | `"unknown"` | The vendor of the GPU.                       |
| `modelName`                   | string                                               | `"unknown"` | The model of the GPU.                        |
| `architecture`                | string                                               | `"unknown"` | The micro-architecture of the GPU.           |
| `virtualizationOverHeadModel` | [virtualisation overhead](#virtualisation-overhead)  | `NONE`      | The performance lost when the GPU is shared. |

### Example host

The following host has a CPU with four cores and a GPU, and shares the GPU in fixed shares:

``` json
{
    "name": "DualGpuHost",
    "cpu": { "coreCount": 4, "coreSpeed": "2 GHz" },
    "memory": { "memorySize": "128 GiB" },
    "cpuPowerModel": { "modelType": "linear", "idlePower": "100 W", "maxPower": "200 W" },
    "cpuDistributionPolicy": { "type": "MAX_MIN_FAIRNESS" },
    "gpu": {
        "coreCount": 2,
        "coreSpeed": "2 GHz",
        "virtualizationOverHeadModel": { "type": "CONSTANT", "percentageOverhead": 0.25 }
    },
    "gpuPowerModel": { "modelType": "linear", "idlePower": "100 W", "maxPower": "200 W" },
    "gpuDistributionPolicy": { "type": "FIXED_SHARE", "shareRatio": 0.5 }
}
```

## Power model

A *power model* determines the power draw of a CPU or GPU from its utilisation. Every host has one for its CPU
(`cpuPowerModel`) and one for its GPU (`gpuPowerModel`). A host without a power model uses a linear model with an idle
power of 200&nbsp;W and a maximum power of 400&nbsp;W.

A power model has the following fields:

| Field               | Type                                 | Default  | Description                                                                  |
|---------------------|--------------------------------------|----------|------------------------------------------------------------------------------|
| `modelType`         | string                               | required | The model, which is one of the model types in the next table.                |
| `idlePower`         | [power](units.md#values-with-a-unit) | required | The power draw at 0% utilisation.                                            |
| `maxPower`          | [power](units.md#values-with-a-unit) | required | The power draw at 100% utilisation. It must be at least `idlePower`.         |
| `power`             | [power](units.md#values-with-a-unit) | `400 W`  | The power draw of the `constant` model.                                      |
| `calibrationFactor` | number                               | `1.0`    | The calibration factor of the `mse` model.                                   |
| `asymUtil`          | number                               | `0.0`    | The utilisation above which the `asymptotic` model grows close to linearly.  |
| `dvfs`              | boolean                              | `true`   | Whether the `asymptotic` model models dynamic voltage and frequency scaling. |

The `modelType` field selects one of seven models:

| Model type   | Power draw                                                                         |
|--------------|------------------------------------------------------------------------------------|
| `constant`   | Always `power`, independent of the utilisation.                                    |
| `linear`     | Grows linearly from `idlePower` at 0% utilisation to `maxPower` at 100%.           |
| `square`     | Grows with the square of the utilisation.                                          |
| `cubic`      | Grows with the cube of the utilisation.                                            |
| `sqrt`       | Grows with the square root of the utilisation.                                     |
| `mse`        | Fitted to measurements, after Fan et al., ISCA 2007.                               |
| `asymptotic` | Grows close to linearly above the utilisation `asymUtil`, adapted from GreenCloud. |

The following host uses a `sqrt` model for its CPU:

``` json
"cpuPowerModel": {
    "modelType": "sqrt",
    "idlePower": "32 W",
    "maxPower": "180 W"
}
```

## Distribution policy

A *distribution policy* determines how the tasks on a host share the capacity of a CPU or GPU. Every host has one for
its CPU (`cpuDistributionPolicy`) and one for its GPU (`gpuDistributionPolicy`). The default is `MAX_MIN_FAIRNESS`.

The `type` field selects the policy:

| Type               | Fields                                                | Behaviour                                                                                                     |
|--------------------|-------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `MAX_MIN_FAIRNESS` |                                                       | Satisfies the smallest demands first, so that no task is starved. This is the default.                        |
| `BEST_EFFORT`      | `updateIntervalLength` (milliseconds, default `1000`) | Serves the tasks in round-robin order, rotating every `updateIntervalLength`, while using as much of the capacity as possible. Inspired by the vGPU scheduler of NVIDIA. |
| `EQUAL_SHARE`      |                                                       | Gives every task the same share of the capacity, regardless of its demand. Inspired by the vGPU scheduler of NVIDIA. |
| `FIRST_FIT`        |                                                       | Gives each task its full demand, in order, until the capacity is used up.                                     |
| `FIXED_SHARE`      | `shareRatio` (default `1.0`)                          | Gives each active task a fixed fraction of the capacity. Unused shares are not given to other tasks. Inspired by the vGPU scheduler of NVIDIA. |

The following host uses the `BEST_EFFORT` policy for its GPU, with an interval of 1&nbsp;minute:

``` json
"gpuDistributionPolicy": {
    "type": "BEST_EFFORT",
    "updateIntervalLength": 60000
}
```

## Virtualisation overhead

A *virtualisation overhead model* determines how much performance a GPU loses when virtual machines share it. The
overhead lowers the capacity that the GPU can supply. The `virtualizationOverHeadModel` field of a [GPU](#gpu) sets the
model, and the default is `NONE`.

The `type` field selects the model:

| Type          | Fields               | Behaviour                                                                                                                                                                                                |
|---------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `NONE`        |                      | No overhead, as with GPU pass-through. This is the default.                                                                                                                                              |
| `CONSTANT`    | `percentageOverhead` | A fixed overhead, as a fraction of the capacity. When `percentageOverhead` is omitted, the overhead is 0.05 (5%).                                                                                        |
| `SHARE_BASED` |                      | An overhead that depends on the number of virtual machines sharing the GPU, based on Garg et al., [Empirical Analysis of Hardware-Assisted GPU Virtualization](https://doi.org/10.1109/HiPC.2019.00054). |

The following model sets a fixed overhead of 25% of the capacity:

``` json
"virtualizationOverHeadModel": {
    "type": "CONSTANT",
    "percentageOverhead": 0.25
}
```

## Power source

Each [cluster](#cluster) has a *power source* that powers its hosts. A power source can name a
[carbon trace](#carbon-traces), which OpenDC 2 uses to compute the carbon emissions of the cluster. A power source has
the following fields:

| Field             | Type    | Default         | Description                                                                   |
|-------------------|---------|-----------------|-------------------------------------------------------------------------------|
| `name`            | string  | `"PowerSource"` | Name of the power source.                                                     |
| `maxPower`        | integer | unlimited       | The maximum power the source can supply, in watts.                            |
| `carbonTracePath` | string  | no carbon trace | The path to a carbon trace with the carbon intensity of the energy over time. |

### Carbon traces

A *carbon trace* is a Parquet file with the carbon intensity of the energy over time:

| Column             | Type                     | Description                                       |
|--------------------|--------------------------|---------------------------------------------------|
| `timestamp`        | timestamp (milliseconds) | The time from which the carbon intensity applies. |
| `carbon_intensity` | double                   | The carbon intensity, in gCO2/kWh.                |

The following power source reads its carbon intensity from a carbon trace:

``` json
"powerSource": {
    "carbonTracePath": "carbon_traces/AT_2021-2024.parquet"
}
```

## Battery

A [cluster](#cluster) can have a *battery*, which stores energy from the power source and supplies it to the hosts
later. The policy of the battery decides when it charges and discharges, based on the carbon intensity of the
[power source](#power-source). A battery has the following fields:

| Field              | Type                         | Default     | Description                                              |
|--------------------|------------------------------|-------------|----------------------------------------------------------|
| `name`             | string                       | `"Battery"` | Name of the battery.                                     |
| `capacity`         | number                       | required    | The amount of energy the battery can store, in kWh.      |
| `chargingSpeed`    | number                       | required    | The speed at which the battery charges, in watts.        |
| `initialCharge`    | number                       | `0`         | The charge at the start of the simulation, in kWh.       |
| `batteryPolicy`    | [policy](#battery-policies)  | required    | When the battery charges and discharges.                 |
| `embodiedCarbon`   | number                       | `0`         | The carbon emitted to manufacture the battery, in grams. |
| `expectedLifetime` | number                       | `0`         | The expected lifetime of the battery, in years.          |

### Battery policies

In each battery policy, the battery charges until it is full when the carbon intensity is low, and discharges until it
is empty when the carbon intensity is high. The `type` field selects the policy:

| Type              | Fields                             | Behaviour                                                                                                                              |
|-------------------|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `single`          | `carbonThreshold`                  | The carbon intensity is low when it is below `carbonThreshold`.                                                                        |
| `double`          | `lowerThreshold`, `upperThreshold` | The carbon intensity is low when it is below `lowerThreshold`, and high when it is above `upperThreshold`. In between, the battery is idle. |
| `runningMean`     | `startingThreshold`, `windowSize`  | The carbon intensity is low when it is below the mean of the last `windowSize` carbon intensities.                                     |
| `runningMeanPlus` | `startingThreshold`, `windowSize`  | As `runningMean`, but the battery only starts charging once the carbon intensity stops decreasing.                                     |

The following battery uses the `runningMeanPlus` policy:

``` json
"battery": {
    "capacity": 100.0,
    "chargingSpeed": 5000.0,
    "batteryPolicy": { "type": "runningMeanPlus", "startingThreshold": 123.2, "windowSize": 168 }
}
```
