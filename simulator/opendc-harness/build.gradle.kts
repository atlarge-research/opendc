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

description = "Harness for defining repeatable experiments using OpenDC"

/* Build configuration */
plugins {
    `kotlin-library-convention`
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Library.KOTLINX_COROUTINES}")
    api("org.junit.platform:junit-platform-commons:${Library.JUNIT_PLATFORM}")
    implementation("io.github.classgraph:classgraph:4.8.98")
    implementation("me.tongfei:progressbar:0.9.0")
    implementation("io.github.microutils:kotlin-logging:2.0.4")

    api("org.junit.platform:junit-platform-engine:${Library.JUNIT_PLATFORM}")
    api("org.junit.platform:junit-platform-suite-api:${Library.JUNIT_PLATFORM}")
    api("org.junit.platform:junit-platform-launcher:${Library.JUNIT_PLATFORM}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Library.JUNIT_JUPITER}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Library.JUNIT_JUPITER}")
}
