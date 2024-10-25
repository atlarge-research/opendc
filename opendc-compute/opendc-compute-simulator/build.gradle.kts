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

description = "Simulator for OpenDC Compute"

// Build configuration
plugins {
    `kotlin-library-conventions`
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    api(projects.opendcSimulator.opendcSimulatorCompute)
    api(projects.opendcTrace.opendcTraceParquet)
    api(libs.commons.math3)
    implementation(projects.opendcCommon)
    implementation(libs.kotlin.logging)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    api(libs.microprofile.config)
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-topology")))
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-carbon")))

    implementation(project(mapOf("path" to ":opendc-trace:opendc-trace-api")))
    implementation(project(mapOf("path" to ":opendc-trace:opendc-trace-parquet")))

    testImplementation(projects.opendcSimulator.opendcSimulatorCore)
    testRuntimeOnly(libs.slf4j.simple)
    testRuntimeOnly(libs.log4j.slf4j)
}
