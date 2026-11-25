---
description: Batteries, which store energy for a datacenter to use when the carbon intensity is high.
---

# Battery

A [datacenter](index.md#datacenter) can have a *battery*. A battery stores energy from the power source and supplies
it to the hosts later. OpenDC uses batteries to lower the carbon emissions of a datacenter: a battery charges while
the carbon intensity of the power source is low, and discharges while it is high. This page lists the fields of a
battery and the policies that decide when it charges.

## Fields

| Field              | Type                          | Default     | Description                                                     |
|--------------------|-------------------------------|-------------|-----------------------------------------------------------------|
| `name`             | string                        | `"Battery"` | The name of the battery.                                        |
| `capacity`         | [energy](../../units.md)      | required    | The amount of energy that the battery can store.                |
| `chargingSpeed`    | [power](../../units.md)       | required    | The power at which the battery charges.                         |
| `initialCharge`    | [energy](../../units.md)      | `0`         | The charge at the start of the simulation.                      |
| `policy`           | [policy](#battery-policies)   | required    | When the battery charges and discharges.                        |
| `embodiedCarbon`   | number                        | `0`         | The carbon emitted to manufacture the battery, in kg.           |
| `expectedLifetime` | number                        | `0`         | The expected lifetime of the battery, in years.                 |

!!! warning "Plain numbers are joules"

    A plain number for `capacity` or `initialCharge` is read in joules: `100` means 100&nbsp;J. Write `"100 kWh"` for
    kilowatt-hours.

## Battery policies

A *battery policy* decides when the battery charges and discharges. Every policy compares the carbon intensity of the
power source with a threshold. The battery charges while the carbon intensity is low, and discharges while it is high.
The policies differ in how they set the threshold.

The field `type` selects the policy:

| Type              | Further fields                     | The carbon intensity is low when it is                                                              |
|-------------------|------------------------------------|-----------------------------------------------------------------------------------------------------|
| `single`          | `carbonThreshold`                  | below `carbonThreshold`. Otherwise it is high.                                                      |
| `double`          | `lowerThreshold`, `upperThreshold` | below `lowerThreshold`. It is high above `upperThreshold`, and in between the battery is idle.      |
| `runningMean`     | `startingThreshold`, `windowSize`  | below the mean of the last `windowSize` carbon intensities.                                         |
| `runningMeanPlus` | `startingThreshold`, `windowSize`  | below that mean as well, but the battery only starts to charge once the carbon intensity stops decreasing. |

All thresholds are carbon intensities in g/kWh. Battery policies react to the carbon intensity, so a battery is only
useful in a datacenter whose power source has a [carbon model](power-source.md#carbon-models).

## Example

``` json
"battery": {
    "capacity": "100 kWh",
    "chargingSpeed": "5000 W",
    "initialCharge": "20 kWh",
    "policy": { "type": "runningMeanPlus", "startingThreshold": 150.0, "windowSize": 168 },
    "embodiedCarbon": 1200.0,
    "expectedLifetime": 10.0
}
```
