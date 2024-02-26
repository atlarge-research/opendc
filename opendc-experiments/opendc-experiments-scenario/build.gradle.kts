/*
 * Copyright (c) 2019 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

description = "Experiments for the Scenario work"

/* Build configuration */
plugins {
    `kotlin-library-conventions`
    `kotlin-conventions`
    `testing-conventions`
    `jacoco-conventions`
    `benchmark-conventions`
    distribution
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation(projects.opendcSimulator.opendcSimulatorCore)
    implementation(projects.opendcSimulator.opendcSimulatorCompute)
    implementation(projects.opendcCompute.opendcComputeSimulator)

    implementation(libs.clikt)
    implementation(libs.progressbar)
    implementation(libs.kotlin.logging)

    implementation(libs.jackson.module.kotlin)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-telemetry")))
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-topology")))
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-workload")))
    implementation(project(mapOf("path" to ":opendc-experiments:opendc-experiments-base")))

    runtimeOnly(projects.opendcTrace.opendcTraceOpendc)
    runtimeOnly(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j)
}

val createScenarioApp by tasks.creating(CreateStartScripts::class) {
    dependsOn(tasks.jar)

    applicationName = "scenario"
    mainClass.set("org.opendc.experiments.scenario.ScnearioCLI")
    classpath = tasks.jar.get().outputs.files + configurations["runtimeClasspath"]
    outputDir = project.buildDir.resolve("scripts")
}

/* Create custom Greenifier distribution */
distributions {
    main {
        distributionBaseName.set("scenario")

        contents {
            from("README.md")
            from("LICENSE.txt")
            from("../../LICENSE.txt") {
                rename { "LICENSE-OpenDC.txt" }
            }

            into("bin") {
                from(createScenarioApp)
            }

            into("lib") {
                from(tasks.jar)
                from(configurations["runtimeClasspath"])
            }

            into("resources") {
                from("src/main/resources")
            }

            into("Python_scripts") {
                from("src/main/Python_scripts")
            }
        }
    }
}
