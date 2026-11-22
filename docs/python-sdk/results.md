---
description: How the Python SDK returns the results of a simulation as pandas DataFrames.
---

# Results

`simulate` returns a `SimulationResults` object, which holds every table that the simulation exported as a pandas
DataFrame. This page describes how to simulate, how the results are organised, and how to relate a row to the scenario
that produced it.

## Simulating

``` python
results = opendc.simulate(experiment, traces={}, input_root=".", parallelism=1)
```

`simulate` takes an experiment or a single scenario, as a spec or as a [builder](specs.md#builders). It has three
optional arguments:

| Argument      | Default | Description                                                                              |
|---------------|---------|------------------------------------------------------------------------------------------|
| `traces`      | none    | The [traces](traces.md) that the experiment references, by name or URI.                  |
| `input_root`  | `"."`   | The directory against which the SDK resolves references that are missing from `traces`.  |
| `parallelism` | `1`     | The number of runs that are simulated at the same time. Each needs the memory of its own simulation. |

Before it simulates, `simulate` validates the experiment. An invalid experiment raises a `ValueError` that lists every
issue:

``` text
ValueError: The spec is invalid:
  topologies: must not be empty
  workloads: must not be empty
```

The call returns when every run has finished. The SDK writes the traces and the output of the runs to a temporary
directory, under `TMPDIR`, and removes the directory before it returns. The results therefore live in memory only. A
program that needs them later saves the DataFrames itself, for example with `DataFrame.to_parquet`.

## Tables

The results map the name of each [output file](../reference/output/index.md) to one DataFrame that holds the rows of
all runs. A table is an item or an attribute of the results, and attributes are written in snake case:

``` python
list(results)             # ['battery', 'cluster', 'dataCenter', 'host', 'meta', 'powerSource', 'service', 'task']
results["powerSource"]    # a DataFrame
results.power_source      # the same DataFrame
```

The columns of each table are the columns of its file, which the [output reference](../reference/output/index.md)
lists. Two columns come first in every table: `scenario`, the
[number of the scenario](../reference/experiment/index.md#scenarios), and `seed`, the seed of the run.

``` python
results.power_source.head(3)
```

``` text
   scenario  seed  timestamp  power_source_id  power_draw  energy_usage  carbon_intensity  carbon_emission
0         0     0     300000                0       862.5      258750.0               0.0              0.0
1         0     0     600000                0       862.5      517500.0               0.0              0.0
2         0     0     900000                0       925.0      795000.0               0.0              0.0
```

Runs can differ in their columns. For example, the host table of a topology with GPUs has columns that the table of a
topology without GPUs lacks. Such runs share one DataFrame, in which the cells without a value are empty.

The export model of an experiment applies as it does to files: a table that `filesToExport` leaves out is absent from
the results.

## Meta tables

`results.meta` holds the [meta files](../reference/output/meta.md) in the same form. They map the identifiers in the
tables to names and fixed properties:

``` python
results.meta.host.head(3)
```

``` text
   scenario  seed  host_id host_name  cluster_id  core_count  cpu_capacity  mem_capacity
0         0     0        0      Host           0          16       38400.0         65536
1         0     0        1    Host-0           0          16       38400.0         65536
2         0     0        2    Host-1           0          16       38400.0         65536
```

Identifiers are meaningful within one run only, so a join of a table with its meta table uses `scenario` and `seed` as
well as the identifier:

``` python
host = results.host.merge(results.meta.host, on=["scenario", "seed", "host_id"])
```

Times in the tables are in milliseconds since the start of the simulation. `results.meta.simulation` holds the start
of each run in milliseconds since the Unix epoch, which converts them to absolute times.

## Scenarios

`results.scenarios` is a DataFrame with one row per run, indexed by `scenario` and `seed`. Its columns hold the specs
that the run simulated: `topology`, `workload`, `allocation_policy`, `export_model`, `failure_model`,
`checkpoint_model` and `max_num_failures`. It relates the rows of a table to what was simulated:

``` python
energy = results.power_source.groupby(["scenario", "seed"])["energy_usage"].last() / 3.6e6   # kWh per run

for (scenario, seed), kwh in energy.items():
    topology = results.scenarios.loc[(scenario, seed), "topology"]
    hosts = sum(host.count for dc in topology.datacenters for cluster in dc.clusters for host in cluster.hosts)
    print(f"{hosts} hosts: {kwh:.1f} kWh")
```

``` text
4 hosts: 6.8 kWh
16 hosts: 19.3 kWh
```

Energy and carbon emissions are totals since the start of the simulation, so the last sample of a run holds its
result. [Output](../reference/output/index.md#conventions) describes this and the other conventions of the tables.
