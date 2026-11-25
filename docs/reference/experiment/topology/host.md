---
description: The hosts on which tasks run, with their CPU, memory and GPU.
---

# Host

A *host* is a machine on which tasks run. It has a CPU and memory, and can have a GPU. This page lists the fields of a
host and of its three components.

## Fields

| Field             | Type                                           | Default                                 | Description                                |
|-------------------|------------------------------------------------|-----------------------------------------|--------------------------------------------|
| `name`            | string                                         | `"Host"`                                | The name of the host.                      |
| `count`           | integer                                        | `1`                                     | The number of identical hosts to create.   |
| `cpu`             | [CPU](#cpu)                                    | required                                | The CPU of the host.                       |
| `memory`          | [memory](#memory)                              | required                                | The memory of the host.                    |
| `gpu`             | [GPU](#gpu)                                    | no GPU                                  | The GPU of the host.                       |
| `cpuPowerModel`   | [power model](power-model.md)                  | linear, 200&nbsp;W idle, 400&nbsp;W maximum | The power draw of the CPU.             |
| `gpuPowerModel`   | [power model](power-model.md)                  | linear, 200&nbsp;W idle, 400&nbsp;W maximum | The power draw of the GPU.             |
| `cpuDistribution` | [distribution policy](distribution-policy.md)  | `maxMinFairness`                        | How tasks share the CPU.                   |
| `gpuDistribution` | [distribution policy](distribution-policy.md)  | `maxMinFairness`                        | How tasks share the GPU.                   |

## CPU

| Field          | Type                          | Default     | Description                           |
|----------------|-------------------------------|-------------|---------------------------------------|
| `coreCount`    | integer                       | required    | The number of cores per CPU.          |
| `coreSpeed`    | [frequency](../../units.md)   | required    | The speed of each core.               |
| `count`        | integer                       | `1`         | The number of CPUs in the host.       |
| `vendor`       | string                        | `"unknown"` | The vendor of the CPU.                |
| `modelName`    | string                        | `"unknown"` | The model of the CPU.                 |
| `architecture` | string                        | `"unknown"` | The micro-architecture of the CPU.    |

The *CPU capacity* of a host is the product of `count`, `coreCount` and `coreSpeed`. A host with 16&nbsp;cores of
2.1&nbsp;GHz has a capacity of 33.6&nbsp;GHz, which its tasks share.

## Memory

| Field          | Type                          | Default     | Description                    |
|----------------|-------------------------------|-------------|--------------------------------|
| `size`         | [data size](../../units.md)   | required    | The amount of memory.          |
| `speed`        | [frequency](../../units.md)   | unspecified | The speed of the memory.       |
| `vendor`       | string                        | `"unknown"` | The vendor of the memory.      |
| `modelName`    | string                        | `"unknown"` | The model of the memory.       |
| `architecture` | string                        | `"unknown"` | The memory technology.         |

## GPU

| Field                    | Type                                                    | Default     | Description                                      |
|--------------------------|---------------------------------------------------------|-------------|--------------------------------------------------|
| `coreCount`              | integer                                                 | required    | The number of cores per GPU.                     |
| `coreSpeed`              | [frequency](../../units.md)                             | required    | The speed of each core.                          |
| `count`                  | integer                                                 | `1`         | The number of GPUs in the host.                  |
| `memory`                 | [data size](../../units.md)                             | unspecified | The memory of each GPU.                          |
| `memoryBandwidth`        | [data rate](../../units.md)                             | unspecified | The memory bandwidth of each GPU.                |
| `vendor`                 | string                                                  | `"unknown"` | The vendor of the GPU.                           |
| `modelName`              | string                                                  | `"unknown"` | The model of the GPU.                            |
| `architecture`           | string                                                  | `"unknown"` | The micro-architecture of the GPU.               |
| `virtualizationOverhead` | [virtualisation overhead](virtualization-overhead.md)   | `none`      | The performance that the GPU loses when shared.  |

## Example

The following host has two CPUs with 24&nbsp;cores each, and two GPUs:

``` json
{
    "name": "gpu-host",
    "count": 8,
    "cpu": { "coreCount": 24, "coreSpeed": "2.8 GHz", "count": 2, "vendor": "Intel" },
    "memory": { "size": "192 GiB" },
    "cpuPowerModel": { "type": "linear", "idlePower": "100 W", "maxPower": "350 W" },
    "gpu": {
        "coreCount": 5120,
        "coreSpeed": "1.5 GHz",
        "count": 2,
        "memory": "32 GiB",
        "memoryBandwidth": "900 GBps",
        "virtualizationOverhead": { "type": "constant", "percentageOverhead": 0.1 }
    },
    "gpuPowerModel": { "type": "linear", "idlePower": "50 W", "maxPower": "300 W" },
    "gpuDistribution": { "type": "fixedShare", "shareRatio": 0.5 }
}
```
