A host is a machine that can execute tasks. A host consist of the following components:

| variable              | type                                                                         | required? | default | description                                                                    |
|-----------------------|:-----------------------------------------------------------------------------|:----------|---------|--------------------------------------------------------------------------------|
| name                  | string                                                                       | no        | Host    | The name of the host. This is only important for debugging and post-processing |
| count                 | integer                                                                      | no        | 1       | The amount of hosts of this type are in the cluster                            |
| cpuModel              | [CPU](#cpu)                                                                  | yes       | N/A     | The CPUs in the host                                                           |
| memory                | [Memory](#memory)                                                            | yes       | N/A     | The memory used by the host                                                    |
| cpuPowerModel         | [Power Model](/docs/documentation/Input/Topology/PowerModel)                 | no        | Default | The power model used to determine the power draw of the cpu                    |
| gpuPowerModel         | [Power Model](/docs/documentation/Input/Topology/PowerModel)                 | no        | Default | The power model used to determine the power draw of the gpu                    |
| cpuDistributionPolicy | [Distribution Policy](/docs/documentation/Input/Topology/DistributionPolicy) | no        | Default | The distribution policy used for the CPU                                       |
| gpuDistributionPolicy | [Distribution Policy](/docs/documentation/Input/Topology/DistributionPolicy) | no        | Default | The distribution policy used for the GPU                                       |

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

## GPU 

GPUS are an optional component of a host. The required fields are only required if the host has GPUs.

| variable                     | type                                                                                          | Unit             | required? | default | description                                                            |
|------------------------------|-----------------------------------------------------------------------------------------------|------------------|-----------|---------|------------------------------------------------------------------------|
| modelName                    | string                                                                                        | N/A              | no        | unknown | The name of the GPU.                                                   |
| vendor                       | string                                                                                        | N/A              | no        | unknown | The vendor of the GPU                                                  |
| arch                         | string                                                                                        | N/A              | no        | unknown | the micro-architecture of the GPU                                      |
| count                        | integer                                                                                       | N/A              | no        | 1       | The number of GPUs of this type used by the host                       |
| coreCount                    | integer                                                                                       | count            | yes       | N/A     | The number of cores in the GPU                                         |
| coreSpeed                    | Double                                                                                        | Mhz              | yes       | N/A     | The speed of each core in Mhz                                          |
| memorySize                   | integer                                                                                       | Byte             | no        | N/A     | The speed of each core in Mhz                                          |
| memoryBandwidth              | Double                                                                                        | Bytes per Second | no        | N/A     | The speed of each core in Mhz                                          |
| virtualizationOverHeadModel  | [VirtualizationOverHeadModel](/docs/documentation/Input/Topology/VirtualizationOverHeadModel) | N/A              | no        | N/A     | The virtualization model of the GPU, used to determine the performance |

## Example - No GPU

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
    "CpuPowerModel": {
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

## Example - With GPU

```json
{
    "clusters": [
        {
            "name": "C01",
            "hosts": [
                {
                    "name": "DualGpuHost",
                    "cpu": {
                        "coreCount": 4,
                        "coreSpeed": 2000
                    },
                    "memory": {
                        "memorySize": 140457600000
                    },
                    "cpuPowerModel": {
                        "modelType": "linear",
                        "power": 400.0,
                        "idlePower": 100.0,
                        "maxPower": 200.0
                    },
                    "cpuDistributionPolicy": {
                        "type": "MAX_MIN_FAIRNESS"
                    },
                    "gpu": {
                        "coreCount": 2,
                        "coreSpeed": 2000,
                        "virtualizationOverHeadModel": {
                            "type": "CONSTANT",
                            "percentageOverhead": 0.25
                        }
                    },
                    "gpuPowerModel": {
                        "modelType": "linear",
                        "power": 400.0,
                        "idlePower": 100.0,
                        "maxPower": 200.0
                    },
                    "gpuDistributionPolicy": {
                        "type": "FIXED_SHARE",
                        "shareRatio": 0.5
                    }
                }
            ]
        }
    ]
}
```

This example creates a host with 4 CPU cores and 2 GPU cores, both with a speed of 2.0 Ghz.
The host has 140 GB of memory and uses a linear power model for both CPU and GPU with a power of 400 W, idle power of 100 W, and max power of 200 W.
For more information on the power model, see [Power Model](/docs/documentation/Input/Topology/PowerModel).
