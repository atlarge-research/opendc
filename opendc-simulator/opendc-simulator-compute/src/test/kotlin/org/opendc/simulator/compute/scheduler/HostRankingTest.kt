/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.simulator.compute.scheduler

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.compute.scheduler.filters.HostFilter
import org.opendc.simulator.compute.scheduler.filters.ThresholdFilter
import org.opendc.simulator.compute.task.SimTask
import org.opendc.simulator.flow.engine.FlowEngine
import org.opendc.simulator.flow.graph.FlowDistributor
import org.opendc.simulator.flow.graph.distributionPolicies.FlowDistributorFactory
import java.time.Instant
import java.time.InstantSource
import java.util.Random

/**
 * Test suite for the [HostRanking].
 */
internal class HostRankingTest {
    /** The room of every host, by id; only changed together with [HostRanking.put], as the ranking requires. */
    private val room = HashMap<Int, Double>()

    /** The room the next task needs. */
    private var need = 0.0

    private val roomFilter =
        object : ThresholdFilter {
            override fun test(
                host: SimHost,
                task: SimTask,
            ): Boolean = room.getValue(host.id) >= need

            override fun available(host: SimHost): Double = room.getValue(host.id)

            override fun required(task: SimTask): Double = need
        }

    /** A filter the ranking cannot see into. */
    private val oddFilter = HostFilter { host, _ -> host.id % 5 != 0 }

    private val task = mockk<SimTask>()

    private val engine = mockk<FlowEngine>(relaxed = true)
    private val powerModel = mockk<PowerModel>(relaxed = true)
    private val distributor = mockk<FlowDistributor>(relaxed = true)

    /** A real host, because the ranking reads host ids far too often for a mock to record. */
    private fun host(id: Int): SimHost {
        val machine =
            MachineModel(
                CpuModel(0, 4, 2600.0, "vendor", "model", "arch"),
                MemoryUnit("vendor", "model", 3200.0, 4096),
                null,
                FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS,
                FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS,
            )
        return SimHost(id, "H$id", "C01", InstantSource.fixed(Instant.EPOCH), engine, machine, powerModel, null, 0.0, 1.0, distributor)
    }

    @Test
    fun testRandomOperationsKeepTheRankingConsistent() {
        val ranking = HostRanking(listOf(roomFilter, oddFilter), blockSize = 8, numHosts = 4)
        val random = Random(0)
        val hosts = List(150) { host(it) }
        val scores = HashMap<Int, Double>()

        repeat(20_000) { step ->
            val host = hosts[random.nextInt(hosts.size)]
            when (random.nextInt(100)) {
                in 0 until 60 -> {
                    // Few distinct scores, so ties are common
                    val score = random.nextInt(20).toDouble()
                    room[host.id] = random.nextInt(50).toDouble()
                    scores[host.id] = score
                    ranking.put(host, score)
                }
                in 60 until 99 -> {
                    ranking.remove(host)
                    scores.remove(host.id)
                }
                else -> {
                    val newScores = hosts.associate { it.id to random.nextInt(20).toDouble() }
                    ranking.rescore { newScores.getValue(it.id) }
                    scores.replaceAll { id, _ -> newScores.getValue(id) }
                }
            }

            ranking.checkInvariants()

            val expected = scores.keys.sortedWith(compareByDescending<Int> { scores.getValue(it) }.thenBy { it })
            assertEquals(expected, ranking.hosts().map { it.id }) { "Wrong order at step $step" }

            need = random.nextInt(50).toDouble()
            val expectedFit = expected.firstOrNull { room.getValue(it) >= need && it % 5 != 0 }
            assertEquals(expectedFit, ranking.firstFit(task)?.id) { "Wrong fit at step $step" }
        }
    }

    @Test
    fun testBlockWithoutRoomIsSkippedWithoutTestingItsHosts() {
        val tested = mutableListOf<Int>()
        val recordingFilter = HostFilter { host, _ -> tested.add(host.id) }
        val ranking = HostRanking(listOf(roomFilter, recordingFilter), blockSize = 2, numHosts = 8)

        // The hosts without room (0..3) rank first; the first block is skipped whole and 2 and 3 on their stored room
        for (id in 0 until 6) {
            room[id] = if (id < 4) 0.0 else 10.0
            ranking.put(host(id), (10 - id).toDouble())
        }

        need = 5.0
        assertEquals(4, ranking.firstFit(task)?.id)
        assertEquals(listOf(4), tested)
    }

    @Test
    fun testHostsWithTheSameIdAreRejected() {
        val ranking = HostRanking(listOf(roomFilter), blockSize = 8, numHosts = 4)
        room[1] = 1.0
        ranking.put(host(1), 1.0)

        assertThrows<IllegalStateException> { ranking.put(host(1), 2.0) }
    }
}
