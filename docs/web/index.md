---
description: The OpenDC web module, which offers the simulator as a service with a browser interface.
---

# OpenDC as a Service

The web module offers OpenDC as a service. People design topologies and experiments in the browser, and a server
simulates them on its own machine, on a Kubernetes cluster or on a SLURM cluster. This page introduces the parts of the
web module and the terms the other pages of this section use.

## What the service adds

The [command-line interface](../reference/cli.md) simulates on the machine that runs it and writes the results to a
local folder. The service keeps experiments, traces and results on a server instead, which has three consequences:

- a team shares topologies, experiments and traces through *projects*;
- a long simulation runs away from the laptop that submitted it, on hardware sized for it;
- the browser charts the results while the simulation still runs.

The service accepts the same [experiment document](../reference/experiment/index.md) as the command-line interface. An
experiment can therefore move between the two without changes, apart from the traces it references, which the service
reads from its [trace library](using.md#traces).

## Parts of a deployment

A *deployment* is one installed instance of the service. Figure 1 shows its parts.

``` mermaid
flowchart LR
    browser["Browser"] -->|"pages and REST API"| server
    scripts["opendc run --api-url,<br>Python SDK"] -->|"REST API"| server
    subgraph deployment ["Deployment"]
        direction TB
        server["Server"] --- db[("Database")]
        server --- store[("Object store")]
        server --- telemetry[("Telemetry store")]
    end
    server -->|"starts"| launcher["Launcher"]
    launcher -->|"progress"| server
    launcher -->|"reads traces,<br>writes results"| store
```

/// figure-caption
The parts of a deployment. The server starts one launcher per execution, on the platform the deployment is configured for.
///

The *server* answers the REST API, serves the pages of the web application and decides when simulations run. It records
accounts, projects and experiments in a database, and keeps traces and results in an *object store*, which is a
directory or an S3-compatible bucket.

The server does not simulate. It hands simulations to a *platform*: its own machine, a Kubernetes cluster or a SLURM
cluster. A *dispatcher* is the part of the server that talks to one kind of platform. On the platform, a *launcher*
process reads what it has to simulate, simulates it and stores the results. While it runs, the launcher reports progress
and samples to the server, which keeps them in a *telemetry store* until the results arrive.

Four Gradle modules under `opendc-web/` implement these parts:

| Module                  | Role                                                                                           |
|-------------------------|------------------------------------------------------------------------------------------------|
| `opendc-web-server`     | The server: the REST API under `/api/v1`, the queue of simulations, and the web application's files. |
| `opendc-web-frontend`   | The web application, built as static files that the server serves.                             |
| `opendc-web-dispatcher` | The dispatchers for local processes, Kubernetes Jobs and SLURM jobs.                           |
| `opendc-web-launcher`   | The launcher.                                                                                  |

## Terms

The service uses the terms of the [experiment document](../getting-started/concepts.md) and adds a few of its own:

project
:   A shared space that holds topologies and experiments. Each member of a project has a role.

topology
:   A saved datacenter design. An experiment stores its own copy of each topology it uses.

experiment
:   An experiment document together with its state. An experiment is a *draft* until someone submits it, and frozen
    afterwards.

run
:   The simulation of one scenario with one seed. An experiment with 4&nbsp;scenarios and 2&nbsp;runs per scenario has
    8&nbsp;runs.

execution
:   One launcher process, which simulates one or more runs of the same experiment.

trace library
:   The workload, carbon and failure traces an account may use: the traces it uploaded, the traces others shared with
    it, and the traces built into the deployment.

## Where to go next

- [Using the Web Application](using.md) walks through the browser interface.
- [How Experiments Execute](execution.md) explains what happens between submitting an experiment and reading its results.
- [REST API](api.md) lists the endpoints for scripts.
- [Deployment](deployment.md), [Configuration](configuration.md) and [Operations](operations.md) are for the people who
  run a deployment.
