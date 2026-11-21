/*
 * Copyright (c) 2026 AtLarge Research
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

description = "The process that runs one bag of simulation work"

plugins {
    `kotlin-conventions`
    `testing-conventions`
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    applicationName = "opendc-launcher"
    mainClass.set("org.opendc.web.launcher.MainKt")
    // A JVM that has run out of memory is in no state to report anything, and its exit code is
    // what tells every dispatcher to retry with more.
    applicationDefaultJvmArgs = listOf("-XX:+ExitOnOutOfMemoryError")
}

dependencies {
    // Deliberately dependency-light: the simulator, the model, and the JDK's own HTTP client.
    // Anything added here is staged onto every compute node the launcher ever runs on.
    api(projects.opendcSdk.opendcSdkModel)
    implementation(projects.opendcSdk.opendcSdkRunner)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j)
}
