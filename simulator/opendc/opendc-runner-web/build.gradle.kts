/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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
    `kotlin-library-convention`
    application
}

application {
    mainClassName = "com.atlarge.opendc.runner.web.MainKt"
}

dependencies {
    api(project(":opendc:opendc-core"))
    implementation(project(":opendc:opendc-compute"))
    implementation(project(":opendc:opendc-format"))
    implementation(project(":opendc:opendc-experiments-sc20"))
    implementation(project(":opendc:opendc-simulator"))

    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("io.github.microutils:kotlin-logging:1.7.10")

    implementation("org.mongodb:mongodb-driver-sync:4.0.5")
    implementation("org.apache.spark:spark-sql_2.12:3.0.0") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j")
    }

    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.13.1")
    runtimeOnly("org.apache.logging.log4j:log4j-1.2-api:2.13.1")
}
