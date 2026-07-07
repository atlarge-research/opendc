/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.validation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.checkpoint.CheckpointModel
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.failure.UniformDistribution
import org.opendc.sdk.model.failure.WeibullDistribution
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.InstanceCountFilter
import org.opendc.sdk.model.scheduler.TaskStopper
import org.opendc.sdk.model.topology.Cluster
import org.opendc.sdk.model.topology.Cpu
import org.opendc.sdk.model.topology.Host
import org.opendc.sdk.model.topology.PowerModel
import org.opendc.sdk.model.topology.Topology
import org.opendc.sdk.model.validExperiment
import org.opendc.sdk.model.validMemory
import org.opendc.sdk.model.validTask
import org.opendc.sdk.model.workload.TraceWorkload
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that [Validatable.validate] reports the expected [ValidationIssue]s for malformed models,
 * never throws, and returns an empty list for well-formed models.
 */
class ValidationTest {
    @Test
    fun `trace workload with non-positive sampleFraction reports sampleFraction`() {
        val workload = TraceWorkload(source = NamedReference("trace"), sampleFraction = 0.0)

        val issues = assertDoesNotThrow { workload.validate() }

        assertEquals("must be greater than zero", issues.messageAt("sampleFraction"))
    }

    @Test
    fun `filter allocation policy with zero subsetSize reports subsetSize`() {
        val policy = FilterAllocationPolicy(subsetSize = 0)

        val issues = assertDoesNotThrow { policy.validate() }

        assertContains(issues.paths(), "subsetSize")
    }

    @Test
    fun `task stopper with out-of-range forecastThreshold reports forecastThreshold`() {
        val stopper = TaskStopper(forecastThreshold = 2.0)

        val issues = assertDoesNotThrow { stopper.validate() }

        assertEquals("must be in 0.0..1.0", issues.messageAt("forecastThreshold"))
    }

    @Test
    fun `instance count filter with zero limit reports limit`() {
        val filter = InstanceCountFilter(limit = 0)

        val issues = assertDoesNotThrow { filter.validate() }

        assertEquals("must be >= 1", issues.messageAt("limit"))
    }

    @Test
    fun `power model with maxPower below idlePower reports maxPower`() {
        val model = PowerModel(maxPower = Power.ofWatts(100), idlePower = Power.ofWatts(200))

        val issues = assertDoesNotThrow { model.validate() }

        assertEquals("must be >= idlePower", issues.messageAt("maxPower"))
    }

    @Test
    fun `experiment with empty topologies and workloads reports both`() {
        val experiment = Experiment(topologies = emptySet(), workloads = emptySet())

        val issues = assertDoesNotThrow { experiment.validate() }

        val paths = issues.paths()
        assertContains(paths, "topologies")
        assertContains(paths, "workloads")
    }

    @Test
    fun `uniform distribution with upper not above lower reports upper`() {
        val distribution = UniformDistribution(upper = 1.0, lower = 1.0)

        val issues = assertDoesNotThrow { distribution.validate() }

        assertEquals("must be > lower", issues.messageAt("upper"))
    }

    @Test
    fun `weibull distribution with non-positive alpha reports alpha`() {
        val distribution = WeibullDistribution(alpha = 0.0, beta = 1.0)

        val issues = assertDoesNotThrow { distribution.validate() }

        assertContains(issues.paths(), "alpha")
    }

    @Test
    fun `checkpoint model with non-positive interval reports interval`() {
        val model = CheckpointModel(interval = TimeDelta.zero)

        val issues = assertDoesNotThrow { model.validate() }

        assertEquals("must be greater than zero", issues.messageAt("interval"))
    }

    @Test
    fun `task with zero cpuCoreCount reports cpuCoreCount`() {
        val task = validTask.copy(cpuCoreCount = 0)

        val issues = assertDoesNotThrow { task.validate() }

        assertEquals(listOf("cpuCoreCount"), issues.paths())
    }

    @Test
    fun `fully valid experiment reports no issues`() {
        val issues = assertDoesNotThrow { validExperiment.validate() }

        assertTrue(issues.isEmpty(), "expected no validation issues but got $issues")
    }

    @Test
    fun `nested issue paths are prefixed down to the offending field`() {
        val topology =
            Topology(
                listOf(
                    Cluster(
                        hosts = listOf(Host(cpu = Cpu(coreCount = 0, coreSpeed = Frequency.ofGHz(3.0)), memory = validMemory)),
                    ),
                ),
            )

        val issues = assertDoesNotThrow { topology.validate() }

        assertContains(issues.paths(), "clusters[0].hosts[0].cpu.coreCount")
        assertTrue(issues.any { it.path.contains("clusters[0].hosts[0].cpu") })
    }

    private fun List<ValidationIssue>.paths(): List<String> = map { it.path }

    private fun List<ValidationIssue>.messageAt(path: String): String? = firstOrNull { it.path == path }?.message
}
