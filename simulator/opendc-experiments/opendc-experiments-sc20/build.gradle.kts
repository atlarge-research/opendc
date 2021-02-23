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

description = "Experiments for the SC20 paper"

/* Build configuration */
plugins {
    `kotlin-library-conventions`
    `testing-conventions`
    application
}

application {
    mainClass.set("org.opendc.harness.runner.console.ConsoleRunnerKt")
    applicationDefaultJvmArgs = listOf("-Xms2500M")
}

dependencies {
    api(project(":opendc-core"))
    api(project(":opendc-harness"))
    implementation(project(":opendc-format"))
    implementation(project(":opendc-simulator:opendc-simulator-core"))
    implementation(project(":opendc-simulator:opendc-simulator-compute"))
    implementation(project(":opendc-simulator:opendc-simulator-failures"))
    implementation(project(":opendc-compute:opendc-compute-simulator"))

    implementation("io.github.microutils:kotlin-logging:${versions.kotlinLogging}")
    implementation("me.tongfei:progressbar:0.9.0")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")

    implementation("org.apache.parquet:parquet-avro:1.11.0")
    implementation("org.apache.hadoop:hadoop-client:3.2.1") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j")
    }
}
