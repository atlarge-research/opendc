---
description: How the OpenDC server turns a submitted experiment into executions on a platform, and how it retries and settles them.
---

# How Experiments Execute

This page follows an experiment from the moment someone submits it to the moment its results are stored. It explains how
the server sizes and groups simulations, where they run, and what happens when one fails. Operators need this to size a
deployment, and developers to change the server.

## Overview

Submitting an experiment creates one *run* for each scenario and seed. The server groups waiting runs into *executions*.
An execution is one launcher process, which simulates its runs and stores their results. Figure 1 shows the steps.

``` mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant P as Platform
    participant L as Launcher
    participant O as Object store
    C->>S: submit experiment
    S->>S: check, estimate, queue runs
    loop every 2 s
        S->>S: pack waiting runs into an execution
        S->>O: write manifest
        S->>P: launch execution
    end
    P->>L: start
    L->>O: read manifest and traces
    L-->>S: progress and samples
    L->>O: write results and outcomes
    P-->>S: execution ended
    S->>O: read outcomes
    S->>S: settle, retry or finish runs
    C->>S: read status and results
```

/// figure-caption
The path of an experiment from submission to results.
///

Three properties of this design explain most of its behaviour:

- The platform decides whether an execution succeeded. The server asks the platform how a process ended and reads the
  outcome that the launcher stored for each run.
- Progress reported during a run is a preview that expires. Nothing that has to outlive a run is kept only in the
  telemetry store.
- A run that fails for lack of resources is not the fault of the experiment, so the server retries it with more
  resources and does not charge for the failed attempt.

## Submitting

A draft may be incomplete or invalid. Submitting is the point at which the server checks it, and it refuses an
experiment for any of these reasons:

