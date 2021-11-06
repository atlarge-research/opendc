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

/* Create sourceSet for CloudSim comparison experiments */
val cmp: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

val cmpImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["implementation"])
}

val cmpRuntimeClasspath: Configuration by configurations.getting {
    extendsFrom(configurations["runtimeClasspath"])
}

val cmpJar by tasks.creating(Jar::class) {
    from(cmp.output)

    archiveBaseName.set("${project.name}-perf")
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
    implementation(libs.jenetics.main)
    implementation(libs.jenetics.ext)
    implementation(libs.jackson.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.opentelemetry.semconv)

    runtimeOnly(projects.opendcTrace.opendcTraceOpendc)
    runtimeOnly(libs.log4j.slf4j)

    cmpImplementation("org.cloudsimplus:cloudsim-plus:7.1.1") {
        exclude(group = "ch.qos.logback")
    }
}

val createRadiceApp by tasks.creating(CreateStartScripts::class) {
    dependsOn(tasks.jar)

    applicationName = "radice"
    mainClass.set("org.opendc.experiments.radice.RadiceCli")
    classpath = tasks.jar.get().outputs.files + configurations["runtimeClasspath"]
    outputDir = project.buildDir.resolve("scripts")
}

val createRadicePerfApp by tasks.creating(CreateStartScripts::class) {
    dependsOn(cmpJar)
    dependsOn(tasks.jar)

    applicationName = "radice-perf"
    mainClass.set("org.opendc.experiments.radice.comparison.RadicePerfCli")
    classpath = tasks.jar.get().outputs.files + cmpJar.outputs.files + cmpRuntimeClasspath
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
            from("05-performance.ipynb")
            from("06-validation.ipynb")
            from("electricity-price.csv")
            from("co2-price.csv")

            into("bin") {
                from(createRadiceApp)
                from(createRadicePerfApp)
            }

            into("lib") {
                from(tasks.jar)
                from(cmpJar)
                from(cmpRuntimeClasspath) // Also includes main classpath
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
