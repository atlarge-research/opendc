The topology of a datacenter defines all available hardware. Topologies are defined using a JSON file. 
A topology consist of one or more clusters. Each cluster consist of at least one host on which jobs can be executed. 
Each host consist of one or more CPUs, a memory unit and a power model.

:::info Code
The code related to reading and processing topology files can be found [here](https://github.com/atlarge-research/opendc/tree/master/opendc-compute/opendc-compute-topology/src/main/kotlin/org/opendc/compute/topology)
:::

## Schema

The schema for the topology file is provided in [schema](TopologySchema.md).
In the following section, we describe the different components of the schema.

### Cluster

| variable    | type                         | required? | default | description                                                                       |
|-------------|------------------------------|-----------|---------|-----------------------------------------------------------------------------------|
| name        | string                       | no        | Cluster | The name of the cluster. This is only important for debugging and post-processing |
| count       | integer                      | no        | 1       | The amount of clusters of this type are in the data center                        |
| hosts       | List[[Host](#host)]          | yes       | N/A     | A list of the hosts in a cluster.                                                 |
| powerSource | [PowerSource](#power-source) | no        | N/A     | The power source used by all hosts connected to this cluster.                     |
| battery     | [Battery](#battery)          | no        | null    | The battery used by a cluster to store energy.                                    |

### Power Source
Each cluster has a power source that provides power to the hosts in the cluster.
A user can connect a power source to a carbon trace to determine the carbon emissions during a workload. 

The power source consist of the following components:

| variable        | type                         | required? | default        | description                                                                       |
|-----------------|------------------------------|-----------|----------------|-----------------------------------------------------------------------------------|
| name            | string                       | no        | PowerSource    | The name of the cluster. This is only important for debugging and post-processing |
| totalPower      | integer                      | no        | Long.Max_Value | The total power that the power source can provide in Watt.                        |
| carbonTracePath | path/to/file                 | no        | null           | A list of the hosts in a cluster.                                                 |

### Battery
A battery can be connected to a cluster to store energy based on a policy. In previous research we have used this to 
store energy from the grid when the carbon intensity is low, and use this energy when the carbon intensity is high.
The specific input needed for Batteries are defined in [Battery](Battery.md).

### Host
A host is a machine that can execute tasks. A host consist of the following components:

| variable    | type                        | required? | default | description                                                                    |
|-------------|-----------------------------|-----------|---------|--------------------------------------------------------------------------------|
| name        | string                      | no        | Host    | The name of the host. This is only important for debugging and post-processing |
| count       | integer                     | no        | 1       | The amount of hosts of this type are in the cluster                            |
| cpuModel    | [CPU](#cpu)                 | yes       | N/A     | The CPUs in the host                                                           |
| memory      | [Memory](#memory)           | yes       | N/A     | The memory used by the host                                                    |
| power model | [Power Model](#power-model) | yes       | N/A     | The power model used to determine the power draw of the host                   |

### CPU

| variable  | type    | Unit  | required? | default | description                                      |
|-----------|---------|-------|-----------|---------|--------------------------------------------------|
| name      | string  | N/A   | no        | unknown | The name of the CPU.                             |
| vendor    | string  | N/A   | no        | unknown | The vendor of the CPU                            |
| arch      | string  | N/A   | no        | unknown | the micro-architecture of the CPU                |
| count     | integer | N/A   | no        | 1       | The amount of cpus of this type used by the host |
| coreCount | integer | count | yes       | N/A     | The number of cores in the CPU                   |
| coreSpeed | Double  | Mhz   | yes       | N/A     | The speed of each core in Mhz                    |

### Memory

| variable    | type    | Unit | required? | default | description                                                              |
|-------------|---------|------|-----------|---------|--------------------------------------------------------------------------|
| name        | string  | N/A  | no        | unknown | The name of the CPU.                                                     |
| vendor      | string  | N/A  | no        | unknown | The vendor of the CPU                                                    |
| arch        | string  | N/A  | no        | unknown | the micro-architecture of the CPU                                        |
| count       | integer | N/A  | no        | 1       | The amount of cpus of this type used by the host                         |
| memorySize  | integer | Byte | yes       | N/A     | The number of cores in the CPU                                           |
| memorySpeed | Double  | ?    | no        | -1      | The speed of each core in Mhz. PLACEHOLDER: this currently does nothing. |

### Power Model
To calculate the power draw of a host, a power model is used. The power model can be defined using the following parameters:

| variable        | type   | Unit | required? | default  | description                                                                   |
|-----------------|--------|------|-----------|----------|-------------------------------------------------------------------------------|
| vendor          | string | N/A  | yes       | N/A      | The type of model used to determine power draw                                |
| modelName       | string | N/A  | yes       | N/A      | The type of model used to determine power draw                                |
| arch            | string | N/A  | yes       | N/A      | The type of model used to determine power draw                                |
| totalPower      | Int64  | Watt | no        | max long | The power draw of a host when using max capacity in Watt                      |
| carbonTracePath | string | N/A  | no        | null     | Path to a carbon intensity trace. If not given, carbon intensity is always 0. |

## Examples

In the following section, we discuss several examples of topology files. Any topology file can be verified using the
JSON schema defined in [schema](TopologySchema.md).

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
            ]
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
            ]
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
