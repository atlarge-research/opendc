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

package org.opendc.sdk.model.experiment

import org.junit.jupiter.api.Test
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.SchedulerName
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.validHost
import org.opendc.sdk.model.workload.TraceWorkload
import org.opendc.sdk.model.workload.Workload
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartesianTest {
    private fun topology(name: String): TopologySpec = TopologySpec(listOf(ClusterSpec(name = name, hosts = listOf(validHost))))

    private fun workload(name: String): Workload = TraceWorkload(NamedReference(name))

    private fun policy(scheduler: SchedulerName): PrefabAllocationPolicy = PrefabAllocationPolicy(scheduler)

    @Test
    fun `expand yields the product of the varying axis sizes`() {
        val topologies = setOf(topology("t0"), topology("t1"))
        val workloads = setOf(workload("w0"), workload("w1"))
        val policies = setOf(policy(SchedulerName.Mem), policy(SchedulerName.CoreMem), policy(SchedulerName.Random))

        val experiment =
            Experiment(
                topologies = topologies,
                workloads = workloads,
                allocationPolicies = policies,
            )

        val scenarios = experiment.expand()

        assertEquals(topologies.size * workloads.size * policies.size, scenarios.size)
        assertEquals(12, scenarios.size)
    }

    @Test
    fun `expand produces every combination exactly once`() {
        val topologies = setOf(topology("t0"), topology("t1"))
        val workloads = setOf(workload("w0"), workload("w1"))
        val policies = setOf(policy(SchedulerName.Mem), policy(SchedulerName.CoreMem), policy(SchedulerName.Random))

        val experiment =
            Experiment(
                topologies = topologies,
                workloads = workloads,
                allocationPolicies = policies,
            )

        val scenarios = experiment.expand()

        val expected =
            topologies.flatMap { t ->
                workloads.flatMap { w ->
                    policies.map { p -> Triple(t, w, p) }
                }
            }.toSet()
        val observed = scenarios.map { Triple(it.topology, it.workload, it.allocationPolicy) }

        assertEquals(expected, observed.toSet())
        assertEquals(observed.size, observed.toSet().size)
    }

    @Test
    fun `expand propagates runs and initialSeed to every scenario`() {
        val experiment =
            Experiment(
                topologies = setOf(topology("t0"), topology("t1")),
                workloads = setOf(workload("w0"), workload("w1")),
                allocationPolicies = setOf(policy(SchedulerName.Mem), policy(SchedulerName.CoreMem)),
                runs = 7,
                initialSeed = 42,
            )

        val scenarios = experiment.expand()

        assertTrue(scenarios.all { it.runs == 7 })
        assertTrue(scenarios.all { it.initialSeed == 42 })
    }

    @Test
    fun `expand assigns sequential ids and matching names from zero`() {
        val experiment =
            Experiment(
                topologies = setOf(topology("t0"), topology("t1")),
                workloads = setOf(workload("w0"), workload("w1")),
                allocationPolicies = setOf(policy(SchedulerName.Mem), policy(SchedulerName.CoreMem), policy(SchedulerName.Random)),
            )

        val scenarios = experiment.expand()

        assertEquals((0 until scenarios.size).toList(), scenarios.map { it.id })
        assertEquals(scenarios.map { it.id.toString() }, scenarios.map { it.name })
    }

    @Test
    fun `expand scales the product when an additional axis varies`() {
        val topologies = setOf(topology("t0"), topology("t1"))
        val workloads = setOf(workload("w0"), workload("w1"))
        val policies = setOf(policy(SchedulerName.Mem), policy(SchedulerName.CoreMem), policy(SchedulerName.Random))
        val maxFailures = setOf(5, 10)

        val experiment =
            Experiment(
                topologies = topologies,
                workloads = workloads,
                allocationPolicies = policies,
                maxNumFailures = maxFailures,
            )

        val scenarios = experiment.expand()

        assertEquals(topologies.size * workloads.size * policies.size * maxFailures.size, scenarios.size)

        val tuples =
            scenarios.map {
                listOf(it.topology, it.workload, it.allocationPolicy, it.maxNumFailures)
            }
        assertEquals(tuples.size, tuples.toSet().size)
        assertEquals(maxFailures, scenarios.map { it.maxNumFailures }.toSet())
    }

    @Test
    fun `expand of single-valued axes yields one scenario with id zero`() {
        val experiment =
            Experiment(
                topologies = setOf(topology("t0")),
                workloads = setOf(workload("w0")),
            )

        val scenarios = experiment.expand()

        assertEquals(1, scenarios.size)
        assertEquals(0, scenarios.single().id)
        assertEquals("0", scenarios.single().name)
    }
}
