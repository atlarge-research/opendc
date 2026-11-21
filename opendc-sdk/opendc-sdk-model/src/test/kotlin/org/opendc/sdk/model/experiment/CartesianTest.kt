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
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.scheduler.SchedulerPrefabSpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.validHost
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.model.workload.WorkloadSpec
import kotlin.test.assertEquals

class CartesianTest {
    private fun topology(name: String): TopologySpec =
        TopologySpec(
            listOf(
                DataCenterSpec(
                    listOf(
                        ClusterSpec(name = name, hosts = listOf(validHost)),
                    ),
                ),
            ),
        )

    private fun workload(name: String): WorkloadSpec = TraceWorkloadSpec(NamedReference(name))

    private fun policy(scheduler: SchedulerPrefabSpec): PrefabAllocationPolicySpec = PrefabAllocationPolicySpec(scheduler)

    @Test
    fun `expand yields the product of the varying axis sizes`() {
        val topologies = setOf(topology("t0"), topology("t1"))
        val workloads = setOf(workload("w0"), workload("w1"))
        val policies =
            setOf(policy(SchedulerPrefabSpec.Mem), policy(SchedulerPrefabSpec.CoreMem), policy(SchedulerPrefabSpec.ActiveServers))

        val experiment =
            ExperimentSpec(
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
        val policies =
            setOf(policy(SchedulerPrefabSpec.Mem), policy(SchedulerPrefabSpec.CoreMem), policy(SchedulerPrefabSpec.ActiveServers))

        val experiment =
            ExperimentSpec(
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
    fun `expand assigns sequential ids and matching names from zero`() {
        val experiment =
            ExperimentSpec(
                topologies = setOf(topology("t0"), topology("t1")),
                workloads = setOf(workload("w0"), workload("w1")),
                allocationPolicies =
                    setOf(
                        policy(SchedulerPrefabSpec.Mem),
                        policy(SchedulerPrefabSpec.CoreMem),
                        policy(SchedulerPrefabSpec.ActiveServers),
                    ),
            )

        val scenarios = experiment.expand()

        assertEquals((0 until scenarios.size).toList(), scenarios.map { it.id })
        assertEquals(scenarios.map { it.id.toString() }, scenarios.map { it.name })
    }

    @Test
    fun `expand scales the product when an additional axis varies`() {
        val topologies = setOf(topology("t0"), topology("t1"))
        val workloads = setOf(workload("w0"), workload("w1"))
        val policies =
            setOf(policy(SchedulerPrefabSpec.Mem), policy(SchedulerPrefabSpec.CoreMem), policy(SchedulerPrefabSpec.ActiveServers))
        val maxFailures = setOf(5, 10)

        val experiment =
            ExperimentSpec(
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
            ExperimentSpec(
                topologies = setOf(topology("t0")),
                workloads = setOf(workload("w0")),
            )

        val scenarios = experiment.expand()

        assertEquals(1, scenarios.size)
        assertEquals(0, scenarios.single().id)
        assertEquals("0", scenarios.single().name)
    }

    @Test
    fun `expand orders scenarios mixed-radix with topologies most significant`() {
        val t0 = topology("t0")
        val t1 = topology("t1")
        val w0 = workload("w0")
        val w1 = workload("w1")

        val experiment =
            ExperimentSpec(
                topologies = setOf(t0, t1),
                workloads = setOf(w0, w1),
                maxNumFailures = setOf(5, 10),
            )

        val scenarios = experiment.expand()

        // Ordering is contractual (D7, Cartesian.kt KDoc): maxNumFailures varies fastest (least
        // significant), topologies slowest (most significant). A silent reorder would corrupt
        // executions because the flattened index identifies the work shard.
        val observed = scenarios.map { Triple(it.topology, it.workload, it.maxNumFailures) }
        val expected =
            listOf(
                Triple(t0, w0, 5),
                Triple(t0, w0, 10),
                Triple(t0, w1, 5),
                Triple(t0, w1, 10),
                Triple(t1, w0, 5),
                Triple(t1, w0, 10),
                Triple(t1, w1, 5),
                Triple(t1, w1, 10),
            )
        assertEquals(expected, observed)

        // The scenario id is exactly the flattened index — the work-shard identity under Indexed
        // Jobs / SLURM arrays (SCENARIO_INDEX selects expand()[index]).
        assertEquals((0 until 8).toList(), scenarios.map { it.id })
    }
}
