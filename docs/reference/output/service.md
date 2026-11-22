---
description: The columns of service.parquet, which summarises the whole simulation at each sample.
---

# Service File

`service.parquet` gives an overview of the whole simulation: how many hosts are available, and how many tasks wait, run
and have finished. It has one row per sample.

## Columns

| Column             | Type  | Unit  | Description                                                          |
|--------------------|-------|-------|----------------------------------------------------------------------|
| `timestamp`        | int64 | ms    | The time of the sample.                                              |
| `hosts_up`         | int32 | count | The number of hosts that are up.                                     |
| `hosts_down`       | int32 | count | The number of hosts that are down.                                   |
| `tasks_total`      | int32 | count | The number of tasks submitted so far.                                |
| `tasks_pending`    | int32 | count | The number of tasks that wait to be scheduled.                       |
| `tasks_active`     | int32 | count | The number of tasks that run.                                        |
| `tasks_completed`  | int32 | count | The number of tasks that completed.                                  |
| `tasks_terminated` | int32 | count | The number of tasks that were terminated.                            |
| `attempts_success` | int32 | count | The number of scheduling attempts that placed a task on a host.      |
| `attempts_failure` | int32 | count | The number of scheduling attempts that failed.                       |

The last row of the file summarises the outcome of a run: `tasks_completed` and `tasks_terminated` together account
for every task that ended.
