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

description = "Library for reading common data formats for topology simulation"

/* Build configuration */
plugins {
    `kotlin-library-conventions`
    `testing-conventions`
    `jacoco-conventions`
}

dependencies {
    api(platform(projects.opendcPlatform))
    api(projects.opendcCompute.opendcComputeApi)
    api(projects.opendcWorkflow.opendcWorkflowApi)
    implementation(projects.opendcSimulator.opendcSimulatorCompute)
    implementation(projects.opendcCompute.opendcComputeSimulator)
    api(libs.jackson.module.kotlin) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    implementation(kotlin("reflect"))
    implementation(libs.parquet)
    implementation(libs.hadoop.client) {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j")
    }

    testRuntimeOnly(libs.slf4j.simple)
}
