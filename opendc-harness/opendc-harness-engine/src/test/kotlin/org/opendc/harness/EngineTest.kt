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

package org.opendc.harness

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.engine.ExperimentEngine
import org.opendc.harness.engine.ExperimentEngineLauncher
import org.opendc.harness.engine.ExperimentExecutionListener
import org.opendc.harness.engine.discovery.DiscoveryProvider
import org.opendc.harness.engine.discovery.DiscoveryRequest

/**
 * A test suite for the [ExperimentEngine].
 */
internal class EngineTest {
    @Test
    fun test() {
        val listener = object : ExperimentExecutionListener {}
        ExperimentEngineLauncher()
            .withListener(listener)
            .runBlocking(flowOf(TestExperiment().toDefinition()))
    }

    @Test
    fun discovery() {
        runBlocking {
            val discovery = DiscoveryProvider.findById("dsl")?.create(Thread.currentThread().contextClassLoader)
            assertNotNull(discovery)
            val res = mutableListOf<ExperimentDefinition>()
            discovery?.discover(DiscoveryRequest())?.toList(res)
            println(res)
            assertEquals(1, res.size)
        }
    }
}
