---
description: How to obtain OpenDC 2 and run an experiment with it.
status: deprecated
search:
  exclude: true
---

# Running OpenDC 2

This page explains how to obtain OpenDC 2 and how to run an experiment with it, from the command line or from IntelliJ.
OpenDC 2 runs an [experiment file](experiment.md) with the program `OpenDCExperimentRunner`, which this page calls the
*runner*. OpenDC 2 needs Java&nbsp;21 or newer.

## Getting OpenDC 2

The last release of OpenDC 2 is [v2.4u](https://github.com/atlarge-research/opendc/releases/tag/v2.4u). Download
`OpenDCExperimentRunner.zip` or `OpenDCExperimentRunner.tar` from the release and unpack it. The archive contains a
`bin` folder with the runner and a `lib` folder with the libraries that the runner needs.

Fixes made after this release are on the
[`OpenDC2-maintenance`](https://github.com/atlarge-research/opendc/tree/OpenDC2-maintenance) branch. Build the runner
from that branch with the following commands:

``` bash
git clone --branch OpenDC2-maintenance https://github.com/atlarge-research/opendc.git
cd opendc
./gradlew :opendc-experiments:opendc-experiments-base:installDist
```

The build places the runner in `opendc-experiments/opendc-experiments-base/build/install/OpenDCExperimentRunner/`.

## Running an experiment

Start the runner with the path of an experiment file:

``` bash
OpenDCExperimentRunner/bin/OpenDCExperimentRunner --experiment-path experiment.json
```

The runner has two options:

| Option              | Description                                                                                                    |
|---------------------|----------------------------------------------------------------------------------------------------------------|
| `--experiment-path` | The experiment file to run. The default is `resources/experiment.json`.                                        |
| `-strict`           | Stop with an error when the experiment or a topology has an unknown key. By default, unknown keys are ignored. |

All paths in an experiment, such as those of the topologies and traces, are relative to the directory from which the
runner is started. The runner writes the results to the folder set in the experiment, as [Output](output.md) describes.

## Running from IntelliJ

To run or debug an experiment from IntelliJ, follow these steps:

1. Open the `OpenDC2-maintenance` checkout in IntelliJ. IntelliJ imports it as a Gradle project.
2. Open `opendc-experiments/opendc-experiments-base/src/main/kotlin/org/opendc/experiments/base/runner/ExperimentCli.kt`.
3. Click the run arrow next to `fun main` and choose **Modify Run Configuration...**.
4. Set **Program arguments** to `--experiment-path experiment.json` and **Working directory** to the folder with the
   experiment files.
5. Run or debug the configuration. The runner writes the results to the working directory.

[M3SA](m3sa.md) explains how to run Multi-Meta-Model Simulation Analysis after a simulation.
