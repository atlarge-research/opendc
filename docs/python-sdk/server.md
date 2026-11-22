---
description: How to run an experiment on an OpenDC server from Python and receive the same results as from a local simulation.
---

# Running on a Server

`opendc.Server` runs an experiment on an [OpenDC server](../web/index.md) instead of in the Python process. A server
suits experiments that are too large for the local machine, and it keeps the experiment and its results for others to
see. This page explains how to connect to a server, how traces reach it, and what happens when a run fails.

## Simulating on a server

A `Server` object stands for the server at a URL. Its function `simulate` has the result of the local
[`simulate`](results.md): a `SimulationResults` object with every table as a DataFrame.

``` python
import opendc

server = opendc.Server("https://opendc.example.org", token="odc_pat_...")
results = server.simulate(experiment)
```

`simulate` validates the experiment, creates it on the server and submits it. It then waits until the experiment has
ended, downloads the results and returns them. The experiment remains on the server, where the
[web application](../web/using.md#reading-the-results) shows it with its results.

`Server` takes the following arguments:

| Argument        | Default           | Description                                                                               |
|-----------------|-------------------|-------------------------------------------------------------------------------------------|
| `url`           | required          | The address of the server, with the scheme `http` or `https`.                             |
| `token`         | `OPENDC_TOKEN`    | A [personal access token](../web/using.md#account). Without the argument, the SDK reads the environment variable `OPENDC_TOKEN`. A server that signs nobody in needs no token. |
| `project`       | `"opendc-python"` | The name of the [project](../web/using.md#projects) in which the experiments run. The SDK creates the project on first use. |
| `poll_interval` | `1.0`             | The time between two requests for the state of the experiment, in seconds.                |
| `patience`      | `120.0`           | How long the SDK keeps trying while the server or the network fails, in seconds.          |

!!! tip "Keep the token out of the program"

    A token grants access to an account. Set it in the environment variable `OPENDC_TOKEN` instead of writing it in a
    script or a notebook that is shared.

## Traces

A server simulates only with traces of its [trace library](../web/using.md#traces). The SDK takes care of this for
the traces that a program passes:

``` python
results = server.simulate(
    experiment,
    traces={"jobs": opendc.WorkloadTrace.from_parquet("traces/jobs")},
)
```

`simulate` uploads each trace in `traces` that the experiment references to the library of the account, and points the
experiment at the upload. The name of an upload ends in a digest of its content, such as `jobs-710fc16060a2`. A later
call that passes the same content under the same name finds the upload and does not send it again.

A reference that is missing from `traces` has to name a trace that the library already holds: a built-in trace, such as
`bitbrains-small`, or a trace of an account, written as `<handle>/<name>`. Unlike the local `simulate`, a server does
not read local files, so it has no `input_root`.

## Failures

`Server.simulate` raises one of two exceptions when an experiment does not succeed:

| Exception                | Raised when                                        | Attributes                                                     |
|--------------------------|----------------------------------------------------|----------------------------------------------------------------|
| `opendc.ApiFailure`      | the server refuses a request                       | `status`, the HTTP status, and `title` and `issues`, the reasons |
| `opendc.RemoteRunFailed` | the experiment ends, but not all of its runs succeed | `state`, `failures` with the reason per scenario, and `results` |

The server refuses an experiment that it cannot run, for example one that references an unknown trace or exceeds the
[budget](../web/operations.md#plans-and-budgets) of the account:

``` python
try:
    results = server.simulate(experiment)
except opendc.ApiFailure as failure:
    print(failure.status, failure.title, failure.issues)
```

``` text
400 The experiment document is invalid ['workloads[0].source: no trace called no-such-trace in your library']
```

An experiment in which some runs fail, for example because they run out of memory, ends in the state `partial` or
`failed`. The `results` of `RemoteRunFailed` hold the tables of the runs that succeeded, so the work of an experiment
that partly failed is not lost:

``` python
try:
    results = server.simulate(experiment)
except opendc.RemoteRunFailed as failure:
    print(failure.failures)
    results = failure.results
```

## Interrupting

Interrupting the program with ++ctrl+c++ cancels the experiment on the server, in a script as well as in an interactive
session. The same holds for any other exception that ends `simulate` early: a run that no program waits for would only
use up the budget of the account.

In summary, `Server.simulate` is the remote counterpart of `simulate`: it uploads the traces that a program passes,
runs the experiment under the account of the token, and returns the same DataFrames.
