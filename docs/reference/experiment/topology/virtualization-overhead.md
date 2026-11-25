---
description: Virtualisation overhead models, which determine the performance that a shared GPU loses.
---

# Virtualisation Overhead

A *virtualisation overhead model* determines how much performance a GPU loses when virtual machines share it. The
overhead lowers the capacity that the GPU supplies. The field `virtualizationOverhead` of a [GPU](host.md#gpu) sets the
model.

## Models

The field `type` selects the model:

| Type         | Further fields       | Behaviour                                                                                                    |
|--------------|----------------------|--------------------------------------------------------------------------------------------------------------|
| `none`       |                      | No overhead, as with GPU pass-through. This is the default.                                                  |
| `constant`   | `percentageOverhead` | A fixed overhead, as a fraction of the capacity. Without `percentageOverhead`, the overhead is 0.05, that is 5%. |
| `shareBased` |                      | An overhead that depends on the number of virtual machines that share the GPU.                               |

The `shareBased` model is based on Garg et al.,
[*Empirical Analysis of Hardware-Assisted GPU Virtualization*](https://doi.org/10.1109/HiPC.2019.00054) (HiPC 2019).

## Example

``` json
"virtualizationOverhead": {
    "type": "constant",
    "percentageOverhead": 0.25
}
```
