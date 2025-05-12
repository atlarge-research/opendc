The topology of a datacenter defines all available hardware. Topologies are defined using a JSON file. 
A topology consist of one or more clusters. Each cluster consist of at least one host on which jobs can be executed. 
Each host consist of one or more CPUs, a memory unit and a power model.

:::info Code
The code related to reading and processing topology files can be found [here](https://github.com/atlarge-research/opendc/tree/master/opendc-compute/opendc-compute-topology/src/main/kotlin/org/opendc/compute/topology)
:::

In the following section, we describe the different components of a topology file.

### Cluster

| variable    | type                                                          | required? | default | description                                                                       |
|-------------|---------------------------------------------------------------|-----------|---------|-----------------------------------------------------------------------------------|
| name        | string                                                        | no        | Cluster | The name of the cluster. This is only important for debugging and post-processing |
| count       | integer                                                       | no        | 1       | The amount of clusters of this type are in the data center                        |
| hosts       | List[[Host](/docs/documentation/Input/Topology/Host)]         | yes       | N/A     | A list of the hosts in a cluster.                                                 |
| powerSource | [PowerSource](/docs/documentation/Input/Topology/PowerSource) | no        | N/A     | The power source used by all hosts connected to this cluster.                     |
| battery     | [Battery](/docs/documentation/Input/Topology/Battery)         | no        | null    | The battery used by a cluster to store energy. When null, no batteries are used.  |

Hosts, power sources and batteries all require objects to use. See their respective pages for more information.

## Examples

In the following section, we discuss several examples of topology files.

### Simple

The simplest data center that can be provided to OpenDC is shown below:

```json
{
    "clusters": [
        {
            "hosts": [
                {
                    "cpu":
                    {
                        "coreCount": 16,
                        "coreSpeed": 1000
                    },
                    "memory": {
                        "memorySize": 100000
                    }
                }
            ],
            "powerSource": {
                "carbonTracePath": "carbon_traces/AT_2021-2024.parquet"
            }
        }
    ]
}
```

This creates a data center with a single cluster containing a single host. This host consist of a single 16 core CPU
with a speed of 1 Ghz, and 100 MiB RAM memory.

### Count

Duplicating clusters, hosts, or CPUs is easy using the "count" keyword:

```json
{
    "clusters": [
        {
            "count": 2,
            "hosts": [
                {
                    "count": 5,
                    "cpu":
                    {
                        "coreCount": 16,
                        "coreSpeed": 1000,
                        "count": 10
                    },
                    "memory": 
                    {
                        "memorySize": 100000
                    }
                }
            ],
            "powerSource": {
                "carbonTracePath": "carbon_traces/AT_2021-2024.parquet"
            }
        }
    ]
}
```

This topology creates a datacenter consisting of 2 clusters, both containing 5 hosts. Each host contains 10 16 core
CPUs.
Using "count" saves a lot of copying.

### Complex

Following is an example of a more complex topology:

```json
{
    "clusters": [
        {
            "name": "C01",
            "count": 2,
            "hosts": [
                {
                    "name": "H01",
                    "count": 2,
                    "cpus": [
                        {
                            "coreCount": 16,
                            "coreSpeed": 1000
                        }
                    ],
                    "memory": {
                        "memorySize": 1000000
                    },
                    "powerModel": {
                        "modelType": "linear",
                        "idlePower": 200.0,
                        "maxPower": 400.0
                    }
                },
                {
                    "name": "H02",
                    "count": 2,
                    "cpus": [
                        {
                            "coreCount": 8,
                            "coreSpeed": 3000
                        }
                    ],
                    "memory": {
                        "memorySize": 100000
                    },
                    "powerModel": {
                        "modelType": "square",
                        "idlePower": 300.0,
                        "maxPower": 500.0
                    }
                }
            ]
        }
    ]
}
```

This topology defines two types of hosts with different coreCount, and coreSpeed.
Both types of hosts are created twice. 


### With Units of Measure

Aside from using number to indicate values it is also possible to define values using strings. This allows the user to define the unit of the input parameter.
```json
{
    "clusters": [
        {
            "count": 2,
            "hosts" :
            [
                {
                    "name": "H01",
                    "cpuModel":
                    {
                        "coreCount": 8,
                        "coreSpeed": "3.2 Ghz"
                    },
                    "memory": {
                        "memorySize": "128e3 MiB",
                        "memorySpeed": "1 Mhz"
                    },
                    "powerModel": {
                        "modelType": "linear",
                        "power": "400 Watts",
                        "maxPower": "1 KW",
                        "idlePower": "0.4 W"
                    }
                }
            ]
        }
    ]
}
```
