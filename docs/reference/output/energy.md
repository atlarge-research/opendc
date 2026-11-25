---
description: The columns of powerSource.parquet, cluster.parquet, dataCenter.parquet and battery.parquet, which describe power, energy and carbon.
---

# Energy and Carbon Files

Four files describe the power draw, the energy use and the carbon emissions of a simulation, each at another level of
the [topology](../experiment/topology/index.md). `powerSource.parquet`, `cluster.parquet` and `dataCenter.parquet`
share their columns, and `battery.parquet` describes the batteries.

## Shared columns

The files of power sources, clusters and datacenters have the following columns, besides the identifier of their
entity:

| Column             | Type  | Unit  | Description                                                    |
|--------------------|-------|-------|----------------------------------------------------------------|
| `timestamp`        | int64 | ms    | The time of the sample.                                        |
| `power_draw`       | float | W     | The power that the entity draws or supplies.                   |
| `energy_usage`     | float | J     | The energy used or supplied since the start of the simulation. |
| `carbon_intensity` | float | g/kWh | The carbon intensity of the energy.                            |
| `carbon_emission`  | float | g     | The carbon emitted since the start of the simulation.          |

The carbon intensity comes from the [carbon model](../experiment/topology/power-source.md#carbon-models) of the power
source. Without a carbon model, `carbon_intensity` and `carbon_emission` are 0.

## powerSource.parquet

`powerSource.parquet` has one row per [power source](../experiment/topology/power-source.md) and sample. It describes
the power that each power source supplies, and the carbon emitted to produce it.

| Column            | Type  | Description                                                              |
|-------------------|-------|--------------------------------------------------------------------------|
| `power_source_id` | int32 | The identifier of the power source. See [`meta/powerSource.parquet`](meta.md). |

## cluster.parquet

`cluster.parquet` has one row per [cluster](../experiment/topology/index.md#cluster) and sample. It describes the power
draw and the carbon emissions of the hosts of each cluster.

| Column       | Type  | Description                                                         |
|--------------|-------|---------------------------------------------------------------------|
| `cluster_id` | int32 | The identifier of the cluster. See [`meta/cluster.parquet`](meta.md). |

## dataCenter.parquet

`dataCenter.parquet` has one row per [datacenter](../experiment/topology/index.md#datacenter) and sample. It describes
the power draw and the carbon emissions of each datacenter.

| Column           | Type  | Description                                                                 |
|------------------|-------|-----------------------------------------------------------------------------|
| `data_center_id` | int32 | The identifier of the datacenter. See [`meta/dataCenter.parquet`](meta.md). |

## battery.parquet

`battery.parquet` has one row per [battery](../experiment/topology/battery.md) and sample. It describes the charge and
the state of each battery. The file has no rows when the topology has no battery.

| Column                     | Type   | Unit | Description                                                              |
|----------------------------|--------|------|--------------------------------------------------------------------------|
| `timestamp`                | int64  | ms   | The time of the sample.                                                  |
| `battery_id`               | int32  |      | The identifier of the battery. See [`meta/battery.parquet`](meta.md).    |
| `battery_state`            | string |      | `CHARGING`, `IDLE` or `DISCHARGING`.                                     |
| `charge`                   | float  | J    | The energy stored in the battery.                                        |
| `power_draw`               | float  | W    | The power that the battery supplies.                                     |
| `energy_usage`             | float  | J    | The energy that the battery supplied since the start of the simulation.  |
| `embodied_carbon_emission` | float  | g    | The embodied carbon of the battery.                                      |

## Converting units

The files state energy in joules and carbon in grams. One kilowatt-hour is 3.6&nbsp;MJ, so dividing `energy_usage` by
3,600,000 gives kilowatt-hours. Because both columns are totals, the last sample of each power source holds the result
of the whole run. The following script computes the energy that the power sources of a run supplied, and the carbon
emitted to produce it:

``` python
import pandas as pd

power = pd.read_parquet("output/simple/raw-output/0/seed=0/powerSource.parquet")
last = power.groupby("power_source_id").last()
print("Energy:", last["energy_usage"].sum() / 3.6e6, "kWh")
print("Carbon:", last["carbon_emission"].sum() / 1000, "kg")
```
