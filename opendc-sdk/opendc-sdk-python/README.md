# OpenDC Python SDK

`opendc-sdk` builds [OpenDC](https://opendc.org) experiments in Python, simulates them in the Python process or on an
OpenDC server, and returns every table they export as a pandas DataFrame.

| Name | Role |
|---|---|
| `opendc.ExperimentSpec`, `opendc.HostSpec`, ... | the Kotlin SDK's classes, with named and default arguments |
| `opendc.experiment()`, `opendc.topology()`, ... | a DSL that mirrors the Kotlin DSL in `with` blocks |
| `opendc.WorkloadTrace`, `CarbonTrace`, `FailureTrace` | traces as DataFrames or parquet files |
| `opendc.simulate()` | simulates in the Python process |
| `opendc.Server` | simulates on an OpenDC server |
| `opendc.SimulationResults` | the exported tables, one DataFrame each |

An *experiment* (`ExperimentSpec`) lists candidate topologies, workloads and policies. The SDK expands it into
*scenarios*, one per combination, and simulates each scenario once per seed (`runs` seeds, counting up from
`initial_seed`); one such simulation is a *run*. Each run exports tables, such as `host` and `powerSource`, and the SDK
joins the runs of each table into one DataFrame.

## Install

```bash
pip install opendc-sdk
```

The package installs as `opendc-sdk` and imports as `opendc`. It needs Python 3.11 or newer and Java 21 or newer, found
through `JAVA_HOME`. The wheel, about 47 MiB, bundles the JVM libraries of the SDK, each under its own licence, so it
works offline.

## Example

This example runs one workload on two clusters, one of 4 hosts and one of 16 hosts, and plots the power each draws. It
needs matplotlib.

```python
import matplotlib.pyplot as plt

import opendc
from opendc import ghz, gib, minutes

HOST_COUNTS = [4, 16]
COLORS = ["#2a78d6", "#eb6834"]

with opendc.experiment() as experiment:
    for host_count in HOST_COUNTS:
        with experiment.topology() as topology:
            with topology.datacenter() as datacenter:
                with datacenter.cluster() as cluster:
                    with cluster.host(count=host_count) as host:
                        host.cpu(core_count=16, core_speed=ghz(2.4))
                        host.memory(size=gib(64))

    with experiment.workload() as workload:
        for task_id in range(24):
            with workload.task(
                id=task_id,
                submission_time=minutes(10 * task_id),
                duration=minutes(90),
                cpu_core_count=8,
                cpu_capacity=ghz(16),
                memory=gib(16),
            ) as task:
                task.fragment(duration=minutes(90), cpu_usage=ghz(12))

results = opendc.simulate(experiment)

# One column per scenario; the scenarios follow the order of the topologies
power = results.power_source.pivot_table(index="timestamp", columns="scenario", values="power_draw")
hours = power.index / 3_600_000  # timestamps are milliseconds since the start

fig, ax = plt.subplots(figsize=(8, 4))
for scenario, host_count, color in zip(power.columns, HOST_COUNTS, COLORS):
    ax.plot(hours, power[scenario] / 1000, label=f"{host_count} hosts", color=color, linewidth=2)
ax.set_ylim(bottom=0)
ax.grid(axis="y", color="#e6e6e3")
ax.spines[["top", "right"]].set_visible(False)
ax.legend(frameon=False)
ax.set(xlabel="Time (h)", ylabel="Power draw (kW)", title="One workload on 4 and on 16 hosts")
fig.savefig("power.png", dpi=150, bbox_inches="tight")
```

The figure in `power.png` shows that 16 hosts draw about three times the power of 4 hosts, mostly idle power, yet
finish the workload only about 20 minutes sooner.

## Building specs

A *spec* is an immutable SDK object that describes an experiment or a part of one, such as `HostSpec`. Python builds
specs with the SDK classes, with the DSL, or by decoding a JSON file.

**The SDK classes.** Every public class, object and top-level function of the Kotlin SDK model and runner is an
attribute of `opendc`. Arguments go by position or by name, in camelCase or snake_case, and the arguments left out take
Kotlin's defaults; `inspect.signature(opendc.CpuSpec)` lists the parameters. Members read in snake_case:

```python
cpu = opendc.CpuSpec(core_count=8, core_speed=ghz(3))
cpu.core_speed  # 3.000000 GHz
cpu.copy(count=2)  # a changed copy
opendc.SchedulerPrefabSpec.CoreMem  # an enum constant; also objects, nested classes and companion members
```

Unknown or missing arguments raise `TypeError`, unknown attributes raise `AttributeError`, and an exception thrown by
Kotlin keeps its message. `DataSize` and `DataRate` tell bits from bytes by case alone (`ofGib`, `ofGiB`), so their
members take Kotlin's exact name.

**The DSL.** `opendc.experiment()`, `scenario()`, `topology()`, `inline_workload()`, `trace_workload()`,
`filter_scheduler()` and `prefab_scheduler()` mirror the Kotlin DSL in `org.opendc.sdk.model.dsl`: one Python builder
per Kotlin builder, with the same functions, parameters and properties in snake_case. A `with` block takes the place of
a Kotlin builder lambda, and a block that raises adds nothing. A parameter or property left unset takes Kotlin's
default. A builder is accepted wherever a spec is expected.

**Units.** A quantity takes a unit, never a bare number. The helpers `ghz`, `mhz`, `gib`, `mib`, `bytes`, `watts`,
`kwatts`, `kwh`, `gbps` (gigabytes per second), `kibps` (kibibits per second), `ms`, `seconds`, `minutes` and `hours`
mirror `Units.kt`, and units support arithmetic and comparison (`minutes(5) * 2`).

**Experiment files.** `opendc.SdkJson` decodes an experiment stored as JSON:

```python
experiment = opendc.SdkJson.decode_experiment(Path("experiment.json").read_text())
```

## Traces

A spec names a trace by a *reference*: a `NamedReference` name or a `UriReference` URI. `simulate()` takes traces under
those keys, each table as a DataFrame or as parquet:

```python
results = opendc.simulate(
    experiment,
    traces={
        "bitbrains": opendc.WorkloadTrace(tasks=tasks_frame, fragments=fragments_frame),
        "surf": opendc.WorkloadTrace.from_parquet("traces/surf"),  # tasks.parquet and fragments.parquet
        "carbon-nl": opendc.CarbonTrace(carbon_frame),
        "failures": opendc.FailureTrace.from_parquet("traces/failures.parquet"),
    },
)
```

A reference missing from `traces` resolves to a file under `input_root`, the working directory by default, as the
`opendc` command-line interface (CLI) resolves it under `--input-root`. Inline carbon and failure models need no trace.
A trace of the wrong kind for its reference, such as a `CarbonTrace` under the key of a workload, raises `ValueError`
before anything runs, locally and on a server.

`WorkloadTrace.task_schema()`, `fragment_schema()`, `CarbonTrace.schema()` and `FailureTrace.schema()` return the
columns each table takes; a column that is not nullable is required. Durations are in milliseconds, `cpu_capacity`,
`gpu_capacity`, `cpu_usage` and `gpu_usage` in MHz, and `mem_capacity` in MiB. The SDK casts each table to its schema:
pandas datetimes and timedeltas become milliseconds, and a DataFrame column the schema lacks, or a missing required
column or value, raises `ValueError`. A parquet file may carry extra columns, which are ignored. The SDK reads a parquet
file in place when the file already matches the schema, as the traces OpenDC writes do.

## Running on a server

`opendc.Server` runs an experiment on an OpenDC server, as `opendc run --api-url` does, and returns the same results
as `simulate()`:

```python
server = opendc.Server("https://opendc.example.org", token="odc_pat_...")
results = server.simulate(experiment, traces={"surf": opendc.WorkloadTrace.from_parquet("traces/surf")})
```

The token is a personal access token; without one, `Server` reads `OPENDC_TOKEN`, and a server that signs nobody in
needs none. `Server.simulate()` uploads each trace in `traces` that the spec references; a later call that passes the
same content under the same key reuses the upload. A reference missing from `traces` has to name a trace on the server:
a built-in one, such as `bitbrains-small`, or one you own or that is shared with you, as `<handle>/<name>`. Ctrl-C
cancels the experiment on the server, from a script as well as interactively. An experiment whose runs do not all
succeed raises `RemoteRunFailed`, whose `results` hold the tables of the runs that succeeded.

## Results

`SimulationResults` maps the name of each table, as listed in the
[output reference](https://github.com/atlarge-research/opendc/blob/master/docs/reference/output/index.md), to one
DataFrame over all runs. Every DataFrame starts with the `scenario` and `seed` of the run. Attributes reach the tables
in snake_case:

```python
results.power_source  # also results["powerSource"]
results.meta.host  # the static attributes of each host
results.scenarios  # the spec of each run, indexed by (scenario, seed)
```

Runs whose columns differ, such as topologies with and without GPUs, share one DataFrame with empty cells.
`simulate()` validates the spec first and raises every issue in one `ValueError`. It stages traces and output in a
temporary directory under `TMPDIR` and removes the directory before it returns.

## The JVM

The SDK runs in a Java virtual machine (JVM) inside the Python process. The JVM starts when the SDK is first used. To
pass JVM options, call `opendc.start_jvm("-Xmx8g")` before anything else uses the SDK: a process starts one JVM only.
`simulate(parallelism=4)` simulates 4 runs at once, and each needs the memory of its own simulation. Do not fork the
process once the JVM runs.

## How it works

[JPype](https://jpype.readthedocs.io) runs the JVM inside CPython. Kotlin hides default arguments and value classes
from Java, so the bridge, `org.opendc.sdk.python`, calls the SDK through kotlin-reflect: it binds Python arguments by
name and lets Kotlin apply its defaults. Gradle stages the bridge and its runtime classpath in `opendc/jars/`, which
the wheel ships. The DSL is written by hand, so a test fails when a builder, function, parameter or property of the
Kotlin DSL has no Python counterpart.

## Build and test

Building needs JDK 21 and [uv](https://docs.astral.sh/uv/). Gradle runs uv with `JAVA_HOME` set to its Java toolchain.

| Task | Runs |
|---|---|
| `stageJvmLibraries` | copies the bridge jar and its runtime classpath into `opendc/jars/` |
| `uvSync` | `uv sync --locked`, after staging the jars |
| `uvFormat`, `uvFormatCheck` | `uv run --locked ruff format [--check]` |
| `uvLint` | `uv run --locked ruff check` |
| `uvTypeCheck` | `uv check --locked` (ty) |
| `uvTest` | `uv run --locked pytest`, reported to `build/test-results/pytest` |
| `uvBuild` | `uv build --wheel` into `build/distributions` |

```bash
./gradlew :opendc-sdk:opendc-sdk-python:build          # JUnit, pytest, Ruff, ty and the wheel
./gradlew :opendc-sdk:opendc-sdk-python:spotlessApply  # licence headers and ktlint for the bridge
./gradlew :opendc-sdk:opendc-sdk-python:uvSync         # then: uv run pytest, uv run python
```

A GitHub release tagged `v*` publishes the wheel to PyPI through
[`publish.yml`](https://github.com/atlarge-research/opendc/blob/master/.github/workflows/publish.yml). Snapshots are
never published.
