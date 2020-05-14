/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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
    `kotlin-library-convention`
    application
}

application {
    mainClassName = "com.atlarge.opendc.experiments.sc20.ExperimentRunnerCliKt"
    applicationDefaultJvmArgs = listOf("-Xmx2500M", "-Xms1800M")
}

dependencies {
    api(project(":opendc:opendc-core"))
    implementation(project(":opendc:opendc-format"))
    implementation(kotlin("stdlib"))
    implementation("com.github.ajalt:clikt:2.6.0")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("org.apache.parquet:parquet-avro:1.11.0")
    implementation("org.apache.hadoop:hadoop-client:3.2.1") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j")
    }
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("de.bytefish.pgbulkinsert:pgbulkinsert-core:5.1.0")
    implementation("de.bytefish.pgbulkinsert:pgbulkinsert-rowwriter:5.1.0")
    implementation("me.tongfei:progressbar:0.8.1")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.13.1")
    runtimeOnly("org.postgresql:postgresql:42.2.12")
    runtimeOnly(project(":odcsim:odcsim-engine-omega"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Library.JUNIT_JUPITER}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Library.JUNIT_JUPITER}")
    testImplementation("org.junit.platform:junit-platform-launcher:${Library.JUNIT_PLATFORM}")
}
