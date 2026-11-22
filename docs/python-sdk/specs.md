---
description: How to build experiments with the Python SDK, with builders, with the classes of the model or from JSON.
---

# Building Experiments

A *spec* is an immutable object that describes an experiment or a part of one, such as a host. The Python SDK offers
three ways to obtain a spec: builders, the classes of the model and JSON. This page describes each of them, and how
to write quantities with units.

The specs of the Python SDK are the specs of the [Kotlin SDK](../kotlin-sdk/model.md). Their fields are the fields of
the [experiment file](../reference/experiment/index.md), so the reference pages of the file also document the specs.
Python writes names in snake case: the field `coreCount` is the argument `core_count`.

## Builders

A builder collects the parts of a spec in a `with` block. The block of a builder offers one function per part, and
a part that has parts of its own is again a `with` block:

``` python
import opendc
from opendc import ghz, gib, hours, watts

with opendc.topology() as small:
    with small.datacenter(name="DC1") as datacenter:
        with datacenter.cluster(name="C01") as cluster:
            with cluster.host(count=4, name="H01") as host:
                host.cpu(core_count=16, core_speed=ghz(2.4))
                host.memory(size=gib(64))
                host.cpu_power_model = opendc.LinearPowerModelSpec(idle_power=watts(100), max_power=watts(350))
```

The SDK has seven builders:

| Function                    | Builds                                                                              |
|-----------------------------|-------------------------------------------------------------------------------------|
| `opendc.experiment()`       | an [experiment](../reference/experiment/index.md)                                   |
| `opendc.scenario()`         | a single [scenario](../kotlin-sdk/model.md#experiments-and-scenarios)               |
| `opendc.topology()`         | a [topology](../reference/experiment/topology/index.md)                             |
| `opendc.inline_workload()`  | a [workload](../reference/experiment/workload.md#inline-workloads) of listed tasks  |
| `opendc.trace_workload()`   | a workload that replays a [trace](traces.md)                                        |
| `opendc.filter_scheduler()` | an [allocation policy](../reference/experiment/allocation-policy.md) of filters and weighers |
| `opendc.prefab_scheduler()` | an allocation policy by the name of a prefab                                        |

The builders mirror the builders of the Kotlin SDK: each has the same functions, parameters and properties, in snake
case. A parameter or property that is not set takes the default of the experiment file.

The builder of an experiment collects candidates. Each call of `topology`, `workload`, `allocation_policy`,
`failure_model`, `checkpoint_model`, `export_model` or `max_num_failures` adds one candidate, and OpenDC simulates
every combination. These functions accept a finished spec or another builder, so parts can be built once and used in
several experiments:

``` python
with opendc.filter_scheduler() as packing:
    packing.filter(opendc.ComputeHostFilterSpec)
    packing.filter(opendc.VCpuFilterSpec(allocation_ratio=2.0))
    packing.filter(opendc.RamFilterSpec())
    packing.weigher(opendc.RamWeigherSpec(multiplier=-1.0))

with opendc.experiment() as experiment:
    experiment.name = "sweep"
    experiment.runs = 2
    experiment.topology(small)
    experiment.workload(opendc.trace_workload(source=opendc.NamedReference("traces/bitbrains-small")))
    experiment.allocation_policy(opendc.prefab_scheduler())
    experiment.allocation_policy(packing)
    experiment.export_model(opendc.ExportSpec(export_interval=hours(1)))
```

This experiment has one topology, one workload and two allocation policies, and therefore two scenarios. With two runs
per scenario, it stands for four simulations.

`simulate` accepts a builder directly. `build()` returns the spec that a builder describes, for a program that wants
to inspect it:

``` python
spec = experiment.build()
len(spec.expand())    # 4, one scenario per run
spec.validate()       # [], the list of issues
```

A `with` block that raises an exception adds nothing to its parent.

## Classes of the model

Every public class, object and function of the Kotlin SDK is an attribute of `opendc`: the specs, such as
`opendc.HostSpec`, and the tools around them, such as `opendc.SdkJson`. A class is called like a Python class.
Arguments go by position or by name, and an argument that is left out takes its default:

``` python
host = opendc.HostSpec(
    name="H01",
    count=4,
    cpu=opendc.CpuSpec(core_count=16, core_speed=ghz(2.4)),
    memory=opendc.MemorySpec(size=gib(64)),
)

host.cpu.core_speed                    # 2.400000 GHz
host.copy(count=8)                     # a changed copy
opendc.SchedulerPrefabSpec.CoreMem     # a constant of an enumeration
```

`inspect.signature(opendc.CpuSpec)` lists the parameters of a class. The classes suit programs that generate specs,
for example in a loop over parameters, whereas builders read better for specs that are written by hand.

The SDK reports mistakes as Python exceptions:

| Mistake                                 | Exception                                                        |
|-----------------------------------------|------------------------------------------------------------------|
| an unknown or a missing argument        | `TypeError`, with the parameters of the class                    |
| an argument of the wrong type           | `TypeError`, with the expected type                              |
| an unknown attribute                    | `AttributeError`, with the attributes of the object              |
| an invalid spec passed to `simulate`    | `ValueError`, with every [issue](../kotlin-sdk/model.md#validation) and its path |

## JSON

`opendc.SdkJson` converts between specs and the JSON of an experiment file. It gives a Python program access to
experiments that were written for the [`opendc`](../reference/cli.md) command or exported from the
[web application](../web/using.md):

``` python
from pathlib import Path

experiment = opendc.SdkJson.decode_experiment(Path("experiment.json").read_text())
text = opendc.SdkJson.encode_to_string(experiment)
```

`SdkJson` does not resolve [`importFrom`](../reference/experiment/index.md#splitting-an-experiment-over-several-files),
which is a feature of the `opendc` command. An experiment that is split over several files therefore has to be joined
before Python can read it.

## Units

A quantity always has a unit and is never a bare number. The following functions construct quantities:

| Quantity  | Functions                                                                   |
|-----------|-----------------------------------------------------------------------------|
| Frequency | `ghz`, `mhz`                                                                |
| Data size | `gib`, `mib`, `bytes`                                                       |
| Power     | `watts`, `kwatts`                                                           |
| Energy    | `kwh`                                                                       |
| Data rate | `gbps` (gigabytes per second), `kibps` (kibibits per second)                |
| Duration  | `ms`, `seconds`, `minutes`, `hours`                                         |

Quantities support arithmetic and comparison, such as `minutes(5) * 2` and `ghz(2.4) > ghz(2)`. The classes behind the
functions offer further units, such as `opendc.DataSize.ofTiB(1)` and `opendc.Frequency.ofKHz(800)`. The names of
these functions are written as in Kotlin, because `DataSize` and `DataRate` tell bits from bytes by case alone:
`ofGib` is gibibits, and `ofGiB` is gibibytes.

!!! warning "`bytes` shadows a built-in"

    `from opendc import bytes` replaces the built-in type `bytes` of Python in the importing module. Write
    `opendc.bytes(...)` instead where a program also uses the built-in type.
