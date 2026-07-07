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

package org.opendc.sdk.model.serialization

import org.junit.jupiter.api.Test
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.failure.CustomFailure
import org.opendc.sdk.model.failure.ExponentialDistribution
import org.opendc.sdk.model.failure.LogNormalDistribution
import org.opendc.sdk.model.failure.UniformDistribution
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicy
import org.opendc.sdk.model.workload.InlineWorkload
import org.opendc.sdk.model.workload.TraceWorkload
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies that the hand-written SDK JSON documents under `resources/scenarios` and
 * `resources/experiments` decode through [SdkJson] and pass model validation, and that the
 * jsonb (database) code path round-trips an [Experiment] purely in memory.
 */
class JsonFixtureTest {
    @Test
    fun `inline-topology trace-workload scenario decodes and validates`() {
        val scenario = decodeScenario("/scenarios/inline-topology-trace-workload.json")

        assertTrue(scenario.topology.clusters.isNotEmpty(), "expected an inline topology with clusters")
        assertIs<TraceWorkload>(scenario.workload)
        assertEquals(emptyList(), scenario.validate())
    }

    @Test
    fun `inline-workload scenario decodes and validates`() {
        val scenario = decodeScenario("/scenarios/inline-workload.json")

        assertTrue(scenario.topology.clusters.isNotEmpty(), "expected an inline topology with clusters")
        val workload = assertIs<InlineWorkload>(scenario.workload)
        assertEquals(2, workload.tasks.size)
        assertIs<FilterAllocationPolicy>(scenario.allocationPolicy)
        assertTrue(scenario.validate().isEmpty(), "expected no validation issues")
    }

    @Test
    fun `timeshift taskstopper experiment decodes and validates`() {
        val experiment = decodeExperiment("/experiments/timeshift-taskstopper.json")

        val policy = assertIs<TimeShiftAllocationPolicy>(experiment.allocationPolicies.single())
        assertNotNull(policy.taskStopper, "expected an embedded task stopper")
        assertTrue(experiment.validate().isEmpty(), "expected no validation issues")
    }

    @Test
    fun `custom-failure distributions experiment decodes and validates`() {
        val experiment = decodeExperiment("/experiments/custom-failure-distributions.json")

        val failure = assertIs<CustomFailure>(experiment.failureModels.single())
        assertIs<ExponentialDistribution>(failure.interArrival)
        assertIs<LogNormalDistribution>(failure.duration)
        assertIs<UniformDistribution>(failure.hostFraction)
        assertTrue(experiment.validate().isEmpty(), "expected no validation issues")
    }

    @Test
    fun `experiments round-trip through json element without a filesystem`() {
        val fixtures =
            listOf(
                "/experiments/timeshift-taskstopper.json",
                "/experiments/custom-failure-distributions.json",
            )

        for (path in fixtures) {
            val experiment = decodeExperiment(path)
            val restored = SdkJson.fromJsonElement(SdkJson.toJsonElement(experiment))
            assertEquals(experiment, restored, "jsonb round-trip changed the experiment for $path")
        }
    }

    private fun decodeScenario(path: String): Scenario = resource(path).use { SdkJson.decodeScenario(it) }

    private fun decodeExperiment(path: String): Experiment = resource(path).use { SdkJson.decodeExperiment(it) }

    private fun resource(path: String): InputStream = requireNotNull(javaClass.getResourceAsStream(path)) { "missing test resource $path" }
}
