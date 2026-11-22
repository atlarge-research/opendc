---
description: The commands and options of the opendc command-line interface.
---

# Command Line

The `opendc` command is the command-line interface (CLI) of OpenDC. It runs, validates and inspects
[experiment files](experiment/index.md). This page documents its commands and options.
[Installation](../getting-started/installation.md) explains how to obtain the command, and
[First Experiment](../getting-started/first-experiment.md) introduces it by example.

## Synopsis

``` text
opendc [<global options>] <command> [<options>] <experiment>
```

Every command takes the path of an experiment file, and `--help` prints the options of `opendc` or of a command.

| Command    | Description                                                               |
|------------|---------------------------------------------------------------------------|
| `run`      | Simulates every scenario of the experiment and writes the results.        |
| `validate` | Checks the experiment and reports every problem, without simulating.      |
| `show`     | Prints the topologies of the experiment.                                  |

## Global options

The following options belong to `opendc` itself and apply to every command:

| Option               | Description                                                                                          |
|----------------------|------------------------------------------------------------------------------------------------------|
| `--strict`           | Stops with an error when the experiment has an unknown key. By default, OpenDC ignores unknown keys. |
| `--input-root <dir>` | The directory against which OpenDC resolves the relative paths of an experiment file: its [imports](experiment/index.md#splitting-an-experiment-over-several-files) and its [trace references](experiment/index.md#referencing-trace-files). By default, this is the directory from which `opendc` is started. |
| `--legacy`           | Reads the experiment in the format of OpenDC&nbsp;2. See [Migrating to OpenDC 3](../opendc-2/migrating.md). |

!!! warning "Global options come before the command"

    A global option after the command is an error:

    ``` bash
    opendc run experiment.json --strict      # Error: no such option --strict
    opendc --strict run experiment.json      # correct
    ```

## Running an experiment

`opendc run` simulates each scenario of the experiment and writes the [output](output/index.md) of each run. While it
simulates, it shows a dashboard with the progress of the experiment and the most recent log messages. When it is done,
it prints a summary line per run and the location of the results.

| Option                     | Default  | Description                                                                             |
|----------------------------|----------|-----------------------------------------------------------------------------------------|
| `-o`, `--output <dir>`     | `output` | The directory to which the results are written.                                         |
| `-p`, `--parallelism <n>`  | `1`      | The number of runs that are simulated at the same time.                                 |
| `--writer-threads <n>`     | `1`      | The number of threads that write the task and host files of a run.                      |
| `--no-progress`            |          | Does not show the dashboard.                                                            |
| `--no-summary`             |          | Does not print the summary at the end.                                                  |

Three of these options trade speed against memory:

- Every run that `--parallelism` adds needs the memory of its own simulation. The number cannot exceed the number of
  processors of the machine.
- More `--writer-threads` speed up a single large run on a machine that is otherwise idle. With many runs in parallel,
  the processors are already busy, and one writer thread is the better choice.
- OpenDC keeps samples in memory to compute the summary. `--no-summary` saves this memory for large experiments.

`--no-progress` suits scripts and any situation in which the output goes to a file, because the dashboard needs an
interactive terminal.

``` bash
opendc run experiment.json -o results -p 4 --no-progress
```

### Running on a server

With `--api-url`, `opendc run` does not simulate on the local machine. It submits the experiment to an
[OpenDC server](../web/index.md), follows its progress in the same dashboard, and downloads the results to the output
directory in the layout of a local run.

``` bash
OPENDC_TOKEN=odc_pat_... opendc run experiment.json --api-url https://opendc.example.org -o results
```

| Option            | Description                                                                                             |
|-------------------|---------------------------------------------------------------------------------------------------------|
| `--api-url <url>` | The address of the server.                                                                              |
| `--project <id>`  | The project in which the experiment runs. By default, OpenDC uses a project called `opendc-cli`, which it creates on first use. |
| `--token <token>` | A [personal access token](../web/using.md#account). The environment variable `OPENDC_TOKEN` is an alternative. A server that signs nobody in needs no token. |

The server checks the experiment as it checks one from the web application. A workload therefore has to reference a
trace that the server knows, by its name in the [trace library](../web/using.md#traces): a built-in trace such as
`bitbrains-small`, or a trace of the account such as `alice/nightly`. `opendc` does not upload local trace files. The
[Python SDK](../python-sdk/server.md) does.

Interrupting the command with ++ctrl+c++ cancels the experiment on the server.

### Exit status

| Status | Meaning                                                                                    |
|--------|--------------------------------------------------------------------------------------------|
| 0      | Every scenario was simulated.                                                              |
| 1      | The experiment file is invalid or unreadable, or a run on a server did not succeed.        |
| 2      | The server refused the experiment. `opendc` prints the reasons of the server.              |

## Validating an experiment

`opendc validate` reads the experiment and checks its constraints, such as that every host has at least one core. It
reports every problem with the path of the field that it concerns:

``` bash
opendc validate experiment.json
```

``` text
✗ experiment.json has 2 issue(s):
  • runs: must be > 0
  • topologies[0].datacenters[0].clusters[0].hosts[0].cpu.coreCount: must be > 0
```

`run` and `show` perform the same check before they start. Validation does not open trace files, so a reference to a
trace that does not exist is only found by `run`.

## Showing the topologies

`opendc show` prints a table of the clusters and hosts of each topology. The command is a quick check that a topology
describes the intended hardware, in particular when it is assembled from several files.

## Changing the appearance

The colours, symbols and labels of the command are settings in a properties file. To change them, copy
[`opendc-ui.properties`](https://github.com/atlarge-research/opendc/blob/master/opendc-cli/src/main/resources/opendc-ui.properties)
from the repository, edit the copy and name it in the environment variable `OPENDC_UI_CONFIG`:

``` bash
OPENDC_UI_CONFIG=my-ui.properties opendc run experiment.json
```

The file needs to contain only the settings that differ. For example, `symbol.success=OK` and `symbol.failure=FAIL`
replace two symbols that some terminals cannot display.
