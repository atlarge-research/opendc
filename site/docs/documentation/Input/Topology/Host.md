A host is a machine that can execute tasks. A host consist of the following components:

| variable    | type                                                         | required? | default | description                                                                    |
|-------------|:-------------------------------------------------------------|:----------|---------|--------------------------------------------------------------------------------|
| name        | string                                                       | no        | Host    | The name of the host. This is only important for debugging and post-processing |
| count       | integer                                                      | no        | 1       | The amount of hosts of this type are in the cluster                            |
| cpuModel    | [CPU](#cpu)                                                  | yes       | N/A     | The CPUs in the host                                                           |
| memory      | [Memory](#memory)                                            | yes       | N/A     | The memory used by the host                                                    |
| power model | [Power Model](/docs/documentation/Input/Topology/PowerModel) | no        | Default | The power model used to determine the power draw of the host                   |

## CPU

| variable  | type    | Unit  | required? | default | description                                      |
|-----------|---------|-------|-----------|---------|--------------------------------------------------|
| modelName | string  | N/A   | no        | unknown | The name of the CPU.                             |
| vendor    | string  | N/A   | no        | unknown | The vendor of the CPU                            |
| arch      | string  | N/A   | no        | unknown | the micro-architecture of the CPU                |
| count     | integer | N/A   | no        | 1       | The number of CPUs of this type used by the host |
| coreCount | integer | count | yes       | N/A     | The number of cores in the CPU                   |
| coreSpeed | Double  | Mhz   | yes       | N/A     | The speed of each core in Mhz                    |

## Memory

| variable    | type    | Unit | required? | default | description                                                              |
|-------------|---------|------|-----------|---------|--------------------------------------------------------------------------|
| modelName   | string  | N/A  | no        | unknown | The name of the CPU.                                                     |
| vendor      | string  | N/A  | no        | unknown | The vendor of the CPU                                                    |
| arch        | string  | N/A  | no        | unknown | the micro-architecture of the CPU                                        |
| memorySize  | integer | Byte | yes       | N/A     | The number of cores in the CPU                                           |
| memorySpeed | Double  | Mhz  | no        | -1      | The speed of each core in Mhz. PLACEHOLDER: this currently does nothing. |

## Example

```json
{
    "name": "H01",
    "cpu": {
        "coreCount": 16,
        "coreSpeed": 2100
    },
    "memory": {
        "memorySize": 100000
    },
    "powerModel": {
        "modelType": "sqrt",
        "idlePower": 32.0,
        "maxPower": 180.0
    },
    "count": 100
}
```

This example creates 100 hosts with 16 cores and 2.1 Ghz CPU speed, and 100 GB of memory.
The power model used is a square root model with a power of 400 W, idle power of 32 W, and max power of 180 W.
For more information on the power model, see [Power Model](/docs/documentation/Input/Topology/PowerModel).
