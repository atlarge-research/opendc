---
description: The model of the Kotlin SDK, which describes experiments as immutable, serialisable and self-validating data.
---

# The Model

The library `opendc-sdk-model` describes what to simulate. This page covers its design, the three ways to obtain a
model, and the operations that every model supports: expansion into scenarios, validation and the handling of trace
references.

## Design

The model is a set of immutable Kotlin data classes whose names end in `Spec`, such as `ExperimentSpec`, `HostSpec`
and `TraceWorkloadSpec`. Each class corresponds to one object of the
[experiment document](../reference/experiment/index.md), and each property to one field. The reference pages of the
document therefore also document the classes. Three properties follow from this design:

Path-agnostic
:   A model contains no file paths. It refers to a trace with a [reference](#trace-references), which the runner
    resolves when it simulates. The same model therefore works on a laptop, in a test and on a server.

Serialisable
:   Every class has a JSON form. Where the document offers alternatives, such as the kinds of workload, the classes form
    a sealed hierarchy, and the JSON field `type` selects the class.

Self-validating
:   A model checks its own constraints and reports problems as a list, without throwing. Validating an experiment
    validates everything inside it.

## Obtaining a model

### From builders

The package `org.opendc.sdk.model.dsl` has a builder for every part of an experiment. The builders nest in the way the
document nests:

``` kotlin
import org.opendc.sdk.model.dsl.*
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.topology.SingleBatteryPolicySpec

val design =
    topology {
        datacenter(name = "DC1") {
            powerSource(carbon = NamedReference("carbon_traces/NL_2021-2024.parquet"))
            battery(
                capacity = 100.kwh,
                chargingSpeed = 5.kwatts,
                policy = SingleBatteryPolicySpec(carbonThreshold = 150.0),
            )
            cluster(count = 2, name = "C01") {
                host(count = 8) {
                    cpu(coreCount = 32, coreSpeed = 2.1.ghz)
                    memory(size = 256.gib)
                }
            }
        }
    }
```

`experiment { }` collects candidates. Each call of `topology`, `workload`, `allocationPolicy`, `failureModel`,
`checkpointModel`, `exportModel` or `maxNumFailures` adds one candidate to the corresponding list of the document.
A part that is never set takes the default of the document.

``` kotlin
import org.opendc.sdk.model.checkpoint.CheckpointModelSpec
import org.opendc.sdk.model.failure.FailurePrefabSpec
import org.opendc.sdk.model.failure.PrefabFailureSpec
import org.opendc.sdk.model.scheduler.*

val experiment =
    experiment {
        name = "sweep"
        runs = 4

        topology(design)
        workload(traceWorkload(source = NamedReference("traces/bitbrains-small"), sampleFraction = 0.5))

        allocationPolicy(prefabScheduler())
        allocationPolicy(
            filterScheduler {
                filter(ComputeHostFilterSpec)
                filter(VCpuFilterSpec(allocationRatio = 2.0))
                filter(RamFilterSpec())
                weigher(RamWeigherSpec(multiplier = -1.0))
            },
        )

        failureModel(PrefabFailureSpec(FailurePrefabSpec.G5k06Exp))
        checkpointModel(null)
        checkpointModel(CheckpointModelSpec(interval = 1.hours))
    }
```

This experiment has one topology, one workload, two allocation policies, one failure model and two checkpoint models,
one of which is no checkpointing. It therefore has four scenarios and, with four runs each, sixteen runs.

### From JSON

`SdkJson` converts between models and JSON. It reads the same documents as the `opendc` command:

``` kotlin
import org.opendc.sdk.model.serialization.SdkJson
import java.io.File

val experiment = SdkJson.decodeExperiment(File("experiment.json").readText())
val text = SdkJson.encodeToString(experiment)
```

Decoding ignores unknown keys, and encoding writes every field, defaults included. `SdkJson.fromJsonElement(element,
strict = true)` rejects unknown keys instead. `SdkJson.toJsonElement` and `fromJsonElement` convert to and from a JSON
tree, which suits a database column of a JSON type.

`SdkJson` does not resolve [`importFrom`](../reference/experiment/index.md#splitting-an-experiment-over-several-files).
Imports are a feature of the `opendc` command, which resolves them before it hands the document to `SdkJson`.

### From constructors

The data classes have public constructors with defaults, so a program can also construct and change models directly.
`copy` derives a variant of a model:

``` kotlin
val longer = experiment.copy(name = "sweep-more", runs = 8)
```

## Experiments and scenarios

An `ExperimentSpec` holds a set of candidates for each of its parts. `expand()` turns it into the list of simulations
that it stands for, as `ScenarioSpec` values. A `ScenarioSpec` is fully resolved: it has exactly one topology, one
workload, one allocation policy, one of each model and one seed.

``` kotlin
import org.opendc.sdk.model.experiment.expand

val scenarios = experiment.expand()
println("${scenarios.size} runs, ${scenarios.distinctBy { it.id }.size} scenarios")   // 16 runs, 4 scenarios
```

`expand()` returns one `ScenarioSpec` per run. The runs of the same scenario share their `id`, which is the
[number of the scenario](../reference/experiment/index.md#scenarios), and differ in their `seed`.

A program that needs a single simulation builds a `ScenarioSpec` directly, with `scenario { }`:

``` kotlin
val single =
    scenario {
        topology(design)
        workload {
            task(id = 0, submissionTime = 0.minutes, duration = 5.minutes, cpuCoreCount = 1, cpuCapacity = 2.ghz, memory = 1.gib) {
                fragment(duration = 5.minutes, cpuUsage = 1.ghz)
            }
        }
        seed = 7
        experimentName = "single"
    }
```

## Validation

Every model implements `Validatable`. `validate()` returns a list of `ValidationIssue` values, each with the `path` of
the field it concerns and a `message`. An empty list means that the model is valid.

``` kotlin
for (issue in experiment.validate()) {
    println("${issue.path}: ${issue.message}")
}
```

``` text
runs: must be > 0
topologies[0].datacenters[0].clusters[0].hosts[0].cpu.coreCount: must be > 0
```

A path names the fields from the experiment down to the problem, with the index of each list entry. The `opendc`
command and the web server report issues in the same notation. Validation checks the model only: it does not check
that a referenced trace exists, which the runner finds when it simulates.

## Trace references

Workload traces, carbon traces and failure traces are files outside the model. A model refers to them with a
`ResourceReference`, which has two forms:

| Class            | JSON form                              | Identifies a trace by                                               |
|------------------|----------------------------------------|---------------------------------------------------------------------|
| `NamedReference` | `{ "type": "named", "name": "..." }`   | a name, whose meaning the program that runs the experiment decides  |
| `UriReference`   | `{ "type": "uri", "uri": "..." }`      | a URI, whose scheme states where the trace is                       |

The model treats both as opaque. The [provisioner](runner.md#resolving-trace-references) of the runner gives them a
meaning, such as a path under a directory or an entry in a database.

Two functions help programs that manage traces. `references()` lists every reference of an experiment or a scenario,
with its role and the path of the field that holds it. `mapReferences` returns a copy in which a function has replaced
every reference:

``` kotlin
import org.opendc.sdk.model.resource.UriReference

for (use in experiment.references()) {
    println("${use.role} at ${use.path}: ${use.reference}")
}

val moved = experiment.mapReferences { use ->
    UriReference("file:///data/${(use.reference as NamedReference).name}")
}
```

``` text
WORKLOAD at workloads[0].source: NamedReference(name=traces/bitbrains-small)
CARBON at topologies[0].datacenters[0].powerSource.carbon.source: NamedReference(name=carbon_traces/NL_2021-2024.parquet)
```

The web server uses these functions to check that an experiment references only traces that its submitter may use, and
to replace each name by the location of the trace before a simulation starts.

## Units

Quantities in the model are typed: a core speed is a `Frequency`, a memory size a `DataSize`, a duration a
`TimeDelta`. The types are defined in `org.opendc.common.units`. The builders package adds extension properties that
construct them from numbers:

| Quantity    | Extension properties                    |
|-------------|-----------------------------------------|
| `Frequency` | `ghz`, `mhz`                            |
| `DataSize`  | `gib`, `mib`, `bytes`                   |
| `Power`     | `watts`, `kwatts`                       |
| `Energy`    | `kwh`                                   |
| `DataRate`  | `gbps`, `kibps`                         |
| `TimeDelta` | `ms`, `seconds`, `minutes`, `hours`     |

`gbps` is gigabytes per second, and `kibps` is kibibits per second. The types support arithmetic and conversion, such
as `30.minutes * 2` and `2.1.ghz.toMHz()`. Their companion objects offer further units, such as `Frequency.ofKHz` and
`DataSize.ofTiB`. In JSON, a quantity is a string with a unit, as [Units](../reference/units.md) describes.

In summary, the model is plain data that mirrors the experiment document. A program builds it with builders, JSON or
constructors, expands it into scenarios, validates it and inspects its trace references, all without input or output.
