---
description: The modules of OpenDC, how they depend on each other, and how an experiment becomes results.
---

# Architecture

OpenDC is built in three layers. The *simulator* models a datacenter and advances it through simulated time. The
*SDK* describes experiments and runs them on the simulator. The *entry points*, which are the command, the Python SDK
and the web application, make the SDK available to users. This page describes the modules of each layer and follows an
experiment through them.

## Modules

Figure 1 shows the modules and their dependencies. Every entry point reaches the simulator through the runner of the
SDK, so all of them simulate in the same way.

``` mermaid
flowchart TB
    subgraph entry["Entry points"]
        cli["opendc-cli"]
        python["opendc-sdk-python"]
        server["opendc-web-server"]
        dispatcher["opendc-web-dispatcher"]
        launcher["opendc-web-launcher"]
    end
    subgraph sdk["SDK"]
        runner["opendc-sdk-runner"]
        model["opendc-sdk-model"]
    end
    subgraph simulator["Simulator"]
        failure["opendc-simulator-failure"]
        compute["opendc-simulator-compute"]
        flow["opendc-simulator-flow"]
        core["opendc-simulator-core"]
    end
    server --> dispatcher --> launcher
    cli --> runner
    python --> runner
    launcher --> runner
    server --> model
    runner --> model
    runner --> failure
    model --> compute
    failure --> compute --> flow --> core
```

/// figure-caption
The modules of OpenDC and their dependencies. The figure omits `opendc-common` and `opendc-trace`, which most modules
use.
///

| Module                      | Role                                                                                           |
|-----------------------------|------------------------------------------------------------------------------------------------|
| `opendc-common`             | units of measurement, such as `Frequency` and `DataSize`, and logging                          |
| `opendc-trace-api`, `opendc-trace-parquet` | the tables and columns of traces, and their Parquet readers and writers         |
| `opendc-simulator-core`     | the event engine: a dispatcher with a virtual clock                                            |
| `opendc-simulator-flow`     | the flow network, which distributes a supply over competing demands                            |
| `opendc-simulator-compute`  | the model of a datacenter: hosts, tasks, schedulers, power, batteries and carbon               |
| `opendc-simulator-failure`  | failure models, which take hosts down during a simulation                                      |
| `opendc-sdk-model`          | the [model](../kotlin-sdk/model.md) of an experiment, and the loaders that read traces         |
| `opendc-sdk-runner`         | the [runner](../kotlin-sdk/runner.md), which simulates a model and exports the results         |
| `opendc-sdk-python`         | the [Python SDK](../python-sdk/internals.md), with its bridge to Kotlin                        |
| `opendc-cli`                | the [`opendc`](../reference/cli.md) command                                                    |
| `opendc-web-server`         | the REST server of the [web application](../web/index.md)                                      |
| `opendc-web-dispatcher`     | starts executions on the local machine, on Kubernetes or on a SLURM cluster                    |
| `opendc-web-launcher`       | the program that simulates one execution and reports its progress                              |
| `opendc-web-frontend`       | the user interface in the browser, which uses the REST server                                  |

The simulator is written in Java and Kotlin, the SDK, the command and the web server in Kotlin, and the frontend in
TypeScript.

## The simulator

OpenDC is a *discrete-event simulator*: it does not advance time in fixed steps, but jumps from one event to the next.
Its four modules build on each other.

Event engine
:   `opendc-simulator-core` provides a dispatcher with a virtual clock. Code schedules work for a future moment, and
    the dispatcher runs the scheduled work in order of time, skipping the time in between. A simulation runs on a
    single thread, which makes it deterministic: the same scenario with the same seed gives the same results.

Flow network
:   `opendc-simulator-flow` models resources as a network. A node demands a resource, such as CPU capacity or power,
    from the node that supplies it, and a distributor divides a supply over several demands according to a
    [distribution policy](../reference/experiment/topology/distribution-policy.md). The engine schedules the updates
    of all nodes centrally, so that a change propagates through the network efficiently.

Compute model
:   `opendc-simulator-compute` models the datacenter on top of the flow network. A task demands CPU capacity from its
    host, and the host demands power from its power source, possibly through a battery. The module also contains the
    compute service, which accepts tasks and places them on hosts with a scheduler, the
    [power models](../reference/experiment/topology/power-model.md), and the readers from which results are sampled.

Failure injection
:   `opendc-simulator-failure` takes hosts down and brings them back according to a
    [failure model](../reference/experiment/failure-model.md).

## From an experiment to results

The following steps show how the layers work together when the `opendc` command runs an experiment:

1. The command reads the experiment file, resolves its imports and decodes the JSON into the model with `SdkJson`.
2. The model validates itself. `expand` turns the experiment into one scenario per run.
3. The runner simulates the scenarios, several at once if asked. Each run has its own event engine and shares nothing
   with other runs.
4. For a run, the runner asks its provisioner for the trace files and loads the workload. It then builds the objects
   of the simulator from the model: the compute service with its scheduler, the hosts with their power models, the
   power sources, the batteries and the failure model.
5. The runner replays the workload: it submits each task at its submission time, and the simulator runs until every
   task has ended.
6. At every export interval, the runner samples the state of the simulation and passes the samples to its sinks. The
   Parquet sink writes the [output files](../reference/output/index.md).

The other entry points differ in the first and the last step only. The Python SDK builds the model in Python and reads
the Parquet files into DataFrames. The web application stores the model in a database and runs steps 3 to 6 in a
launcher process, as [How Experiments Execute](../web/execution.md) describes.

## Build logic

The modules share their build configuration through *convention plugins* in `buildSrc/src/main/kotlin`. A module
applies the conventions that fit its kind:

| Convention plugin            | Applies to                 | Configures                                                         |
|------------------------------|----------------------------|--------------------------------------------------------------------|
| `kotlin-conventions`         | applications               | the Kotlin compiler, Java&nbsp;21 and the code style               |
| `kotlin-library-conventions` | published libraries        | the same, plus explicit visibility, API documentation and publishing |
| `testing-conventions`        | modules with tests         | JUnit&nbsp;5 and the logging of tests                              |
| `quarkus-conventions`        | the web server             | Quarkus                                                            |
| `benchmark-conventions`      | modules with benchmarks    | the Java Microbenchmark Harness                                    |

The versions of all dependencies are in the version catalogue `gradle/libs.versions.toml`.

In summary, the simulator knows datacenters but not experiments, the SDK turns experiments into simulations, and each
entry point is a thin layer over the SDK. A change to the behaviour of a simulation therefore belongs in the simulator
or the SDK, where every entry point picks it up.
