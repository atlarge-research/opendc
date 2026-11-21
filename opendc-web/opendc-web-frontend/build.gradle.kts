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

import com.github.gradle.node.pnpm.task.PnpmTask

description = "Modern web interface for OpenDC (Next.js, TypeScript, Mantine)"

plugins {
    base
    id("com.github.node-gradle.node")
}

node {
    download.set(true)
    version.set(libs.versions.node.get())
    pnpmVersion.set(libs.versions.pnpm.get())
}

val install = tasks.named("pnpmInstall")

val sharedInputs: PnpmTask.() -> Unit = {
    dependsOn(install)
    inputs.dir("src")
    inputs.files("package.json", "pnpm-lock.yaml", "tsconfig.json")
}

val nextBuild =
    tasks.register<PnpmTask>("nextBuild") {
        group = "build"
        description = "Build the Next.js static export"
        args.set(listOf("run", "build"))
        sharedInputs()
        inputs.dir("public")
        inputs.files("next.config.ts", "postcss.config.mjs")
        outputs.dir(layout.projectDirectory.dir("out"))
    }

val test =
    tasks.register<PnpmTask>("vitest") {
        group = "verification"
        description = "Run the Vitest suite"
        args.set(listOf("run", "test"))
        sharedInputs()
        inputs.files("vitest.config.ts")
        outputs.upToDateWhen { true }
    }

val lint =
    tasks.register<PnpmTask>("biomeCheck") {
        group = "verification"
        description = "Lint and format-check with Biome"
        args.set(listOf("run", "lint"))
        sharedInputs()
        inputs.files("biome.json")
        outputs.upToDateWhen { true }
    }

tasks.register<PnpmTask>("nextDev") {
    group = "application"
    description = "Run the Next.js dev server"
    args.set(listOf("run", "dev"))
    dependsOn(install)
}

tasks.named("assemble") { dependsOn(nextBuild) }

tasks.named("check") { dependsOn(test, lint) }
