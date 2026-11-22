---
description: Distribution policies, which determine how the tasks on a host share its CPU or GPU.
---

# Distribution Policy

A *distribution policy* determines how the tasks on a host share the capacity of its CPU or GPU. The policy matters
when the tasks together demand more than the host can supply. OpenDC defines five policies.

## Policies

The field `type` selects the policy:

| Type             | Further fields                                                  | Behaviour                                                                                             |
|------------------|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `maxMinFairness` |                                                                 | Satisfies the smallest demands first, so that no task starves. This is the default.                   |
| `bestEffort`     | `updateInterval` ([duration](../../units.md), default `"1 s"`)  | Serves the tasks in turns, rotating every `updateInterval`, while using as much of the capacity as possible. |
| `equalShare`     |                                                                 | Gives every task the same share of the capacity, whatever its demand.                                 |
| `firstFit`       |                                                                 | Gives each task its full demand, in order, until the capacity is used up.                             |
| `fixedShare`     | `shareRatio` (default `1.0`)                                    | Gives each active task a fixed fraction of the capacity. Shares that a task does not use are not given to other tasks. |

The policies `bestEffort`, `equalShare` and `fixedShare` follow the schedulers of NVIDIA's virtual GPU software.

## Example

``` json
"gpuDistribution": {
    "type": "bestEffort",
    "updateInterval": "1 minutes"
}
```
