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

plugins {
    distribution
    id("kotlin-conventions")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    runtimeOnly(project(":opendc-harness:opendc-harness-junit5"))
}

tasks.register<Test>("experiment") {
    // Ensure JUnit Platform is used for resolving tests
    useJUnitPlatform()

    description = "Runs OpenDC experiments"
    group = "application"

    testClassesDirs = sourceSets["main"].output.classesDirs
    classpath = sourceSets["main"].runtimeClasspath
}

distributions {
    create("experiment") {
        contents {
            from("README.md")
            from(tasks.shadowJar.get())
        }
    }
}

tasks.shadowJar {
    dependencies {
        // Do not include the JUnit 5 runner in the final shadow jar, since it is only necessary for development
        // inside IDE
        exclude(project(":opendc-harness:opendc-harness-junit5"))
    }
}
