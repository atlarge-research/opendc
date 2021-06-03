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

description = "Experiments for the Capelin work"

/* Build configuration */
plugins {
    `experiment-conventions`
    `testing-conventions`
}

dependencies {
    api(platform(projects.opendcPlatform))
    api(projects.opendcHarness.opendcHarnessApi)
    implementation(projects.opendcFormat)
    implementation(projects.opendcSimulator.opendcSimulatorCore)
    implementation(projects.opendcSimulator.opendcSimulatorCompute)
    implementation(projects.opendcSimulator.opendcSimulatorFailures)
    implementation(projects.opendcCompute.opendcComputeSimulator)
    implementation(projects.opendcTelemetry.opendcTelemetrySdk)

    implementation(libs.kotlin.logging)
    implementation(libs.config)
    implementation(libs.progressbar)
    implementation(libs.clikt)

    implementation(libs.parquet)
    implementation(libs.hadoop.client) {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j")
    }

    testImplementation(libs.log4j.slf4j)
}
