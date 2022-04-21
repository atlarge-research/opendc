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
    id("io.quarkus")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))

    implementation(projects.opendcWeb.opendcWebProto)
    compileOnly(projects.opendcWeb.opendcWebUiQuarkus.deployment) /* Temporary fix for Quarkus/Gradle issues */
    implementation(projects.opendcWeb.opendcWebUiQuarkus.runtime)

    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.resteasy.core)
    implementation(libs.quarkus.resteasy.jackson)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.quarkus.smallrye.openapi)

    implementation(libs.quarkus.security)
    implementation(libs.quarkus.oidc)

    implementation(libs.quarkus.hibernate.orm)
    implementation(libs.quarkus.hibernate.validator)
    implementation(libs.quarkus.jdbc.postgresql)
    quarkusDev(libs.quarkus.jdbc.h2)

    testImplementation(libs.quarkus.junit5.core)
    testImplementation(libs.quarkus.junit5.mockk)
    testImplementation(libs.quarkus.jacoco)
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

/* Make sure the jacoco-report-aggregation plugin picks up the Quarkus coverage data */
configurations.create("coverageDataElementsForQuarkus") {
    isVisible = false
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["implementation"], configurations["runtimeOnly"])

    @Suppress("UnstableApiUsage")
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.VERIFICATION))
        attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType::class.java, VerificationType.JACOCO_RESULTS))
    }

    artifacts {
        add("coverageDataElementsForQuarkus", layout.buildDirectory.file("jacoco-quarkus.exec")) {
            @Suppress("UnstableApiUsage")
            type = ArtifactTypeDefinition.BINARY_DATA_TYPE
            builtBy(tasks.test)
        }
    }
}

/* Fix for Quarkus/ktlint-gradle incompatibilities */
tasks.named("runKtlintCheckOverMainSourceSet") {
    mustRunAfter(tasks.quarkusGenerateCode)
    mustRunAfter(tasks.quarkusGenerateCodeDev)
}

tasks.named("runKtlintCheckOverTestSourceSet") {
    mustRunAfter(tasks.quarkusGenerateCodeTests)
}
