/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * This class contains the versions of the dependencies shared by the different
 * subprojects.
 */
public class Versions(private val project: Project) {
    /**
     * A delegate for obtaining configuration values from a [Project] instance.
     */
    private fun version(name: String? = null): ReadOnlyProperty<Versions, String> =
        ReadOnlyProperty { _, property -> get(name ?: property.name) }

    val junitJupiter by version(name = "junit-jupiter")
    val junitPlatform by version(name = "junit-platform")
    val mockk by version()

    val slf4j by version()
    val kotlinLogging by version(name = "kotlin-logging")
    val log4j by version()

    val kotlinxCoroutines by version(name = "kotlinx-coroutines")


    /**
     * Obtain the version for the specified [dependency][name].
     */
    operator fun get(name: String) = project.extra.get("$name.version") as String

    companion object {
        /**
         * The JVM version to target.
         */
        val jvmTarget = JavaVersion.VERSION_1_8
    }
}
