# opendc-sdk-runner

The execution engine for the OpenDC SDK. It takes a simulation described with the pure data model
in [`opendc-sdk-model`](../opendc-sdk-model) and runs it on the OpenDC discrete-event simulator,
returning strongly-typed results and/or writing them to disk.

`opendc-sdk-runner` is the single, canonical way to drive the simulator from code. It is consumed by
the CLI, by the web platform, and by anyone embedding OpenDC as a library.

## The one entry point: `OpenDC`

Configure an `OpenDC` instance with a builder, then hand it an `Experiment` (a design of experiments
that expands into scenarios) or a single `Scenario`:

```kotlin
val report =
    OpenDC.builder()
        .provisioner(FileSystemResourceProvisioner(inputRoot))  // resolve trace references
        .output(outputRoot)                                     // write parquet
        .sink(InMemorySink())                                   // also capture metrics in memory
        .parallelism(8)                                         // runs to execute concurrently
        .build()
        .simulate(experiment)

val energy = report.runs.first().metrics!!.powerSource.sumOf { it.energyUsage }
```

`simulate` expands the experiment's cartesian axes into `Scenario`s, runs each repetition on its own
simulated clock (across an OS-thread pool, since a run blocks rather than suspends), and returns a
`SimulationReport` — one `ScenarioResult` per scenario, each holding one `RunResult` per repetition.

## Resolving external data: `ResourceProvisioner`

The SDK model never embeds filesystem paths; it *denotes* traces with a `ResourceReference`
(`NamedReference` or `UriReference`). A `ResourceProvisioner` — supplied through the builder — is the
single place that turns a reference into a local, readable file. `FileSystemResourceProvisioner`
resolves names under a root directory and `file:` URIs; provide your own for a database or object
store. Three references are resolved per run: the workload trace, the failure trace, and the carbon
trace on a power source.

## Modelling output: the sink pattern

Output is modelled as composable **sinks**. A sink is a factory that opens a per-run session yielding
a `ComputeMonitor`; the runner fans every metric record out to all sinks, so they compose freely —
parquet, in-memory capture, and streaming can all observe the same run.

| Sink | Purpose |
|------|---------|
| `ParquetSink` | Writes the canonical layout `<root>/<experiment>/raw-output/<id>/seed=<seed>/{host,task,powerSource,battery,service}.parquet`. Added by `builder().output(root)`. |
| `InMemorySink` | Captures selected tables as strongly-typed `HostSample`/`TaskSample`/`ServiceSample`/`PowerSourceSample`/`BatterySample`, available on `RunResult.metrics`. |
| `CallbackSink` | Streams each snapshot to per-table callbacks without retaining it — for progress or large sweeps. |
| `MonitorSink` | Feeds a caller-supplied `ComputeMonitor` — the full-control escape hatch. |

`RunResult` exposes convenience accessors: `outputPath` (the parquet directory) and `metrics` (the
in-memory `CollectedMetrics`), alongside the raw `results: List<SinkResult>`.

## How it works

The runner owns the entire SDK-model → engine translation and never depends on the deprecated
`opendc-experiments-base`:

- **Topology** → `List<ClusterSpec>` (CPU/GPU/memory models, power models, distribution policies,
  virtualization overhead, power source, battery), fed to the reused `setupHosts` provisioning step.
- **Workload** → `List<ServiceTask>` (trace workloads via `ComputeWorkloadLoader`; inline workloads
  built directly).
- **AllocationPolicy** → `ComputeScheduler` (prefab, filter, or time-shift, with filters/weighers).
- **FailureModel** → the engine failure model injected during replay.
- **ExportModel** → the parquet column configuration and export cadence.

Each run wires up a `Provisioner`, compute service, hosts, monitors and carbon receivers, then
replays the workload on the virtual clock to completion.

## Building and testing

```bash
./gradlew :opendc-sdk:opendc-sdk-runner:test          # unit + ported base suite + demo integration
./gradlew :opendc-sdk:opendc-sdk-runner:spotlessApply # license headers + ktlint
./gradlew :opendc-sdk:opendc-sdk-runner:build
```

The test suite ports the `opendc-experiments-base` validation suite one-to-one (identical assertion
values, inputs recreated in SDK-model form) and runs the `opendc-demos` experiments end-to-end.
