---
description: The files that OpenDC writes during a simulation, where it writes them, and how to read them.
---

# Output

During a run, OpenDC regularly takes a *sample* of the state of the datacenter and writes it to Parquet files. The
[export model](../experiment/export-model.md) decides which files are written, which columns they have, and how often
OpenDC takes a sample. This page describes where the files are, what they have in common, and how to read them.

## Where the results are written

Each run has its own folder, `<output>/<experiment name>/raw-output/<scenario>/seed=<seed>/`. `<output>` is the folder
given with `opendc run -o`, and `<scenario>` is the [number of the scenario](../experiment/index.md#scenarios).

``` text
output/
  simple/                   the name of the experiment
    raw-output/
      0/                    scenario 0
        seed=0/             the run with seed 0
          host.parquet
          task.parquet
          service.parquet
          powerSource.parquet
          battery.parquet
          cluster.parquet
          dataCenter.parquet
          meta/
        seed=1/             the run with seed 1
      1/                    scenario 1
```

The archive that the [web application](../../web/using.md#reading-the-results) offers for download has the same layout.

## Output files

| File                                           | One row per                               |
|------------------------------------------------|-------------------------------------------|
| [`host.parquet`](host.md)                      | host and sample                           |
| [`task.parquet`](task.md)                      | task and sample, and per change of state  |
| [`service.parquet`](service.md)                | sample                                    |
| [`powerSource.parquet`](energy.md#powersourceparquet) | power source and sample            |
| [`battery.parquet`](energy.md#batteryparquet)  | battery and sample                        |
| [`cluster.parquet`](energy.md#clusterparquet)  | cluster and sample                        |
| [`dataCenter.parquet`](energy.md#datacenterparquet) | datacenter and sample                |
| [`meta/`](meta.md)                             | entity: the names and fixed properties of hosts, tasks, clusters and so on |

## Conventions

Three conventions apply to all output files.

Identifiers
:   The sample files hold only values that change during a simulation, which keeps them small. Hosts, tasks, clusters,
    datacenters, power sources and batteries are identified by an integer, such as `host_id`. The
    [meta files](meta.md) map each identifier to a name and to fixed properties, such as the number of cores of a host.

Time
:   `timestamp` and all other times are in milliseconds since the start of the simulation. The simulation starts when
    the first task is submitted. `meta/simulation.parquet` holds the absolute start time, in milliseconds since the Unix
    epoch. Adding it to a time gives an absolute time.

Totals
:   Energy, carbon emissions, uptime and CPU time are totals since the start of the simulation, not amounts per sample.
    The difference between two consecutive samples of an entity gives the amount for the interval between them.

## Example

The following Python script reads the host file of a run, and adds the names of the hosts and absolute times. It needs
the packages `pandas` and `pyarrow`.

``` python
import pandas as pd

run = "output/simple/raw-output/0/seed=0"

host = pd.read_parquet(f"{run}/host.parquet")
host_meta = pd.read_parquet(f"{run}/meta/host.parquet")
start_time = pd.read_parquet(f"{run}/meta/simulation.parquet")["start_time"].iloc[0]

host = host.merge(host_meta[["host_id", "host_name"]], on="host_id")
host["time"] = pd.to_datetime(host["timestamp"] + start_time, unit="ms")
```

The [Python SDK](../../python-sdk/results.md) performs this reading itself and returns every file as a DataFrame.
