---
description: Checkpoint models, which let tasks save their progress so that a failure loses less work.
---

# Checkpoint Model

Checkpointing reduces the impact of [failures](failure-model.md). With checkpointing, a task periodically saves a
snapshot of its progress, called a *checkpoint*. After a failure, the task does not start from the beginning, but from
its last checkpoint. A *checkpoint model* defines when tasks create checkpoints and what a checkpoint costs.

Checkpointing is off by default. An experiment turns it on by listing at least one checkpoint model in
[`checkpointModels`](index.md#fields).

## Fields

| Field             | Type                     | Default       | Description                                                                                  |
|-------------------|--------------------------|---------------|----------------------------------------------------------------------------------------------|
| `interval`        | [duration](../units.md)  | `"1 hours"`   | The time between two checkpoints.                                                            |
| `duration`        | [duration](../units.md)  | `"5 minutes"` | How long it takes to create a checkpoint.                                                    |
| `intervalScaling` | number                   | `1.0`         | The factor by which `interval` grows after each checkpoint. `1.0` keeps the interval constant. |

All three values have to be greater than 0. The time a task spends on checkpoints is reported in the column
`checkpoint_delay` of [`task.parquet`](../output/task.md).

## Example

``` json
"checkpointModels": [
    { "interval": "1 hours", "duration": "5 minutes", "intervalScaling": 1.5 }
]
```

With this model, the interval between the checkpoints of a task grows by half each time: the first interval is
1&nbsp;hour, the second 1.5&nbsp;hours, the third 2.25&nbsp;hours, and so on.

To compare a scenario with checkpointing against one without, list both:

``` json
"checkpointModels": [
    null,
    { "interval": "1 hours", "duration": "5 minutes" }
]
```
