---
description: How the Python SDK reaches the simulator through a Java virtual machine, and how to build and test it.
---

# Internals

The Python SDK does not implement a simulator. It runs the simulator of OpenDC, which is written for the Java virtual
machine (JVM), inside the Python process. This page explains what this means for a program that uses the SDK, how the
connection works, and how to build and test the SDK.

## The JVM

The SDK starts a JVM inside the Python process when a program first uses it. Three consequences follow:

- A process starts only one JVM. To pass options to it, such as a larger heap, call `opendc.start_jvm` before anything
  else uses the SDK. A later call with options raises `RuntimeError`.
- The simulator uses the memory of the JVM, not of Python. `simulate(parallelism=4)` simulates 4&nbsp;runs at once,
  and each run needs the memory of its own simulation.
- A process must not fork once the JVM runs. Libraries that fork worker processes, such as `multiprocessing` with the
  start method `fork`, have to start their workers before the first use of the SDK, or use the start method `spawn`.

``` python
import opendc

opendc.start_jvm("-Xmx8g")
```

The SDK needs Java&nbsp;21 or newer and finds it through the environment variable `JAVA_HOME`. With an older version,
`start_jvm` raises `RuntimeError` and states the version that it found.

## How the SDK reaches Kotlin

[JPype](https://jpype.readthedocs.io) runs the JVM inside the Python interpreter and lets Python call Java classes.
The SDK of OpenDC is written in Kotlin, and two features of Kotlin are invisible from Java: default arguments, and
value classes such as the quantities with units. The SDK therefore contains a small Kotlin library, the *bridge*, in
the package `org.opendc.sdk.python`. The bridge calls the SDK through the reflection library of Kotlin: it binds the
arguments of a Python call by name and lets Kotlin fill in its defaults. Figure 1 shows the path of a call.

``` mermaid
flowchart LR
    program["Python program"] --> package["opendc package"]
    package -->|"JPype"| bridge["bridge<br>org.opendc.sdk.python"]
    bridge -->|"Kotlin reflection"| sdk["Kotlin SDK<br>model and runner"]
    sdk --> simulator["simulator"]
```

/// figure-caption
The path of a call from a Python program to the simulator.
///

This design has two effects that a user notices. First, the classes of the model are not copied into Python: the
attributes of `opendc` are looked up in the Kotlin SDK, so a class that the Kotlin SDK gains is available in Python at
once. Second, results do not cross the bridge value by value. The simulator writes Parquet files to a temporary
directory, and Python reads the files into DataFrames, which keeps large results fast.

The builders are the exception: they are written by hand in Python, because a `with` block has no counterpart that
could be generated. A test compares them with the builders of the Kotlin SDK and fails when a builder, function,
parameter or property has no Python counterpart.

## Building and testing

The module is `opendc-sdk/opendc-sdk-python` in the [repository](https://github.com/atlarge-research/opendc). Building
it needs Java&nbsp;21 and [uv](https://docs.astral.sh/uv/), which manages the Python environment. Gradle drives uv and
sets `JAVA_HOME` to its own Java toolchain, so the usual Gradle commands build and test the Python code as well:

``` bash
./gradlew :opendc-sdk:opendc-sdk-python:build           # JUnit, pytest, Ruff, ty and the wheel
./gradlew :opendc-sdk:opendc-sdk-python:spotlessApply   # licence headers and ktlint for the bridge
```

The module has the following tasks:

| Task                        | Action                                                                              |
|-----------------------------|-------------------------------------------------------------------------------------|
| `stageJvmLibraries`         | copies the bridge and the libraries it needs into `src/main/python/opendc/jars/`    |
| `uvSync`                    | creates the Python environment with `uv sync --locked`                              |
| `uvFormat`, `uvFormatCheck` | formats the code with Ruff, or checks its format                                    |
| `uvLint`                    | checks the code with Ruff                                                           |
| `uvTypeCheck`               | checks the types with ty                                                            |
| `uvTest`                    | runs pytest and reports to `build/test-results/pytest`                              |
| `uvBuild`                   | builds the wheel into `build/distributions`                                         |

To work on the Python code directly, stage the Java libraries once and then run uv in the module:

``` bash
./gradlew :opendc-sdk:opendc-sdk-python:uvSync
cd opendc-sdk/opendc-sdk-python
JAVA_HOME=<path of a JDK 21> uv run --locked pytest
```

The sources follow the layout of a Gradle module: the Python package is in `src/main/python`, its tests are in
`src/test/python`, and the bridge is in `src/main/kotlin`. The file `pyproject.toml` at the root of the module
configures the package and its tools.

A release of OpenDC publishes the wheel to the Python Package Index through the workflow
[`publish.yml`](https://github.com/atlarge-research/opendc/blob/master/.github/workflows/publish.yml). Development
versions are not published.
