# opendc-sdk-model

A path-agnostic, serializable description of an OpenDC simulation.

The SDK model is the single source of truth for *what* to simulate: the datacenter
topology, the workload, the scheduler, and how failures, checkpointing, power, and
result export behave. It is a plain, immutable data model — no I/O, no engine, no file
paths baked in. The same value can be built in code, parsed from JSON, or loaded from a
database `jsonb` column, and it is validated the same way regardless of where it came
from.

## What it is

- **Path-agnostic.** External data (traces, carbon curves, failure traces) is never
  referenced by a hard-coded path. Instead the model holds a [`ResourceReference`]
  (`NamedReference` or `UriReference`) that a [`ResourceProvisioner`] turns into a local,
  seekable resource at run time. The same experiment can therefore move between a laptop, a
  test fixture, and a server without editing paths.
- **Serializable.** Every model type is a `@Serializable` `kotlinx.serialization` type.
  Sealed hierarchies carry a `type` discriminator, so polymorphic choices (which
  scheduler, which failure model, which workload) round-trip through JSON with no
  manual serializer wiring.
- **Self-validating.** Models implement [`Validatable`] and return a list of
  [`ValidationIssue`]s with dotted field paths, rather than throwing. Validation is
  recursive: validating an `Experiment` validates every topology, workload, policy, and
  export nested inside it.

## Three ways to build it

The model is one shape with three front doors. All three produce the same `Experiment` /
`Scenario` values.

1. **Programmatic DSL** — type-safe Kotlin builders in `org.opendc.sdk.model.dsl`
   (`experiment { }`, `topology { }`, `inlineWorkload { }`, `filterScheduler { }`, plus
   unit extensions such as `3.2.ghz`, `128.gib`, `400.watts`, `30.minutes`). Best for
   tests and code-first configuration.
2. **JSON** — parse and emit through [`SdkJson`] (`decodeExperiment`, `decodeScenario`,
   `encodeToString`). The parser tolerates unknown keys and emits defaults. Best for
   config files and CLI input.
3. **Database `jsonb`** — `SdkJson.toJsonElement` / `SdkJson.fromJsonElement` convert an
   `Experiment` to and from a `JsonElement` tree suitable for storing in a Postgres
   `jsonb` column. Best for a hosted service that persists experiments.

## Core concepts

- **Experiment vs. Scenario.** An [`Experiment`] is a *design of experiments*: each axis
  (topologies, workloads, allocation policies, failure models, …) holds a **set** of
  candidate values. Calling `Experiment.expand()` takes the cartesian product of the axes
  and yields the concrete [`Scenario`]s. A `Scenario` is a single, fully-resolved
  configuration — exactly one topology, one workload, one policy — ready to run.
- **Topology.** A [`Topology`] is simply its list of `Cluster`s — hosts, CPUs/GPUs,
  memory, power models, power sources, and batteries defined directly in the model. A
  topology is always concrete; there is no reference/inline split.
- **Workload: trace vs. inline.** A [`Workload`] is either a `TraceWorkload` (a
  `ResourceReference` to trace data, with sampling and scaling options) or an
  `InlineWorkload` (a list of `Task`s, each with its `TaskFragment` execution profile).
- **ResourceReference.** The indirection that keeps the model path-agnostic. Bulk external
  data — workload traces, carbon-intensity curves, failure traces — is a `NamedReference("…")`
  or `UriReference("…")` provisioned by a `ResourceProvisioner` you supply at run time.
  Topologies are small structured config and are always inlined, never referenced.
- **Validation.** Call `.validate()` on any `Validatable` (an `Experiment`, a `Scenario`,
  or any nested part) to get back a `List<ValidationIssue>`; an empty list means valid.
  Issues carry a path like `topologies[0].clusters[0].hosts` so problems are easy to
  locate.

## DSL example

