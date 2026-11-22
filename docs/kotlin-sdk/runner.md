---
description: The runner of the Kotlin SDK, which simulates experiments and delivers their results to files or to the program.
---

# The Runner

The library `opendc-sdk-runner` simulates what a [model](model.md) describes. Its entry point is the class `OpenDC`.
This page explains how to configure it, how it finds traces, where the results go, and what a simulation returns.

## Simulating

A program configures an `OpenDC` instance with a builder and then calls `simulate`:

``` kotlin
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.sink.InMemorySink
import java.nio.file.Path

val openDc =
    OpenDC.builder()
        .provisioner(FileSystemResourceProvisioner(Path.of("."))) // resolves trace references
        .output(Path.of("output"))                                // writes Parquet files
        .sink(InMemorySink())                                     // keeps samples in memory
        .parallelism(4)                                           // simulates four runs at once
        .build()

val report = openDc.simulate(experiment)
```

The builder has four options:

| Option                        | Required | Description                                                                          |
|-------------------------------|----------|--------------------------------------------------------------------------------------|
| `provisioner(provisioner)`    | yes      | The [provisioner](#resolving-trace-references) that resolves trace references.       |
| `output(root, writerThreads)` | no       | Writes the [output files](../reference/output/index.md) of every run under `root`.   |
| `sink(sink)`                  | no       | Adds a [sink](#sinks) that receives the samples of every run.                        |
| `parallelism(n)`              | no       | The number of runs that are simulated at the same time. The default is 1.            |

`simulate` accepts three kinds of input:

- `simulate(experiment)` expands an `ExperimentSpec` and simulates every run of it.
- `simulate(scenario)` simulates a single `ScenarioSpec`.
- `simulate(experimentName, scenarios)` simulates a chosen list of scenarios, which suits a program that distributes
  the runs of one experiment over several machines.

The call blocks until every run has finished. Each run has its own simulated clock and shares no state with other
runs, so runs execute in parallel on a pool of threads. Every run in progress needs the memory of its own simulation,
which bounds a useful value of `parallelism`.

## Resolving trace references

A model [refers to traces](model.md#trace-references) without stating where they are. A `ResourceProvisioner` turns a
reference into a local file or directory when a run needs it. The runner resolves up to three references per run: the
workload trace, the failure trace and the carbon trace of each power source.

`FileSystemResourceProvisioner(root)` covers the common case:

- a `NamedReference` is a path relative to `root`;
- a `UriReference` with the scheme `file` is the file that the URI names;
- a `UriReference` without a scheme is a path relative to `root`;
- any other scheme is an error.

A program that keeps traces elsewhere supplies its own provisioner. `ResourceProvisioner` has a single function,
which returns a `ProvisionedResource`: a path, and a `close` function that the runner calls when the run no longer
needs the trace.

``` kotlin
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.ProvisionedResource
import org.opendc.sdk.model.resource.ResourceProvisioner
import org.opendc.sdk.model.resource.UriReference

val traces = Path.of("/data/traces")

val provisioner =
    ResourceProvisioner { reference ->
        when (reference) {
            is NamedReference ->
                object : ProvisionedResource {
                    override val path: Path = traces.resolve(reference.name)

                    override fun close() {}
                }
            is UriReference -> throw UnsupportedOperationException("no URIs: ${reference.uri}")
        }
    }
```

The path has to be a real file or directory, because the Parquet reader needs random access. A provisioner that
fetches a trace from a remote store downloads it to a temporary location and deletes it in `close`.

## Sinks

The runner models output as *sinks*. A sink receives every sample of a run as a typed object, as soon as the simulator
produces it. Several sinks can observe the same run, so one simulation can write files, keep samples in memory and
report progress at once. The runner opens a separate session of each sink for every run, so parallel runs never share
state.

| Sink           | Purpose                                                                                           |
|----------------|---------------------------------------------------------------------------------------------------|
| `ParquetSink`  | Writes the [output files](../reference/output/index.md). `output(root)` of the builder adds it.   |
| `InMemorySink` | Keeps the samples of chosen tables in memory, and returns them with the result of the run.        |
| `CallbackSink` | Passes each sample to a function and keeps nothing, which suits progress reports and long runs.   |
| `MonitorSink`  | Passes the samples to a `MetricExporter` that the program implements, for full control.           |

`InMemorySink` takes the set of tables to capture, and `CallbackSink` receives only the tables for which it has a
function. Capturing only what a program reads saves memory:

``` kotlin
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.sink.CallbackSink

val openDc =
    OpenDC.builder()
        .provisioner(FileSystemResourceProvisioner(Path.of(".")))
        .sink(InMemorySink(setOf(OutputFileSpec.HOST, OutputFileSpec.POWER_SOURCE)))
        .sink(CallbackSink(onService = { sample -> println("${sample.tasksCompleted} tasks completed") }))
        .build()
```

Without `output` and without a `ParquetSink`, a simulation writes no files at all.

The sample classes correspond to the output files: `HostSample` to [`host.parquet`](../reference/output/host.md),
`TaskSample` to `task.parquet`, `ServiceSample` to `service.parquet`, and so on. Their properties are the columns of
the files in camel case, such as `powerDraw` for `power_draw`. The export model of a scenario sets the interval between
samples for every sink, and the selection of files and columns for the Parquet files.

## Results

`simulate` returns a `SimulationReport`. Its structure follows the structure of what was simulated:

| Class              | Properties                                                                                          |
|--------------------|-----------------------------------------------------------------------------------------------------|
| `SimulationReport` | `scenarios`, the results of each simulated `ScenarioSpec`, and `runs`, all runs in one list         |
| `ScenarioResult`   | `scenario`, the `ScenarioSpec` that was simulated, and `runs`                                       |
| `RunResult`        | `seed`, `outputPath`, `metrics`, and `results` with the raw result of every sink                    |

`outputPath` is the folder with the Parquet files of the run, or `null` without a `ParquetSink`. `metrics` is what an
`InMemorySink` captured, or `null` without one. It is a `CollectedMetrics` value:

``` kotlin
val run = report.runs.first()
val metrics = run.metrics!!

val energy = metrics.powerSource.last().energyUsage          // joules, total since the start
val peak = metrics.host.maxOf { it.cpuUtilization }          // highest utilisation of any host
val names = metrics.topology!!.hosts.associate { it.hostId to it.hostName }
```

`CollectedMetrics` has one list of samples per table: `host`, `task`, `service`, `powerSource` and `battery`. It also
holds the content of the [meta files](../reference/output/meta.md): `topology` maps identifiers to names and
properties, `taskMeta` holds the properties of each task, and `simulation` the start time.

## Inside a run

For each run, the runner translates the model into the objects of the simulator: the topology into hosts with their
power models, the workload into tasks, the allocation policy into a scheduler, and the failure model into a failure
injector. It then replays the workload on a virtual clock until every task has ended, while the sinks receive samples
at the export interval. [Architecture](../development/architecture.md) describes the modules below the runner.

In summary, a program gives the runner a provisioner for traces and any number of sinks for results, and receives a
report with one result per run.
