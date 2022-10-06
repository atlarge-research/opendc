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

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.VerificationType
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType

plugins {
    id("kotlin-conventions")
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
    id("testing-conventions")
    id("io.quarkus")
}

/* Mark necessary classes as open in Kotlin */
allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
    annotation("javax.persistence.Entity")
}

/* Include metadata for method parameters */
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.javaParameters = true
}

/* Launch Quarkus dev mode from project root directory */
tasks.quarkusDev {
    workingDirectory.set(rootProject.projectDir)
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
