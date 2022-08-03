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

description = "Web server of OpenDC"

/* Build configuration */
plugins {
    `quarkus-conventions`
    distribution
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))

    implementation(projects.opendcWeb.opendcWebProto)
    compileOnly(projects.opendcWeb.opendcWebUiQuarkusDeployment) /* Temporary fix for Quarkus/Gradle issues */
    compileOnly(projects.opendcWeb.opendcWebRunnerQuarkusDeployment)
    implementation(projects.opendcWeb.opendcWebUiQuarkus)
    implementation(projects.opendcWeb.opendcWebRunnerQuarkus)

    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.resteasy.core)
    implementation(libs.quarkus.resteasy.jackson)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.quarkus.smallrye.openapi)

    implementation(libs.quarkus.security)
    implementation(libs.quarkus.oidc)

    implementation(libs.quarkus.hibernate.orm)
    implementation(libs.quarkus.hibernate.validator)
    implementation(libs.quarkus.flyway)
    implementation(libs.quarkus.jdbc.postgresql)
    implementation(libs.quarkus.jdbc.h2)

    testImplementation(libs.quarkus.junit5.core)
    testImplementation(libs.quarkus.junit5.mockk)
    testImplementation(libs.quarkus.jacoco)
    testImplementation(libs.restassured.core)
    testImplementation(libs.restassured.kotlin)
    testImplementation(libs.quarkus.test.security)
    testImplementation(libs.quarkus.jdbc.h2)
}

val createStartScripts by tasks.creating(CreateStartScripts::class) {
    applicationName = "opendc-server"
    mainClass.set("io.quarkus.bootstrap.runner.QuarkusEntryPoint")
    classpath = files("lib/quarkus-run.jar")
    outputDir = project.buildDir.resolve("scripts")
}

distributions {
    main {
        distributionBaseName.set("opendc")

        contents {
            from("../../LICENSE.txt")
            from("config") {
                into("config")
            }

            from(createStartScripts) {
                into("bin")
            }
            from(tasks.quarkusBuild) {
                into("lib")
            }

            from("../../traces") {
                into("traces")
            }
        }
    }
}
