/*
 * Copyright (c) 2020 AtLarge Research
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
    application
}

application {
    mainClass.set("org.opendc.runner.web.MainKt")
}

dependencies {
    api(platform(projects.opendcPlatform))
    implementation(projects.opendcCompute.opendcComputeSimulator)
    implementation(projects.opendcFormat)
    implementation(projects.opendcExperiments.opendcExperimentsCapelin)
    implementation(projects.opendcSimulator.opendcSimulatorCore)
    implementation(projects.opendcTelemetry.opendcTelemetrySdk)

    implementation("io.github.microutils:kotlin-logging")
    implementation("com.github.ajalt.clikt:clikt:${versions["clikt"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${versions["jackson-module-kotlin"]}")
    implementation("io.sentry:sentry-log4j2:${versions["sentry-log4j2"]}")
    implementation("org.mongodb:mongodb-driver-sync:${versions["mongodb-driver-sync"]}")

    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:${versions.log4j}")
    runtimeOnly("org.apache.logging.log4j:log4j-1.2-api:${versions.log4j}")
}
