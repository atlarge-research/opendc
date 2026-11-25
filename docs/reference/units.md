---
description: The units of measurement that OpenDC accepts, and how to write a value with a unit in an experiment.
---

# Units

Many values in an experiment are physical quantities, such as a clock speed, an amount of memory or a duration. This
page explains how to write such values, which units OpenDC knows, and what happens to a value without a unit.

## Writing a value with a unit

A quantity is written as a string that holds a number and a unit:

``` json
"coreSpeed": "2.1 GHz",
"memory": { "size": "64 GiB" },
"exportInterval": "1 hours"
```

OpenDC also accepts a plain number, such as `2100` or `"2100"`. It reads a plain number in the *default unit* of the
quantity and logs a warning. We recommend always writing the unit. It makes an experiment easier to read and prevents
mistakes, in particular for durations and energy, whose default units are the millisecond and the joule.

## Available units

| Quantity  | Default unit       | Examples                                                                | Used for                                                                              |
|-----------|--------------------|-------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| Duration  | millisecond        | `"500 ms"`, `"30 s"`, `"5 minutes"`, `"1 hours"`, `"2 days"`, `"PT13H"` | durations of tasks and fragments, export interval, checkpoint interval and duration   |
| Frequency | megahertz          | `"800 kHz"`, `"2100 MHz"`, `"2.1 GHz"`                                  | speed of CPU and GPU cores, speed of memory, CPU and GPU capacity and usage of tasks  |
| Data size | mebibyte           | `"512 MiB"`, `"64 GiB"`, `"100 GB"`, `"1 TiB"`                          | memory of hosts, GPUs and tasks                                                       |
| Data rate | kibibit per second | `"10 Gbps"`, `"512 MiBps"`, `"900 GBps"`                                | memory bandwidth of GPUs                                                              |
| Power     | watt               | `"400 W"`, `"350 Watts"`, `"50 kW"`, `"1 MW"`                           | power models, maximum power of a power source, charging speed of a battery            |
| Energy    | joule              | `"3600 J"`, `"5 MJ"`, `"1 kWh"`, `"100 kWh"`                            | capacity and initial charge of a battery                                              |
| Timestamp | milliseconds since the Unix epoch | `"2024-01-01T00:00:00Z"`                                 | carbon intensities of an inline carbon trace                                          |

A duration can also be written in the ISO 8601 notation, such as `"PT13H"` for 13&nbsp;hours. OpenDC defines one more
quantity, the percentage, which is written as `"25%"` or as the ratio `0.25`. The simulator uses it internally, and no
field of an experiment takes it.

## Rules

The following rules apply to a value with a unit:

- The number can have decimals or an exponent, such as `"1.5 GHz"` or `"128e3 MiB"`. The space between the number and
  the unit is optional.
- A unit can be written as a symbol or as a word, such as `"W"`, `"watt"` or `"Watts"`.
- Decimal prefixes (`k`, `M`, `G`, `T`) are powers of 1000, and binary prefixes (`Ki`, `Mi`, `Gi`, `Ti`) are powers of
  1024. `"1 GB"` is 1000&nbsp;MB, and `"1 GiB"` is 1024&nbsp;MiB.

Upper and lower case matter in three cases:

- `m` means milli and `M` means mega. `"2400 mhz"` is an error, and the correct form is `"2400 MHz"`. In the same way,
  `"1 MW"` is a megawatt.
- In a duration, `m` means minutes and `ms` means milliseconds.
- In a data size or a data rate, `b` is a bit and `B` is a byte. `"100 Gb"` is 100&nbsp;gigabits.

## Values without a unit

Counts, ratios and multipliers are plain numbers, such as `coreCount`, `allocationRatio` and `multiplier`. A few other
plain numbers have a fixed unit, which the page of the field states. Examples are the embodied carbon of a
[battery](experiment/topology/battery.md), in kilograms, and the distributions of a
[failure model](experiment/failure-model.md#custom-models), in hours.

The [output files](output/index.md) use fixed units as well, which the pages of the files list.

## Units in code

The units are defined in the module `opendc-common`, in the package `org.opendc.common.units`. The
[Kotlin SDK](../kotlin-sdk/model.md#units) and the [Python SDK](../python-sdk/specs.md#units) construct quantities with
functions such as `2.1.ghz` and `gib(64)`, so that a quantity in code never lacks a unit.