```kotlin
import org.opendc.sdk.model.dsl.*
import org.opendc.sdk.model.scheduler.SchedulerName

val exp =
    experiment {
        name = "mem-vs-filter"
        runs = 8

        topology {
            cluster(name = "core", count = 2) {
                host(count = 4) {
                    cpu(coreCount = 32, coreSpeed = 3.2.ghz)
                    memory(size = 128.gib)
                    power {
                        maxPower = 400.watts
                        idlePower = 120.watts
                    }
                }
                powerSource(maxPower = 10.kwatts)
            }
        }

        workload {
            task(
                id = 0,
                name = "job-0",
                submissionTime = 0.minutes,
                duration = 30.minutes,
                cpuCoreCount = 4,
                cpuCapacity = 2.4.ghz,
                memory = 8.gib,
            ) {
                fragment(duration = 30.minutes, cpuUsage = 2.4.ghz)
            }
        }

        // Two allocation-policy candidates -> the experiment expands to two scenarios.
        allocationPolicy(prefabScheduler(SchedulerName.Mem))
        allocationPolicy(filterScheduler { subsetSize = 2 })
    }

val scenarios = exp.expand()
require(exp.validate().isEmpty())
```

A trace-backed workload uses a `ResourceReference` instead of inline tasks:

```kotlin
import org.opendc.sdk.model.resource.NamedReference

workload(traceWorkload(source = NamedReference("bitbrains"), sampleFraction = 0.5))
```

## JSON example

```json
{
  "name": "mem-vs-filter",
  "runs": 8,
  "topologies": [
    {
      "clusters": [
        {
          "name": "core",
          "count": 2,
          "hosts": [
            {
              "count": 4,
              "cpu": { "coreCount": 32, "coreSpeed": "3.2 GHz" },
              "memory": { "size": "128 GiB" },
              "cpuPowerModel": { "type": "linear", "maxPower": "400 Watts", "idlePower": "120 Watts" }
            }
          ],
          "powerSource": { "maxPower": "10 KWatts" }
        }
      ]
    }
  ],
  "workloads": [
    {
      "type": "trace",
      "source": { "type": "named", "name": "bitbrains" },
      "sampleFraction": 0.5
    }
  ],
  "allocationPolicies": [
    { "type": "prefab", "scheduler": "Mem" },
    { "type": "filter", "subsetSize": 2 }
  ]
}
```

Parse, expand, and validate it:

```kotlin
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.serialization.SdkJson

val experiment = SdkJson.decodeExperiment(json)
val scenarios = experiment.expand()
val issues = experiment.validate()
```

Units are written as strings (`"3.2 GHz"`, `"128 GiB"`, `"400 Watts"`, `"30 mins"`);
polymorphic types carry a `"type"` discriminator matching the `@SerialName` of the
subtype. Unspecified fields fall back to their model defaults.

Loading a topology (or any fragment) from a separate file is deliberately **not** a model
concern — the in-memory model is always concrete. Such imports can be layered onto
[`SdkJson`] as a serialization-time step (an `$import`/`$ref`-style directive resolved
through a [`ResourceProvisioner`]), without changing any data class.

[`ResourceReference`]: src/main/kotlin/org/opendc/sdk/model/resource/ResourceReference.kt
[`ResourceProvisioner`]: src/main/kotlin/org/opendc/sdk/model/resource/ResourceProvisioner.kt
[`Validatable`]: src/main/kotlin/org/opendc/sdk/model/validation/Validation.kt
[`ValidationIssue`]: src/main/kotlin/org/opendc/sdk/model/validation/Validation.kt
[`SdkJson`]: src/main/kotlin/org/opendc/sdk/model/serialization/SdkJson.kt
[`Experiment`]: src/main/kotlin/org/opendc/sdk/model/experiment/Experiment.kt
[`Scenario`]: src/main/kotlin/org/opendc/sdk/model/experiment/Scenario.kt
[`Topology`]: src/main/kotlin/org/opendc/sdk/model/topology/Topology.kt
[`Workload`]: src/main/kotlin/org/opendc/sdk/model/workload/Workload.kt
