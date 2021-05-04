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

package org.opendc.harness.engine.internal

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.engine.discovery.Discovery
import org.opendc.harness.engine.discovery.DiscoveryFilter
import org.opendc.harness.engine.discovery.DiscoveryRequest
import org.opendc.harness.engine.discovery.DiscoverySelector

/**
 * A [Discovery] implementation that discovers [Experiment] instances on the classpath.
 */
internal class DslDiscovery : Discovery {
    /*
     * Lazily memoize the results of the classpath scan.
     */
    private val scanResult by lazy { scan() }

    override fun discover(request: DiscoveryRequest): Flow<ExperimentDefinition> {
        return findExperiments()
            .map { cls ->
                val exp = cls.constructors[0].newInstance() as Experiment
                exp.toDefinition()
            }
            .filter(select(request.selectors))
            .filter(filter(request.filters))
            .asFlow()
    }

    /**
     * Find the classes on the classpath implementing the [Experiment] class.
     */
    private fun findExperiments(): Sequence<Class<out Experiment>> {
        return scanResult
            .getSubclasses(Experiment::class.java.name)
            .filter { !(it.isAbstract || it.isInterface) }
            .map { it.loadClass() }
            .filterIsInstance<Class<out Experiment>>()
            .asSequence()
    }

    /**
     * Create a predicate for filtering the experiments based on the specified [filters].
     */
    private fun filter(filters: List<DiscoveryFilter>): (ExperimentDefinition) -> Boolean = { def ->
        filters.isEmpty() || filters.all { it.test(def) }
    }

    /**
     * Create a predicate for selecting the experiments based on the specified [selectors].
     */
    private fun select(selectors: List<DiscoverySelector>): (ExperimentDefinition) -> Boolean = { def ->
        selectors.isEmpty() || selectors.any { it.test(def) }
    }

    /**
     * Scan the classpath using [ClassGraph].
     */
    private fun scan(): ScanResult {
        return ClassGraph()
            .enableClassInfo()
            .enableExternalClasses()
            .ignoreClassVisibility()
            .rejectPackages(
                "java.*",
                "javax.*",
                "sun.*",
                "com.sun.*",
                "kotlin.*",
                "androidx.*",
                "org.jetbrains.kotlin.*",
                "org.junit.*"
            ).scan()
    }
}
