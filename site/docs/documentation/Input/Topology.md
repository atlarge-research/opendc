The topology of a datacenter is defined using a JSON file. A topology consist of one or more clusters.
Each cluster consist of at least one host on which jobs can be executed. Each host consist of one or more CPUs,
a memory unit and a power model.

## Schema

The schema for the topology file is provided in [schema](TopologySchema).
In the following section, we describe the different components of the schema.

### Cluster

| variable | type                | required? | default | description                                                                       |
|----------|---------------------|-----------|---------|-----------------------------------------------------------------------------------|
| name     | string              | no        | Cluster | The name of the cluster. This is only important for debugging and post-processing |
| count    | integer             | no        | 1       | The amount of clusters of this type are in the data center                        |
| hosts    | List[[Host](#host)] | yes       | N/A     | A list of the hosts in a cluster.                                                 |

### Host

| variable    | type                        | required? | default | description                                                                    |
|-------------|-----------------------------|-----------|---------|--------------------------------------------------------------------------------|
| name        | string                      | no        | Host    | The name of the host. This is only important for debugging and post-processing |
| count       | integer                     | no        | 1       | The amount of hosts of this type are in the cluster                            |
| cpus        | List[[CPU](#cpu)]           | yes       | N/A     | A list of the hosts in a cluster.                                              |
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

| variable  | type    | Unit | required? | default | description                                                                |
|-----------|---------|------|-----------|---------|----------------------------------------------------------------------------|
| modelType | string  | N/A  | yes       | N/A     | The type of model used to determine power draw                             |
| power     | string  | Watt | no        | 400     | The constant power draw when using the 'constant' power model type in Watt |
| maxPower  | string  | Watt | yes       | N/A     | The power draw of a host when using max capacity in Watt                   |
| idlePower | integer | Watt | yes       | N/A     | The power draw of a host when idle in Watt                                 |

## Examples

In the following section, we discuss several examples of topology files. Any topology file can be verified using the
JSON schema defined in [schema](TopologySchema).

### Simple

The simplest data center that can be provided to OpenDC is shown below:

```json
{
    "clusters": [
        {
            "hosts": [
                {
                    "cpus": [
                        {
                            "coreCount": 16,
                            "coreSpeed": 1000
                        }
                    ],
                    "memory": {
                        "memorySize": 100000
                    }
                }
            ]
        }
    ]
}
```

This is creates a data center with a single cluster containing a single host. This host consist of a single 16 core CPU
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
                    "cpus": [
                        {
                            "coreCount": 16,
                            "coreSpeed": 1000,
                            "count": 10
                        }
                    ],
                    "memory": {
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
