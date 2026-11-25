---
description: The Kotlin SDK of OpenDC, two JVM libraries that describe and simulate experiments from code.
---

# Kotlin SDK

The Kotlin software development kit (SDK) is the programming interface of OpenDC for the Java virtual machine (JVM). It
describes an experiment as data and simulates it from code. This page introduces its two libraries, shows how to add
them to a project, and walks through a complete program.

## Two libraries

The SDK separates the description of an experiment from its execution:

| Library             | Package                 | Role                                                                             |
|---------------------|-------------------------|----------------------------------------------------------------------------------|
| `opendc-sdk-model`  | `org.opendc.sdk.model`  | The *model*: immutable data classes that describe an experiment, with validation and a JSON form. |
| `opendc-sdk-runner` | `org.opendc.sdk.runner` | The *runner*: simulates an experiment and delivers its results to files or to the program. |

The model performs no input or output and knows no file paths. A program can therefore build an experiment in code,
parse it from a file or load it from a database, and validate it in the same way each time. The runner drives the
simulator. The `opendc` command, the Python SDK and the web server are all built on these two libraries, so a program
that uses the SDK simulates exactly what they simulate.

[The Model](model.md) and [The Runner](runner.md) describe the libraries in detail.

## Adding the SDK to a project

Releases of OpenDC publish the libraries to Maven Central under the group `org.opendc`. The runner depends on the
model, so one dependency is enough:

=== "Gradle (Kotlin)"

    ``` kotlin
    dependencies {
        implementation("org.opendc:opendc-sdk-runner:<version>")
    }
    ```

=== "Maven"

    ``` xml
    <dependency>
        <groupId>org.opendc</groupId>
        <artifactId>opendc-sdk-runner</artifactId>
        <version>VERSION</version>
    </dependency>
    ```

!!! note "Before the first release of OpenDC 3"

    OpenDC&nbsp;3 has not been released yet, so Maven Central does not hold the libraries. Until it does, use the
    development version.

To use the development version, publish the libraries from a checkout of the
[repository](https://github.com/atlarge-research/opendc) to the local Maven repository:

``` bash
./gradlew publishToMavenLocal
```

Then add `mavenLocal()` to the repositories of the project, and depend on the version `3.0-SNAPSHOT`.

The SDK needs Java&nbsp;21 or newer. The runner logs through [SLF4J](https://www.slf4j.org), so a program chooses its
own logging backend.

## A complete program

The following program builds an experiment with two allocation policies, simulates it and prints the energy that each
scenario uses. It describes the topology of [First Experiment](../getting-started/first-experiment.md) and a workload
of one task with the builders of the SDK instead of JSON.

``` kotlin title="Example.kt"
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.ghz
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.dsl.prefabScheduler
import org.opendc.sdk.model.dsl.watts
import org.opendc.sdk.model.scheduler.SchedulerPrefabSpec
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.topology.LinearPowerModelSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.sink.InMemorySink
import java.nio.file.Path

fun main() {
    val experiment =
        experiment { // (1)!
            name = "kotlin-example"

            topology {
                datacenter(name = "DC1") {
                    cluster(name = "C01") {
                        host(count = 4, name = "H01") {
                            cpu(coreCount = 16, coreSpeed = 2.1.ghz) // (2)!
                            memory(size = 64.gib)
                            cpuPowerModel = LinearPowerModelSpec(maxPower = 180.watts, idlePower = 32.watts)
                        }
                    }
                }
            }

            workload {
                task(
                    id = 0,
                    submissionTime = 0.minutes,
                    duration = 10.minutes,
                    cpuCoreCount = 2,
                    cpuCapacity = 4.2.ghz,
                    memory = 2.gib,
                ) {
                    fragment(duration = 10.minutes, cpuUsage = 4.2.ghz)
                }
            }

            allocationPolicy(prefabScheduler(SchedulerPrefabSpec.Mem)) // (3)!
            allocationPolicy(prefabScheduler(SchedulerPrefabSpec.ActiveServers))
            exportModel(ExportSpec(exportInterval = 1.minutes))
        }

    val issues = experiment.validate() // (4)!
    check(issues.isEmpty()) { issues.joinToString("\n") { "${it.path}: ${it.message}" } }

    val report =
        OpenDC.builder()
            .provisioner(FileSystemResourceProvisioner(Path.of("."))) // (5)!
            .output(Path.of("output")) // (6)!
            .sink(InMemorySink())
            .build()
            .simulate(experiment)

    for (result in report.scenarios) {
        for (run in result.runs) {
            val energy = run.metrics!!.powerSource.last().energyUsage
            println("scenario ${result.scenario.id}, seed ${run.seed}: $energy J, written to ${run.outputPath}")
        }
    }
}
```

1.  `experiment { }` is a type-safe builder. Each nested block builds one part of the
    [experiment document](../reference/experiment/index.md), and the names of functions and parameters follow the
    fields of the document.
2.  Quantities carry their unit: `2.1.ghz` is a frequency, `64.gib` a data size.
3.  Calling a function twice adds a second candidate. This experiment has two allocation policies and therefore two
    scenarios.
4.  Validation returns a list of issues and does not throw. An empty list means that the experiment is valid.
5.  The provisioner resolves trace references. This experiment has none, but the runner requires one.
6.  `output` writes the Parquet files of each run, and `InMemorySink` also keeps the samples in memory.

The program prints one line per run:

``` text
scenario 0, seed 0: 87900.0 J, written to output/kotlin-example/raw-output/0/seed=0
scenario 1, seed 0: 87900.0 J, written to output/kotlin-example/raw-output/1/seed=0
```

Both scenarios use the same energy: with a single task on four identical hosts, the two policies have no choice to make
differently.

## Using the SDK from Java

The runner is designed for Java as well as Kotlin: `OpenDC.builder()` is a static method, and its options have
overloads. The builders of the model rely on Kotlin language features, so a Java program reads an experiment from JSON
instead:

``` java
ExperimentSpec experiment = SdkJson.INSTANCE.decodeExperiment(Files.readString(Path.of("experiment.json")));

SimulationReport report = OpenDC.builder()
        .provisioner(new FileSystemResourceProvisioner(Path.of(".")))
        .output(Path.of("output"))
        .build()
        .simulate(experiment);
```
