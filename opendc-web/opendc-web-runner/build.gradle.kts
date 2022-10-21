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

description = "Experiment runner for OpenDC"

/* Build configuration */
plugins {
    `kotlin-library-conventions`
    distribution
}

val cli: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

val cliImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["implementation"])
}
val cliRuntimeOnly: Configuration by configurations.getting
val cliRuntimeClasspath: Configuration by configurations.getting {
    extendsFrom(configurations["runtimeClasspath"])
}

val cliJar by tasks.creating(Jar::class) {
    from(cli.output)

    archiveBaseName.set("${project.name}-cli")
}

dependencies {
    api(projects.opendcWeb.opendcWebClient)
    implementation(projects.opendcExperiments.opendcExperimentsCompute)
    implementation(projects.opendcSimulator.opendcSimulatorCore)
    implementation(projects.opendcTrace.opendcTraceApi)

    implementation(libs.kotlin.logging)
    runtimeOnly(projects.opendcTrace.opendcTraceOpendc)
    runtimeOnly(projects.opendcTrace.opendcTraceBitbrains)

    cliImplementation(libs.clikt)

    cliRuntimeOnly(projects.opendcTrace.opendcTraceOpendc)
    cliRuntimeOnly(libs.log4j.core)
    cliRuntimeOnly(libs.log4j.slf4j)
    cliRuntimeOnly(libs.sentry.log4j2)
}

val createCli by tasks.creating(CreateStartScripts::class) {
    dependsOn(cliJar)

    applicationName = "opendc-runner"
    mainClass.set("org.opendc.web.runner.cli.MainKt")
    classpath = cliJar.outputs.files + cliRuntimeClasspath
    outputDir = project.buildDir.resolve("scripts")
}

distributions {
    main {
        contents {
            into("bin") {
                from(createCli)
            }

            into("lib") {
                from(cliJar)
                from(cliRuntimeClasspath) // Also includes main classpath
            }
        }
    }
}
