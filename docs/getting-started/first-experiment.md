---
description: How to write, run and inspect a first experiment with the opendc command.
---

# First Experiment

This page runs a small experiment from start to finish. We write an experiment file, simulate it with the `opendc`
command and read the results. The page assumes that `opendc` is [installed](installation.md).

## Describing an experiment

An experiment file is a JSON document that tells OpenDC what to simulate. Save the following as `experiment.json`, or
[download it](../examples/first-experiment.json).

``` json title="experiment.json"
--8<-- "examples/first-experiment.json"
```

The file has four parts, each of which a later page covers in depth:

`topologies`
:   The hardware to simulate. This [topology](../reference/experiment/topology/index.md) has one datacenter with one
    cluster of four identical hosts. Each host has 16&nbsp;cores and 64&nbsp;GiB of memory, and draws between
    32&nbsp;W when idle and 180&nbsp;W under full load.

`workloads`
:   The work that the datacenter receives. This [workload](../reference/experiment/workload.md) consists of two
    *tasks*, which correspond to virtual machines or jobs. Each task states when it is submitted, what it needs, and how
    much CPU capacity it uses over time. Larger experiments read their workload from a trace file instead.

`allocationPolicies`
:   The [policy](../reference/experiment/allocation-policy.md) that decides on which host each task runs. `Mem` places a
    task on the host with the most free memory.

`exportModels`
:   What the simulator writes. This [export model](../reference/experiment/export-model.md) records the state of the
    datacenter once per simulated minute.

## Running the experiment

The `run` command simulates the experiment and writes the results to the folder given with `-o`:

``` bash
opendc run experiment.json -o output
```

While it simulates, `opendc` shows a dashboard with the progress of the experiment. When it is done, it prints one line
per simulation with the number of tasks that completed, and the folder that holds the results. This experiment takes
a few seconds, because it simulates 25&nbsp;minutes of a small datacenter.

## Reading the results

OpenDC writes its results as [Apache Parquet](https://parquet.apache.org) files, a column-oriented format that data
analysis tools read directly. The results of this experiment are in `output/getting-started/raw-output/0/seed=0/`:

``` text
output/
  getting-started/            the name of the experiment
    raw-output/
      0/                      scenario 0
        seed=0/               the run with seed 0
          host.parquet
          task.parquet
          service.parquet
          powerSource.parquet
          ...
          meta/
```

Each file holds one kind of measurement. [Output](../reference/output/index.md) lists all files and their columns. As
an example, the following Python script reads the power that the datacenter draws. It needs the packages `pandas` and
`pyarrow`.

``` python
import pandas as pd

run = "output/getting-started/raw-output/0/seed=0"
power = pd.read_parquet(f"{run}/powerSource.parquet")
print(power[["timestamp", "power_draw", "energy_usage"]].tail(2))
```

``` text
    timestamp  power_draw  energy_usage
24    1500000       128.0      236400.0
25    1500000       128.0      236400.0
```

The last sample is taken at 1,500,000&nbsp;ms, which is 25&nbsp;minutes after the first task arrives. At that moment
the four idle hosts draw 128&nbsp;W together, and the datacenter has used 236,400&nbsp;J since the start, which is
about 66&nbsp;Wh.

## Checking a file before running it

Two further commands inspect an experiment file without simulating it. `validate` reports every problem in the file:

``` bash
opendc validate experiment.json
```

``` text
✓ experiment.json is valid
```

`show` prints the topologies of the file as tables, which helps to verify a design:

``` bash
opendc show experiment.json
```

``` text
Experiment getting-started: 1 topolog(ies), 1 workload(s), 1 policy(ies), 1 scenario(s)

Topology #0 — 4 host(s)
┌─────────┬──────┬───────┬────────────────────────────┬───────────────┬─────┐
│ Cluster │ Host │ Count │ CPU                        │ Memory        │ GPU │
├─────────┼──────┼───────┼────────────────────────────┼───────────────┼─────┤
│ C01     │ H01  │ 4     │ 1× 16 cores @ 2.100000 GHz │ 64.000000 GiB │ —   │
└─────────┴──────┴───────┴────────────────────────────┴───────────────┴─────┘
```

[Command Line](../reference/cli.md) documents all commands and options.

## Comparing alternatives

The strength of an experiment file is that most of its parts are lists. To compare two policies, we add a second entry
to `allocationPolicies`:

``` json
"allocationPolicies": [
    { "type": "prefab", "prefabName": "Mem" },
    { "type": "prefab", "prefabName": "ActiveServers" }
]
```

`opendc run` now simulates the workload twice, once with each policy, and writes the results of the second simulation
to the folder `raw-output/1/`. Each such combination is a *scenario*. [Concepts](concepts.md) explains scenarios and the
other terms of OpenDC.

In summary, an experiment file names topologies, workloads and policies, `opendc run` simulates every combination of
them, and the results are Parquet files with one folder per simulation.
