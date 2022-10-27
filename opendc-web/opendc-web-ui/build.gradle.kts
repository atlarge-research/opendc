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

import com.github.gradle.node.npm.task.NpmTask

description = "Web interface for OpenDC"

plugins {
    `java-conventions`
    id("com.github.node-gradle.node")
}

sourceSets {
    main {
        java.srcDir("src")
    }
    test {
        java.srcDir("test")
    }
}

node {
    download.set(true)
    version.set(libs.versions.node.get())
}

val formatTask = tasks.register<NpmTask>("prettierFormat") {
    group = "formatting"
    description = "Use Prettier to format the JavaScript codebase"

    args.set(listOf("run", "format"))
    dependsOn(tasks.npmInstall)
    inputs.dir("src")
    inputs.files("package.json", "next.config.js", ".prettierrc.yaml")
    outputs.upToDateWhen { true }
}

val lintTask = tasks.register<NpmTask>("nextLint") {
    group = "verification"
    description = "Use ESLint to check for problems"

    args.set(listOf("run", "lint"))
    dependsOn(tasks.npmInstall)
    inputs.dir("src")
    inputs.files("package.json", "next.config.js", ".eslintrc")
    outputs.upToDateWhen { true }
}

tasks.register<NpmTask>("nextDev") {
    group = "build"
    description = "Run the Next.js project in development mode"

    args.set(listOf("run", "dev"))
    dependsOn(tasks.npmInstall)
    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("package.json", "next.config.js")
    outputs.upToDateWhen { true }
}

val buildTask = tasks.register<NpmTask>("nextBuild") {
    group = "build"
    description = "Build the Next.js project"

    args.set(listOf("run", "build"))

    val env = listOf(
        "NEXT_PUBLIC_API_BASE_URL",
        "NEXT_PUBLIC_SENTRY_DSN",
        "NEXT_PUBLIC_AUTH0_DOMAIN",
        "NEXT_PUBLIC_AUTH0_CLIENT_ID",
        "NEXT_PUBLIC_AUTH0_AUDIENCE"
    )
    for (envvar in env) {
        environment.put(envvar, "%%$envvar%%")
    }

    dependsOn(tasks.npmInstall)
    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("package.json", "next.config.js")
    outputs.dir(layout.buildDirectory.dir("next"))
}

tasks.register<NpmTask>("nextStart") {
    group = "build"
    description = "Build the Next.js project"

    args.set(listOf("run", "start"))

    dependsOn(buildTask)
    dependsOn(tasks.npmInstall)

    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("package.json", "next.config.js")
    outputs.upToDateWhen { true }
}

tasks.processResources {
    dependsOn(buildTask)
    inputs.dir(project.fileTree("public"))

    from(layout.buildDirectory.dir("next")) {
        include("routes-manifest.json")
        into("META-INF/resources/${project.name}")
    }

    from(layout.buildDirectory.dir("next/static")) {
        into("META-INF/resources/${project.name}/static/_next/static")
    }

    from(layout.buildDirectory.dir("next/server/pages")) {
        include("**/*.html")
        into("META-INF/resources/${project.name}/pages")
    }

    from(project.fileTree("public")) {
        into("META-INF/resources/${project.name}/static")
    }
}
