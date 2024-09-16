/*
 * Copyright (c) 2022 AtLarge Research
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

description = "Support library for simulating VM-based workloads with OpenDC"

// Build configuration
plugins {
    `kotlin-library-conventions`
    `testing-conventions`
    `jacoco-conventions`
    distribution
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {

    api(projects.opendcCompute.opendcComputeSimulator)

    implementation(libs.clikt)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation(libs.progressbar)
    implementation(project(mapOf("path" to ":opendc-simulator:opendc-simulator-core")))

    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-workload")))
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-topology")))
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-carbon")))
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-failure")))

    runtimeOnly(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j)
}

val createScenarioApp by tasks.creating(CreateStartScripts::class) {
    dependsOn(tasks.jar)

    applicationName = "OpenDCScenarioRunner"
    mainClass.set("org.opendc.experiments.base.runner.ScenarioCli")
    classpath = tasks.jar.get().outputs.files + configurations["runtimeClasspath"]
    outputDir = project.buildDir.resolve("scripts")
}

// Create custom Scenario distribution
distributions {
    main {
        distributionBaseName.set("OpenDCScenarioRunner")

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
        }
    }
}
