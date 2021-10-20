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

description = "Experiments for the Risk Analysis work"

/* Build configuration */
plugins {
    `kotlin-conventions`
    `benchmark-conventions`
    distribution
}

dependencies {
    api(projects.opendcHarness.opendcHarnessApi)
    api(projects.opendcCompute.opendcComputeWorkload)

    implementation(projects.opendcSimulator.opendcSimulatorCore)
    implementation(projects.opendcTrace.opendcTraceApi)
    implementation(projects.opendcTrace.opendcTraceParquet)
    implementation(projects.opendcTelemetry.opendcTelemetryCompute)
    implementation(projects.opendcTelemetry.opendcTelemetryRisk)

    implementation(libs.config)
    implementation(libs.clikt)
    implementation(libs.progressbar)
    implementation(libs.kotlin.logging)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.jackson.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jenetics.main)
    implementation(libs.jenetics.ext)

    runtimeOnly(projects.opendcTrace.opendcTraceOpendc)
    runtimeOnly(libs.log4j.slf4j)
}

val createRadiceApp by tasks.creating(CreateStartScripts::class) {
    dependsOn(tasks.jar)

    applicationName = "radice"
    mainClass.set("org.opendc.experiments.radice.RadiceCli")
    classpath = tasks.jar.get().outputs.files + configurations["runtimeClasspath"]
    outputDir = project.buildDir.resolve("scripts")
}

/* Create custom Radice distribution */
distributions {
    main {
        distributionBaseName.set("radice")

        contents {
            from("README.md")
            from("LICENSE.txt")
            from("../../LICENSE.txt") {
                rename { "LICENSE-OpenDC.txt" }
            }

            from("radice.py")
            from("01-sustainability.ipynb")
            from("02-phenomena.ipynb")
            from("03-regret.ipynb")
            from("04-scheduler.ipynb")
            from("electricity-price.csv")
            from("co2-price.csv")

            into("bin") {
                from(createRadiceApp)
            }

            into("lib") {
                from(tasks.jar)
            }

            into("traces") {
                from("traces")
            }

            into("portfolios") {
                from("portfolios")
            }
        }
    }
}
