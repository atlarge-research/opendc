OpenDC uses power models to determine the power draw based on the utilization of a host. 
All models in OpenDC are based on linear models interpolated between the idle and max power draw.
OpenDC currently supports the following power models:
1. **Constant**: The power draw is constant and does not depend on the utilization of the host.
2. **Sqrt**: The power draw interpolates between idle and max using a square root function.
3. **Linear**: The power draw interpolates between idle and max using a linear function.
4. **Square**: The power draw interpolates between idle and max using a square function.
5. **Cubic**The power draw interpolates between idle and max using a cubic function.
6. **MSE**: The power draw interpolates between idle and max using a mean square error function.
7. **Asymptotic**: The power draw interpolates between idle and max using an asymptotic function.

The power model is defined using the following parameters:

| variable          | type    | Unit | required? | default | description                                                                |
|-------------------|---------|------|-----------|---------|----------------------------------------------------------------------------|
| modelType         | string  | N/A  | yes       | N/A     | The type of model used to determine power draw                             |
| power             | double  | Mhz  | no        | 400     | The power draw of a host when using the constant power draw model.         |
| idlePower         | double  | Mhz  | yes       | N/A     | The power draw of a host when idle in Watt.                                |
| maxPower          | double  | Mhz  | yes       | N/A     | The power draw of a host when using max capacity in Watt.                  |
| calibrationFactor | double  | Mhz  | no        | N/A     | The parameter set to minimize the mean squared error.                      |
| asymUtil          | double  | Mhz  | no        | N/A     | A utilization level at which the host attains asymptotic.                  |
| dvfs              | Boolean | N/A  | no        | N/A     | A flag indicates whether dynamic voltage and frequency scaling is enabled. |


## Example

```json
{
    "modelType": "sqrt",
    "idlePower": 32.0,
    "maxPower": 180.0
}
```

This creates a power model that uses a square root function to determine the power draw of a host.
The model uses an idle and max power of 32 W and 180 W respectively.
