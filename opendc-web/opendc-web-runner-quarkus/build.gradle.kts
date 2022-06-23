/*
 * Copyright (c) 2022 AtLarge Research
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

description = "Quarkus extension for the OpenDC experiment runner"

plugins {
    `java-library-conventions`
    id("io.quarkus.extension")
}

quarkusExtension {
    deploymentModule.set("opendc-web-runner-quarkus-deployment")
}

dependencies {
    modules {
        module("javax.annotation:javax.annotation-api") {
            replacedBy("jakarta.annotation:jakarta.annotation-api", "javax has been replaced by Jakarta")
        }
    }

    api(projects.opendcWeb.opendcWebRunner)

    implementation(platform(libs.quarkus.bom))
    implementation(libs.quarkus.core.runtime)
}

evaluationDependsOn(projects.opendcWeb.opendcWebRunnerQuarkusDeployment.dependencyProject.path)
