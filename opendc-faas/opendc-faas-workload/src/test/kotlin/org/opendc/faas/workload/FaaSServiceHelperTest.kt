/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.faas.workload

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.faas.service.autoscaler.FunctionTerminationPolicyFixed
import org.opendc.faas.service.router.RandomRoutingPolicy
import org.opendc.faas.simulator.delay.ColdStartModel
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.core.runBlockingSimulation
import java.io.File
import java.time.Duration

/**
 * Integration test suite for the [FaaSServiceHelper] class.
 */
class FaaSServiceHelperTest {
    /**
     * Smoke test that simulates a small trace.
     */
    @Test
    fun testSmoke() = runBlockingSimulation {
        val trace = ServerlessTraceReader().parse(File("src/test/resources/trace"))
        val runner = FaaSServiceHelper(
            coroutineContext,
            clock,
            createMachineModel(),
            RandomRoutingPolicy(),
            FunctionTerminationPolicyFixed(coroutineContext, clock, timeout = Duration.ofMinutes(10)),
            coldStartModel = ColdStartModel.GOOGLE
        )

        try {
            runner.run(trace)
        } finally {
            runner.close()
        }

        val stats = runner.service.getSchedulerStats()

        assertAll(
            { assertEquals(14, stats.totalInvocations) },
            { assertEquals(2, stats.timelyInvocations) },
            { assertEquals(12, stats.delayedInvocations) },
        )
    }

    /**
     * Construct the machine model to test with.
     */
    private fun createMachineModel(): MachineModel {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        return MachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 1000.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }
}
