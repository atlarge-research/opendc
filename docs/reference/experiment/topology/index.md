---
description: The topology, which defines the datacenters, clusters and hosts that OpenDC simulates.
---

# Topology

A *topology* defines the hardware that OpenDC simulates. This page describes the three levels of a topology: the
topology itself, its datacenters and their clusters. The pages on [hosts](host.md), [power sources](power-source.md)
and [batteries](battery.md) describe the parts below them.

## Structure

A topology consists of one or more datacenters. Each datacenter has a power source, optionally a battery, and one or
more clusters of hosts. Figure 1 shows the structure with the name of the field that holds each part.

``` mermaid
flowchart TB
    topology["topology"] -->|"datacenters"| dc["datacenter"]
    dc -->|"powerSource"| ps["power source"]
    dc -->|"battery"| battery["battery"]
    dc -->|"clusters"| cluster["cluster"]
    cluster -->|"hosts"| host["host"]
    host --> parts["cpu, memory, gpu<br>cpuPowerModel, gpuPowerModel<br>cpuDistribution, gpuDistribution"]
```

/// figure-caption
The structure of a topology.
///

A topology is written directly in the experiment, or in a separate file that the experiment imports with
[`importFrom`](../index.md#splitting-an-experiment-over-several-files).

## Topology

| Field         | Type                               | Default  | Description                      |
|---------------|------------------------------------|----------|----------------------------------|
| `datacenters` | list of [datacenters](#datacenter) | required | The datacenters of the topology. |

## Datacenter

| Field         | Type                             | Default                          | Description                                                |
|---------------|----------------------------------|----------------------------------|------------------------------------------------------------|
| `name`        | string                           | `"DC"`                           | The name of the datacenter.                                |
| `clusters`    | list of [clusters](#cluster)     | required                         | The clusters of the datacenter.                            |
| `powerSource` | [power source](power-source.md)  | unlimited power, no carbon model | The power source of the datacenter, with its carbon model. |
| `battery`     | [battery](battery.md)            | no battery                       | A battery that stores energy for the datacenter.           |

## Cluster

| Field   | Type                     | Default     | Description                                  |
|---------|--------------------------|-------------|----------------------------------------------|
| `name`  | string                   | `"Cluster"` | The name of the cluster.                     |
| `count` | integer                  | `1`         | The number of identical clusters to create.  |
| `hosts` | list of [hosts](host.md) | required    | The hosts of the cluster.                    |

!!! note "Names"

    The names of datacenters, clusters and hosts appear in the [meta files](../../output/meta.md) of the output, which
    makes results easier to read. When several entities have the same name, for example the copies that `count`
    creates, OpenDC appends a number: `H01`, `H01-0`, `H01-1`.

!!! note "Older topology files"

    OpenDC still reads topologies that list `clusters` at the top level, with a power source and a battery per
    cluster. It places the clusters in a single datacenter and logs a warning. The datacenter takes the power source
    and the battery of the first cluster, and those of the other clusters are ignored.

## Example

The following topology has one datacenter with two clusters. The first cluster has 16&nbsp;identical hosts, the second
has 4&nbsp;hosts with a GPU.

``` json
{
    "datacenters": [
        {
            "name": "DC1",
            "powerSource": {
                "carbon": {
                    "type": "trace",
                    "source": { "type": "named", "name": "carbon_traces/NL_2021-2024.parquet" }
                }
            },
            "clusters": [
                {
                    "name": "cpu-cluster",
                    "hosts": [
                        {
                            "name": "cpu-host",
                            "count": 16,
                            "cpu": { "coreCount": 32, "coreSpeed": "2.1 GHz" },
                            "memory": { "size": "256 GiB" },
                            "cpuPowerModel": { "type": "linear", "idlePower": "100 W", "maxPower": "350 W" }
                        }
                    ]
                },
                {
                    "name": "gpu-cluster",
                    "hosts": [
                        {
                            "name": "gpu-host",
                            "count": 4,
                            "cpu": { "coreCount": 16, "coreSpeed": "2.4 GHz" },
                            "memory": { "size": "128 GiB" },
                            "gpu": { "coreCount": 5120, "coreSpeed": "1.5 GHz", "memory": "32 GiB" }
                        }
                    ]
                }
            ]
        }
    ]
}
```
