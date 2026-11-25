---
description: The meta files, which map the identifiers in the output to names and fixed properties.
---

# Meta Files

Each run writes a folder `meta/` next to its output files. The *meta files* map the identifiers in the output files to
names and fixed properties, and state which entity is part of which. This page lists the meta files and explains how
to combine them with the output files.

## Files

| File                       | Columns                                                                                                   |
|----------------------------|-----------------------------------------------------------------------------------------------------------|
| `meta/simulation.parquet`  | `start_time`: the moment at which the simulation starts, in milliseconds since the Unix epoch             |
| `meta/dataCenter.parquet`  | `data_center_id`, `data_center_name`                                                                      |
| `meta/cluster.parquet`     | `cluster_id`, `cluster_name`, `data_center_id`                                                            |
| `meta/host.parquet`        | `host_id`, `host_name`, `cluster_id`, `core_count`, `cpu_capacity` (MHz), `mem_capacity` (MiB), and `gpu_capacity_i` (MHz) for each GPU |
| `meta/powerSource.parquet` | `power_source_id`, `power_source_name`, `data_center_id`                                                  |
| `meta/battery.parquet`     | `battery_id`, `battery_name`, `data_center_id`, `capacity` (J)                                            |
| `meta/task.parquet`        | `task_id`, `cpu_count`, `mem_capacity` (MiB), `gpu_count`, `submission_time` (ms since the start of the simulation) |
| `meta/trackr.json`         | the scenario that the run simulated, as a JSON document                                                   |

A meta file always has all of its columns, whatever the [export model](../experiment/export-model.md) selects. OpenDC
writes every meta file for every run, with one exception: `meta/task.parquet` is written together with `task.parquet`
and has one row per task that was submitted.

## Joining meta files

An output file and the meta file of the same name share an identifier column. Joining the two on that column adds the
names and properties to the samples, for example `host.parquet` with `meta/host.parquet` on `host_id`. The columns
`cluster_id` and `data_center_id` of the meta files lead from a host to its cluster and from a cluster to its
datacenter.

Identifiers are assigned in the order of the topology. They are meaningful only within their own run: the host with
`host_id` 3 in one scenario is not the host with that identifier in a scenario with another topology.

## The scenario of a run

`meta/trackr.json` records what a run simulated: the topology, the workload, the policies and models, and the seed. It
is a [scenario](../../kotlin-sdk/model.md#experiments-and-scenarios) in the JSON form of the SDK, with every default
written out. The file makes a results folder self-describing, because it remains with the results when they are moved
or shared.
