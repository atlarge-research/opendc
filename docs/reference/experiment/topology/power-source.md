---
description: The power source of a datacenter, and the carbon model from which OpenDC computes carbon emissions.
---

# Power Source

Each [datacenter](index.md#datacenter) has a *power source* that supplies its hosts. The power source also carries
the *carbon model*, which states how much carbon the production of its energy emits. This page describes both.

## Fields

| Field      | Type                           | Default          | Description                                         |
|------------|--------------------------------|------------------|-----------------------------------------------------|
| `name`     | string                         | `"PowerSource"`  | The name of the power source.                       |
| `maxPower` | [power](../../units.md)        | unlimited        | The maximum power that the source can supply.       |
| `carbon`   | [carbon model](#carbon-models) | no carbon model  | The carbon intensity of the energy over time.       |

## Carbon models

The *carbon intensity* of energy is the mass of carbon dioxide that its production emits, in g/kWh. It changes over
time with the mix of energy sources. OpenDC multiplies the energy that a datacenter uses by the carbon intensity at
that moment to obtain its carbon emissions. Without a carbon model, the carbon emissions in the output are 0.

The field `type` selects one of two models:

| Type           | Fields                                                              | Carbon intensity                        |
|----------------|---------------------------------------------------------------------|-----------------------------------------|
| `trace`        | `source`: a [trace reference](../index.md#referencing-trace-files)  | read from a [carbon trace](#carbon-traces) |
| `inline-trace` | `intensities`: a list of carbon intensities                         | listed in the experiment                |

A carbon intensity of the `inline-trace` model has two fields:

| Field             | Type      | Description                                                                              |
|-------------------|-----------|------------------------------------------------------------------------------------------|
| `timestamp`       | timestamp | The moment from which the carbon intensity applies, such as `"2024-01-01T00:00:00Z"`.    |
| `carbonIntensity` | number    | The carbon intensity, in g/kWh. It cannot be negative.                                   |

A carbon intensity applies from its timestamp until the next one starts. The first carbon intensity also applies before
its timestamp, and the last one until the end of the simulation. Two carbon intensities cannot have the same timestamp.

!!! note "Earlier format"

    Earlier versions wrote a carbon trace as a bare trace reference, such as
    `"carbon": { "type": "named", "name": "..." }`. OpenDC still reads this format and logs a warning.

## Carbon traces

A carbon trace is a Parquet file with one row per change of the carbon intensity:

| Column             | Type                     | Description                                           |
|--------------------|--------------------------|-------------------------------------------------------|
| `timestamp`        | timestamp (milliseconds) | The moment from which the carbon intensity applies.   |
| `carbon_intensity` | double                   | The carbon intensity, in g/kWh.                       |

Both columns are required. The timestamps of a carbon trace are absolute. For a meaningful result, the trace should
cover the period in which the workload runs: before the first row of the trace, its first carbon intensity applies, and
after the last row, its last. [`submissionTime`](../workload.md#trace-workloads) moves a workload into the period of a
carbon trace.

## Examples

A power source with a carbon trace:

``` json
"powerSource": {
    "name": "grid",
    "carbon": {
        "type": "trace",
        "source": { "type": "named", "name": "carbon_traces/NL_2021-2024.parquet" }
    }
}
```

A power source with a limited supply and two carbon intensities:

``` json
"powerSource": {
    "maxPower": "50 kW",
    "carbon": {
        "type": "inline-trace",
        "intensities": [
            { "timestamp": "2024-01-01T00:00:00Z", "carbonIntensity": 300.0 },
            { "timestamp": "2024-01-01T12:00:00Z", "carbonIntensity": 100.0 }
        ]
    }
}
```
