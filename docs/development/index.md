---
description: How to set up a development environment for OpenDC, and how its repository is organised.
---

# Development

This section is for people who change OpenDC itself: its simulator, its SDKs, its web application or this
documentation. This page explains how to set up a development environment and how the repository is organised. The
other pages of the section cover the [architecture](architecture.md), [building and testing](building.md), and
[contributing](contributing.md).

## Requirements

OpenDC is built with [Gradle](https://gradle.org). The repository contains the Gradle wrapper, which downloads the
right version of Gradle, so only the following tools have to be installed:

| Tool                                 | Needed for                                                              |
|--------------------------------------|-------------------------------------------------------------------------|
| Git                                  | obtaining the source code                                               |
| Java Development Kit (JDK)&nbsp;21   | everything. `java` in `gradle/libs.versions.toml` sets the version.     |
| [uv](https://docs.astral.sh/uv/)     | the Python SDK, whose tools Gradle runs through uv                      |
| Docker                               | building the images of the web application, and running a [deployment](../web/deployment.md) |

Two tools are deliberately absent from this list. Gradle downloads Node.js and pnpm for the web frontend. No test needs
Docker or a database server: tests of the web server run on an in-memory database.

## Getting the source and building it

Clone the repository and build it:

``` bash
git clone https://github.com/atlarge-research/opendc.git
cd opendc
./gradlew build
```

`build` compiles every module, checks the code style and runs all tests. The first build also downloads the
dependencies and therefore takes longer than later ones.

A full build includes the Python SDK, which needs uv. To work without uv, build the modules of interest by name:

``` bash
./gradlew :opendc-cli:build
```

To run the result, install the `opendc` command from the source tree, as
[Installation](../getting-started/installation.md) describes:

``` bash
./gradlew :opendc-cli:installDist
opendc-cli/build/install/OpenDCExperimentRunner/bin/opendc --help
```

[Developing with IntelliJ](intellij.md) explains how to open the project in IntelliJ IDEA and how to run and debug an
experiment from source.

## Repository layout

The repository is a single Gradle build with the following directories:

| Directory           | Content                                                                                         |
|---------------------|-------------------------------------------------------------------------------------------------|
| `opendc-common`     | units of measurement and logging, shared by all modules                                         |
| `opendc-simulator`  | the simulator: the event engine, the flow network, the compute model and failure injection      |
| `opendc-trace`      | reading and writing traces in Parquet                                                           |
| `opendc-sdk`        | the [Kotlin SDK](../kotlin-sdk/index.md), with model and runner, and the [Python SDK](../python-sdk/index.md) |
| `opendc-cli`        | the [`opendc`](../reference/cli.md) command                                                     |
| `opendc-web`        | the [web application](../web/index.md): server, dispatcher, launcher and frontend               |
| `deploy`            | Kubernetes manifests, Grafana dashboards and the smoke test of a deployment                     |
| `docs`              | this documentation, with its configuration in `zensical.toml`                                   |
| `buildSrc`, `gradle` | the build logic: convention plugins, the version catalogue and the Gradle wrapper              |
| `.github/workflows` | the continuous-integration workflows                                                            |

[Architecture](architecture.md) describes the modules in these directories and how they depend on each other.
