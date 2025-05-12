Batteries can be used to store energy for later use. 
In previous work, we have used batteries to store energy from the grid when the carbon intensity is low, 
and use this energy when the carbon intensity is high.

Batteries are defined using the following parameters:

| variable         | type                      | Unit  | required? | default | description                                                                       |
|------------------|---------------------------|-------|-----------|---------|-----------------------------------------------------------------------------------|
| name             | string                    | N/A   | no        | Battery | The name of the battery. This is only important for debugging and post-processing |
| capacity         | Double                    | kWh   | yes       | N/A     | The total amount of energy that the battery can hold.                             |
| chargingSpeed    | Double                    | W     | yes       | N/A     | Charging speed of the battery.                                                    |
| initialCharge    | Double                    | kWh   | no        | 0.0     | The initial charge of the battery. If not given, the battery starts empty.        |
| batteryPolicy    | [Policy](#battery-policy) | N/A   | yes       | N/A     | The policy which decides when to charge and discharge.                            |
| embodiedCarbon   | Double                    | gram  | no        | 0.0     | The embodied carbon emitted while creating this battery.                          |
| expectedLifetime | Double                    | Years | yes       | 0.0     | The expected lifetime of the battery.                                             |

## Battery Policy
To determine when to charge and discharge the battery, a policy is required.
Currently, all policies for batteries are based on the carbon intensity of the grid.

The best performing policy is called "runningMeanPlus" and is based on the running mean of the carbon intensity.
it can be defined with the following JSON:

```json
{
    "type": "runningMeanPlus",
    "startingThreshold": 123.2,
    "windowSize": 168
}
```

In which `startingThreshold` is the initial carbon threshold used.
`windowSize` is the size of the window used to calculate the running mean.

:::info Alert
This page with be extended with more text and policies in the future.
:::
