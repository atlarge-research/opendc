---
description: Power models, which determine the power draw of a CPU or GPU from its utilisation.
---

# Power Model

A *power model* determines the power that a component draws, based on its utilisation. CPUs and GPUs have power
models. OpenDC defines seven models, which this page lists with their formulas.

## Models

The field `type` selects the model. In the formulas of the following table, *u* is the utilisation of the component,
between 0 and 1. *P*~idle~ and *P*~max~ are the fields `idlePower` and `maxPower`, and *P*~Δ~ = *P*~max~ − *P*~idle~
is the difference between them.

| Type         | Further fields                              | Power draw *P*(*u*)                                          |
|--------------|---------------------------------------------|--------------------------------------------------------------|
| `constant`   | `power`                                     | `power`, whatever the utilisation                            |
| `linear`     | `idlePower`, `maxPower`                     | *P*~idle~ + *P*~Δ~ · *u*                                     |
| `square`     | `idlePower`, `maxPower`                     | *P*~idle~ + *P*~Δ~ · *u*^2^                                  |
| `cubic`      | `idlePower`, `maxPower`                     | *P*~idle~ + *P*~Δ~ · *u*^3^                                  |
| `sqrt`       | `idlePower`, `maxPower`                     | *P*~idle~ + *P*~Δ~ · √*u*                                    |
| `mse`        | `idlePower`, `maxPower`, `calibrationFactor` | *P*~idle~ + *P*~Δ~ · (2*u* − *u*^*r*^), with *r* the calibration factor |
| `asymptotic` | `idlePower`, `maxPower`, `asymUtil`, `dvfs` | *P*~idle~ + ½ *P*~Δ~ · (1 + *u* − e^−*u*/*a*^), with *a* the value of `asymUtil` |

All power fields are [power](../../units.md) values, and `idlePower` cannot be higher than `maxPower`. Two models need
further explanation:

- The `mse` model is fitted to measurements by minimising the mean squared error, after Fan et al., *Power provisioning
  for a warehouse-sized computer* (ISCA 2007). The calibration factor has to be greater than 0.
- The `asymptotic` model is adapted from the GreenCloud simulator. Above the utilisation `asymUtil`, the power draw is
  close to linear in the load. Typical values of `asymUtil` are between 0.2 and 0.5. With `dvfs`, which is `true` by
  default, the model accounts for dynamic voltage and frequency scaling: *u*^3^ takes the place of *u* in the formula.

A host that states no power model uses the `linear` model with an idle power of 200&nbsp;W and a maximum power of
400&nbsp;W.

## Example

``` json
"cpuPowerModel": {
    "type": "sqrt",
    "idlePower": "32 W",
    "maxPower": "180 W"
}
```
