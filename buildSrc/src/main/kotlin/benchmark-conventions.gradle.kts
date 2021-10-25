/*
 * Copyright (c) 2021 AtLarge Research
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

import me.champeau.jmh.JMHTask
import org.jetbrains.kotlin.allopen.gradle.*

plugins {
    `java-library`
    kotlin("plugin.allopen")
    id("me.champeau.jmh")
}

configure<AllOpenExtension> {
    annotation("org.openjdk.jmh.annotations.State")
}

jmh {
    jmhVersion.set("1.33")

    profilers.add("stack")
    profilers.add("gc")

    includeTests.set(false) // Do not include tests by default
}

tasks.named("jmh", JMHTask::class) {
    outputs.upToDateWhen { false } // XXX Do not cache the output of this task

    testRuntimeClasspath.setFrom() // XXX Clear test runtime classpath to eliminate duplicate dependencies on classpath
}

dependencies {
    constraints {
        val libs = Libs(project)
        jmh(libs["commons.math3"]) // XXX Force JMH to use the same commons-math3 version as OpenDC
    }
}
