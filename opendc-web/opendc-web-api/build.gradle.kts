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

description = "REST API for the OpenDC website"

/* Build configuration */
plugins {
    `kotlin-conventions`
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
    `testing-conventions`
    `jacoco-conventions`
    id("io.quarkus")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))

    implementation(projects.opendcWeb.opendcWebProto)

    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.resteasy.core)
    implementation(libs.quarkus.resteasy.jackson)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.quarkus.smallrye.openapi)

    implementation(libs.quarkus.security)
    implementation(libs.quarkus.oidc)

    implementation(libs.quarkus.hibernate.orm)
    implementation(libs.quarkus.hibernate.validator)
    implementation(libs.quarkus.hibernate.types)
    implementation(libs.quarkus.jdbc.postgresql)
    quarkusDev(libs.quarkus.jdbc.h2)

    testImplementation(libs.quarkus.junit5.core)
    testImplementation(libs.quarkus.junit5.mockk)
    testImplementation(libs.restassured.core)
    testImplementation(libs.restassured.kotlin)
    testImplementation(libs.quarkus.test.security)
    testImplementation(libs.quarkus.jdbc.h2)
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
    annotation("javax.persistence.Entity")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.javaParameters = true
}

tasks.quarkusDev {
    workingDir = rootProject.projectDir.toString()
}

tasks.test {
    extensions.configure(JacocoTaskExtension::class) {
        excludeClassLoaders = listOf("*QuarkusClassLoader")
        destinationFile = layout.buildDirectory.file("jacoco-quarkus.exec").get().asFile
    }
}

/* Fix for Quarkus/ktlint-gradle incompatibilities */
tasks.named("runKtlintCheckOverMainSourceSet").configure {
    mustRunAfter(tasks.quarkusGenerateCode)
    mustRunAfter(tasks.quarkusGenerateCodeDev)
}

tasks.named("runKtlintCheckOverTestSourceSet").configure {
    mustRunAfter(tasks.quarkusGenerateCodeTests)
}
