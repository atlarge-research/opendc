---
description: The units of measure OpenDC 2 uses for input values.
status: deprecated
search:
  exclude: true
---

# Units

This page lists the units of the input values of OpenDC 2. Only values in a [topology](topology.md) accept a unit. All
other values are plain numbers with a fixed unit.

## Values with a unit

The following values accept a string with a unit, such as `"2.1 GHz"`, or a plain number in the default unit:

| Quantity  | Default unit | Examples                            | Used for                                            |
|-----------|--------------|-------------------------------------|-----------------------------------------------------|
| Frequency | MHz          | `"2100 MHz"`, `"2.1 GHz"`           | `coreSpeed` of CPUs and GPUs, `memorySpeed`         |
| Data size | MiB          | `"512 MiB"`, `"64 GiB"`, `"100 GB"` | `memorySize` of the memory and of GPUs              |
| Data rate | Kibps        | `"10 Gbps"`, `"900 GBps"`           | `memoryBandwidth` of GPUs                           |
| Power     | W            | `"400 W"`, `"1 kW"`                 | `power`, `idlePower` and `maxPower` of power models |

The number can have decimals or an exponent, such as `"128e3 MiB"`. Decimal prefixes (`k`, `M`, `G`) are powers of 1000
and binary prefixes (`Ki`, `Mi`, `Gi`) are powers of 1024. In data sizes and rates, `b` is a bit and `B` is a byte.

## Values with a fixed unit

The following values are plain numbers in a fixed unit:

| Value                                                                                                       | Unit         |
|-------------------------------------------------------------------------------------------------------------|--------------|
| `exportInterval` of an [export model](experiment.md#export-model)                                           | seconds      |
| `checkpointInterval` and `checkpointDuration` of a [checkpoint model](experiment.md#checkpoint-model)       | milliseconds |
| `updateIntervalLength` of the `BEST_EFFORT` [distribution policy](topology.md#distribution-policy)          | milliseconds |
| `maxPower` of a [power source](topology.md#power-source), `chargingSpeed` of a [battery](topology.md#battery) | watts      |
| `capacity` and `initialCharge` of a battery                                                                 | kWh          |
| `embodiedCarbon` of a battery                                                                               | grams        |
| `expectedLifetime` of a battery                                                                             | years        |
| failure distributions (`iatSampler`, `durationSampler`) of a [failure model](experiment.md#custom-models)   | hours        |

[Output](output.md) lists the units of the output files.
