---
description: The experiment file, which defines what OpenDC simulates.
---

# Experiment

An *experiment* tells OpenDC what to simulate: topologies, workloads, policies and models. It is written as a JSON
document, by convention in a file called `experiment.json`. This page describes the fields of an experiment, how an
experiment expands into scenarios, and how an experiment refers to other files.

The [`opendc`](../cli.md) command runs an experiment file directly:

``` bash
opendc run experiment.json -o output
```

The other [entry points](../../getting-started/entry-points.md) use the same document: the web application stores it,
and the SDKs build it from code.

## Fields

| Field                | Type                                                    | Default                            | Description                                                                 |
|----------------------|---------------------------------------------------------|------------------------------------|-----------------------------------------------------------------------------|
| `name`               | string                                                  | `""`                               | The name of the experiment. The results are written to `<output>/<name>/`.  |
| `topologies`         | list of [topologies](topology/index.md)                 | required                           | The datacenters to simulate.                                                |
| `workloads`          | list of [workloads](workload.md)                        | required                           | The tasks that the datacenters receive.                                     |
| `allocationPolicies` | list of [allocation policies](allocation-policy.md)     | the `Mem` prefab                   | The policies that decide on which host a task runs.                         |
| `failureModels`      | list of [failure models](failure-model.md)              | `[{ "type": "none" }]`             | The models that decide when hosts fail.                                     |
| `maxNumFailures`     | list of integers                                        | `[10]`                             | How often a task may fail before OpenDC terminates it.                      |
| `checkpointModels`   | list of [checkpoint models](checkpoint-model.md) or `null` | `[null]`                        | How tasks save their progress. `null` stands for no checkpointing.          |
| `exportModels`       | list of [export models](export-model.md)                | one export model with its defaults | Which results OpenDC writes, and how often.                                 |
| `runs`               | integer                                                 | `1`                                | How often each scenario is simulated, each time with another seed.          |
| `initialSeed`        | integer                                                 | `0`                                | The seed of the first run. Run *i* uses the seed `initialSeed` + *i*.       |

Many fields hold physical quantities, such as `"2.1 GHz"` or `"1 hours"`. [Units](../units.md) explains how to write
them.

By default, OpenDC ignores keys that it does not know, so a misspelt key silently takes its default value.
`opendc --strict` reports unknown keys as errors instead.

## Scenarios

Seven fields of an experiment are lists: `topologies`, `workloads`, `allocationPolicies`, `exportModels`,
`failureModels`, `checkpointModels` and `maxNumFailures`. OpenDC simulates every combination of their entries. Each
combination is a *scenario*, and each scenario is simulated `runs` times. An experiment with 2&nbsp;topologies,
1&nbsp;workload and 3&nbsp;allocation policies has 6&nbsp;scenarios. Identical entries in a list count once.

Scenarios are numbered from 0. The numbering goes through the lists in a fixed order, in which `topologies` changes
slowest and `maxNumFailures` fastest:

``` text
topologies > workloads > allocationPolicies > exportModels > failureModels > checkpointModels > maxNumFailures
```

With 2&nbsp;topologies and 3&nbsp;allocation policies, scenarios 0 to 2 use the first topology with each policy in
turn, and scenarios 3 to 5 the second topology. The number of a scenario is the name of its folder in the
[output](../output/index.md). Every run also stores the scenario it simulated as `meta/trackr.json`.

## Splitting an experiment over several files

An experiment does not have to be written in one file. Any JSON object in it, such as a topology, a cluster, a host or
a workload, can take its fields from another file with the key `importFrom`. This lets several experiments share a
topology or a workload:

``` json
"topologies": [
    { "importFrom": "topologies/small-datacenter.json" }
]
```

Keys written next to `importFrom` override the keys of the imported file. An override replaces a key as a whole:
OpenDC does not merge nested objects. The following host takes everything from a file except its count:

``` json
{ "importFrom": "hosts/big-host.json", "count": 64 }
```

An imported file can import further files. Relative paths are resolved as follows:

- A path in the experiment file is relative to the directory from which `opendc` is started, or to the directory given
  with `opendc --input-root`.
- A path in an imported file is relative to the directory of that file.

!!! warning "Start `opendc` from the directory that the paths assume"

    The paths in an experiment file are not relative to the experiment file. `opendc run studies/big/experiment.json`
    looks for `topologies/small-datacenter.json` in the current directory, not in `studies/big/`. Either start `opendc`
    in `studies/big/`, or pass `--input-root studies/big`.

## Referencing trace files

Workload traces, carbon traces and failure traces are large Parquet files. An experiment does not contain them. It
refers to them with a *trace reference*, by name or by URI:

``` json
"source": { "type": "named", "name": "traces/bitbrains-small" }
"source": { "type": "uri", "uri": "file:///data/traces/bitbrains-small" }
```

What a reference resolves to depends on the entry point. With the `opendc` command, a name, or a URI without a scheme,
is a path relative to the directory from which `opendc` is started. `opendc --input-root <dir>` resolves them against
another directory. On a server, a name refers to a trace in the [trace library](../../web/using.md#traces), and URIs
are not accepted.

## Example

The following experiment has one topology, one workload and two allocation policies, and therefore two scenarios:

``` json
{
    "name": "simple",
    "topologies": [
        { "importFrom": "topologies/small-datacenter.json" }
    ],
    "workloads": [
        {
            "type": "trace",
            "source": { "type": "named", "name": "traces/bitbrains-small" }
        }
    ],
    "allocationPolicies": [
        { "type": "prefab", "prefabName": "Mem" },
        { "type": "prefab", "prefabName": "MemInv" }
    ],
    "exportModels": [
        { "exportInterval": "1 hours" }
    ]
}
```
