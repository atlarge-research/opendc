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

import com.github.gradle.node.yarn.task.YarnTask

description = "Web interface for OpenDC"

plugins {
    java
    id("com.github.node-gradle.node")
}

val lintTask = tasks.register<YarnTask>("lintNext") {
    args.set(listOf("lint"))
    dependsOn(tasks.yarn)
    inputs.dir("src")
    inputs.files("package.json", "next.config.js", ".eslintrc")
    outputs.upToDateWhen { true }
}

val buildTask = tasks.register<YarnTask>("buildNext") {
    args.set(listOf("build"))
    dependsOn(tasks.yarn)
    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("package.json", "next.config.js")
    outputs.dir("${project.buildDir}/build")
}

tasks.register<YarnTask>("dev") {
    args.set(listOf("dev"))
    dependsOn(tasks.yarn)
    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("package.json", "next.config.js")
    outputs.upToDateWhen { true }
}

tasks.register<YarnTask>("start") {
    args.set(listOf("start"))
    dependsOn(buildTask)
    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("package.json", "next.config.js")
    outputs.upToDateWhen { true }
}

sourceSets {
    java {
        main {
            java.srcDir("src")
            resources.srcDir("public")
        }

        test {
            java.srcDir("test")
        }
    }
}

tasks.test {
    dependsOn(lintTask)
}
