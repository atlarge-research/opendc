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
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * This class makes the version catalog accessible for the build scripts until Gradle adds support for it.
 *
 * See https://github.com/gradle/gradle/issues/15383
 */
public class Libs(project: Project) {
    /**
     * The version catalog of the project.
     */
    private val versionCatalog = project.extensions.getByType(VersionCatalogsExtension::class).named("libs")

    /**
     * Obtain the version for the specified [dependency][name].
     */
    operator fun get(name: String): String {
        val dep = versionCatalog.findLibrary(name).get().get()
        return "${dep.module.group}:${dep.module.name}:${dep.versionConstraint.displayName}"
    }

    companion object {
        /**
         * The JVM version to target.
         */
        val jvmTarget = JavaVersion.VERSION_11
    }
}