- the document does not parse, or has a key the format does not know;
- the document has [validation issues](../kotlin-sdk/model.md#validation);
- a trace reference does not name a trace in the library of the submitter, or is a URI;
- a scenario needs more memory or time than the platform gives a single execution;
- the experiment has no scenarios, or more than 10,000&nbsp;runs;
- the [simulation budget](operations.md#plans-and-budgets) of the submitter does not cover the experiment.

The server reports all issues at once, each with the path of the field it concerns. `POST /experiments/preview` runs the
same checks on a document without storing it.

A submitted experiment is frozen. The server records the traces it references, so that nobody deletes or renames them
while the experiment depends on them.

## Estimating a run

The server estimates two numbers for each run before it starts: its peak memory and its CPU time. The estimate uses
what the server knows without simulating:

- the number of hosts in the topology;
- the number of tasks and fragments of the workload, which the trace library records for each trace;
- the sample fraction of the workload;
- the export interval, and whether the host file is exported;
- whether the scenario has a failure model or a checkpoint model.

The estimate is a linear model with coefficients under `opendc.execution.estimator.*`. It serves three purposes. It
sizes the execution that carries the run, it sets the time limit of that execution, and it is the amount of simulation
budget that the run reserves.

## Packing runs into executions

Every dispatcher states the shape of one execution, called its *slot*: a number of cores, an amount of memory and
possibly a time limit. The server fills a slot with as many runs of one experiment as fit. It sorts the waiting runs by
estimated memory and gives the execution one core per run. The memory request is a fixed amount for the Java virtual
machine (JVM) plus, for each run, the largest estimate among the runs of the execution.

An execution gets a time limit of three times the longest estimate of its runs, plus half a minute for start-up. The
factor and the allowance are settings under `opendc.execution.packing.*`.

The queue is first in, first out. Runs of different experiments never share an execution. When the platform has no room
for the next execution, later executions wait behind it.

## Dispatchers

The setting `opendc.execution.dispatcher` selects one of three dispatchers:

| Dispatcher   | An execution is                         | Live progress | Needs                                                    |
|--------------|-----------------------------------------|---------------|----------------------------------------------------------|
| `local`      | a process beside the server             | yes           | the launcher installed beside the server                 |
| `kubernetes` | a Kubernetes Job with one pod           | yes           | S3 storage, and a telemetry address that pods can reach  |
| `slurm`      | a SLURM batch job, submitted over SSH   | no            | a shared file system and Java&nbsp;21 on the cluster     |

The local dispatcher
:   starts the launcher as a child process of the server. By default, its slot is every core and half the memory of the
    machine. It starts an execution when the cores and memory of the running executions leave room for it, and stops a
    process that outlives its time limit. Executions do not survive a restart of the server: the server stops any it
    finds and queues their runs again.

The Kubernetes dispatcher
:   creates one Job per execution from the launcher image. The pod requests exactly the cores and memory of the
    execution, runs without access to the Kubernetes API and has a read-only root file system. The pod reads its inputs
    and writes its outputs through signed URLs, so it holds no credentials of its own. The dispatcher runs at most
    `max-concurrent` Jobs at once.

The SLURM dispatcher
:   assumes that compute nodes reach nothing but their shared file system. The server therefore copies the launcher, the
    traces and a rewritten manifest to the cluster over SFTP, submits the job with `sbatch`, and copies the results back
    after the job ends. For the same reason a SLURM execution reports no progress while it runs.

[Deployment](deployment.md) explains how to set up each platform, and [Configuration](configuration.md#dispatchers)
lists the settings.

## The launcher

The launcher is the program that an execution runs. Its only input is the environment variable `MANIFEST_URL`, which
names a *manifest*: a JSON document that lists the traces to fetch, the runs to simulate, where each output file of
each run goes, and where to report progress.

The launcher works in five steps:

1. It fetches the manifest and the traces.
2. It simulates its runs, as many at once as the execution has cores.
3. After each run, it uploads the output files of the run and then an *outcome* that states whether the run succeeded.
4. While runs simulate, it reports the number of finished tasks and the latest samples to the server every
   5&nbsp;seconds.
5. It deletes its working directory and exits.

The exit code tells the server how the process ended:

| Exit code | Meaning                                                            |
|-----------|--------------------------------------------------------------------|
| 0         | every run succeeded                                                |
| 20        | the manifest could not be read                                     |
| 21        | the launcher failed outside any run                                |
| 22        | the manifest, an input or an outcome could not be transferred      |
| 23        | every run stored its outcome, and at least one run failed          |
| 3         | the JVM ran out of memory                                          |

## Progress is a preview

While an execution runs, its launcher sends samples of the twelve [chart metrics](using.md#reading-the-results) to the
server. The server keeps the latest report of each run in the telemetry store: in its own memory, or in Redis when
several servers share a deployment. A report expires 6&nbsp;hours after the launcher last sent one.

The charts of a running experiment come from these reports. As soon as a run has ended, the server reads its charts from
the output files of the run instead. The state of an experiment never depends on the telemetry store: the server derives
it from the outcomes of the runs, which it records in the database.

The number of tasks in a progress bar is fixed when the experiment is submitted. The launcher reports only how many
tasks have finished, so the total does not change during a run.

## Settling and retrying

When the platform reports that an execution ended, the server *settles* it. It stores the last 256&nbsp;KiB of the
launcher log, reads the outcome of each run and gives each run a verdict. A run with a stored outcome gets that outcome.
A run without one gets the reason for which the process ended. Table 1 lists the reasons.

| Reason            | The process                                                  | Retried                    | Charged |
|-------------------|--------------------------------------------------------------|----------------------------|---------|
| `ok`              | ended normally                                               | no                         | yes     |
| `simulationError` | failed while simulating                                      | no                         | yes     |
| `invalidSpec`     | could not read or validate what it was given                 | no                         | yes     |
| `oom`             | ran out of memory                                            | yes, with more memory      | no      |
| `timeout`         | outlived the time limit the server set                       | yes, with a longer limit   | no      |
| `walltime`        | outlived a limit of the platform                             | while the limit can grow   | no      |
| `unknown`         | disappeared: an eviction, a lost job or a failed transfer    | yes, unchanged             | no      |
| `cancelled`       | was cancelled                                                | no                         | yes     |
| `rejected`        | was refused by the platform                                  | no                         | no      |

/// table-caption
The reasons for which an execution ends, and how the server responds.
///

The server retries only the runs that did not succeed, and only for reasons that more resources or another attempt can
cure. Each retry doubles the memory or the time limit of the failed attempt. A run gets at most three attempts, and a
retry never requests more than 32&nbsp;GiB. An administrator can start a further attempt from the
[admin panel](operations.md#the-admin-panel). The limits are settings under `opendc.execution.retry.*`.

The server charges the submitter for the cores an execution held, multiplied by the time it ran. Attempts that ended for
lack of resources are free, as Table 1 shows.

In summary, the server estimates each run, packs runs into executions that fit the platform, and lets the platform and
the stored outcomes decide how each run ended. Resource failures lead to a retry, and all other failures to a failed
run with a log.
