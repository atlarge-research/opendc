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
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Obtain the default [VersionCatalog] of the project.
 */
public val Project.defaultVersionCatalog: VersionCatalog
    get() = project.extensions.getByType(VersionCatalogsExtension::class).named("libs")

/**
 * Obtain the dependency string for the specified [name].
 */
public operator fun VersionCatalog.get(name: String): String {
    val dep = findLibrary(name).get().get()
    return "${dep.module.group}:${dep.module.name}:${dep.versionConstraint.displayName}"
}

/**
 * Obtain the string representation of the version with the given [name].
 */
public fun VersionCatalog.getVersion(name: String): String {
    return findVersion(name).get().displayName
}
