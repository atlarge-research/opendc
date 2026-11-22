---
description: Export models, which decide which results OpenDC writes, and how often.
---

# Export Model

An *export model* decides which [output files](../output/index.md) OpenDC writes, which columns they have, and how
often OpenDC takes a sample. A long simulation of a large topology produces many samples, so the export model is the
main control over the size of the output.

## Fields

| Field            | Type                     | Default       | Description                                                                 |
|------------------|--------------------------|---------------|-----------------------------------------------------------------------------|
| `exportInterval` | [duration](../units.md)  | `"5 minutes"` | The simulated time between two samples.                                     |
| `printFrequency` | integer or `null`        | `24`          | Logs a short overview of the tasks every this many samples. `null` turns it off. |
| `filesToExport`  | list of strings          | all files     | The files to write: any of `host`, `task`, `service`, `powerSource`, `battery`, `cluster` and `datacenter`. |
| `columns`        | [columns](#columns)      | all columns   | The columns to write in each file.                                          |

!!! warning "Plain numbers are milliseconds"

    A plain number for `exportInterval` is read in milliseconds: `3600` means 3.6&nbsp;seconds. Write `"1 hours"` for
    an hour.

The [meta files](../output/meta.md) are always written, whatever the export model.

## Columns

`columns` has one entry per file: `host`, `task`, `service`, `powerSource`, `battery`, `cluster` and `dataCenter`. Each
entry selects either all columns of the file, which is the default, or the columns it names:

``` json
{ "type": "all" }
{ "type": "only", "columns": ["power_draw", "energy_usage"] }
```

Three rules apply to a selection:

- The identifier of the entity and `timestamp` are always written.
- A GPU column of the host file is named without the number of the GPU: `gpu_usage` selects `gpu_usage_0`,
  `gpu_usage_1`, and so on.
- OpenDC ignores a name that is not a column of the file, and logs a warning that lists the columns of the file.

## Example

``` json
"exportModels": [
    {
        "exportInterval": "1 hours",
        "printFrequency": 168,
        "filesToExport": ["host", "task", "service"],
        "columns": {
            "host": { "type": "only", "columns": ["power_draw", "energy_usage", "cpu_utilization"] },
            "task": { "type": "only", "columns": ["task_state", "schedule_time", "finish_time"] }
        }
    }
]
```

This export model takes a sample every simulated hour and logs an overview every 168&nbsp;samples, which is once per
simulated week. It writes only the host, task and service files. The host and task files have the selected columns, and
the service file has all of its columns.
