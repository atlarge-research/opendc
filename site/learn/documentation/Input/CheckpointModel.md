Checkpointing is a technique to reduce the impact of machine failure. 
When using Checkpointing, tasks make periodical snapshots of their state.
If a task fails, it can be restarted from the last snapshot instead of starting from the beginning.

A user can define a checkpoint model using the following parameters:

| Variable                  | Type   | Required? | Default | Description                                                                                                          |
|---------------------------|--------|-----------|---------|----------------------------------------------------------------------------------------------------------------------|
| checkpointInterval        | Int64  | no        | 3600000 | The time between checkpoints in ms                                                                                   |
| checkpointDuration        | Int64  | no        | 300000  | The time to create a snapshot in ms                                                                                  |
| checkpointIntervalScaling | Double | no        | 1.0     | The scaling of the checkpointInterval after each successful checkpoint. The default of 1.0 means no scaling happens. |

### Example

```json
{
    "checkpointInterval": 3600000,
    "checkpointDuration": 300000,
    "checkpointIntervalScaling": 1.5
}
```

In this example, a snapshot is created every hour, and the snapshot creation takes 5 minutes.
The checkpointIntervalScaling is set to 1.5, which means that after each successful checkpoint, 
the interval between checkpoints will be increased by 50% (for example from 1 to 1.5 hours).
