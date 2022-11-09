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

package org.opendc.faas.simulator

import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.faas.service.FaaSService
import org.opendc.faas.service.autoscaler.FunctionTerminationPolicyFixed
import org.opendc.faas.service.router.RandomRoutingPolicy
import org.opendc.faas.simulator.delay.ColdStartModel
import org.opendc.faas.simulator.delay.StochasticDelayInjector
import org.opendc.faas.simulator.workload.SimFaaSWorkload
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.compute.workload.SimWorkloads
import org.opendc.simulator.kotlin.runSimulation
import java.time.Duration
import java.util.Random

/**
 * A test suite for the [FaaSService] implementation under simulated conditions.
 */
internal class SimFaaSServiceTest {

    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = MachineModel(
            /*cpus*/ List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 1000.0) },
            /*memory*/ List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    @Test
    fun testSmoke() = runSimulation {
        val random = Random(0)
        val workload = spyk(object : SimFaaSWorkload, SimWorkload by SimWorkloads.runtime(1000, 1.0) {
            override suspend fun invoke() {
                delay(random.nextInt(1000).toLong())
            }
        })

        val delayInjector = StochasticDelayInjector(ColdStartModel.GOOGLE, random)
        val deployer = SimFunctionDeployer(dispatcher, machineModel, delayInjector) { workload }
        val service = FaaSService(
            dispatcher,
            deployer,
            RandomRoutingPolicy(),
            FunctionTerminationPolicyFixed(dispatcher, timeout = Duration.ofMillis(10000))
        )

        val client = service.newClient()

        val function = client.newFunction("test", 128)
        function.invoke()
        delay(2000)

        service.close()
        deployer.close()

        yield()

        val funcStats = service.getFunctionStats(function)

        assertAll(
            { coVerify { workload.invoke() } },
            { assertEquals(1, funcStats.totalInvocations) },
            { assertEquals(1, funcStats.delayedInvocations) },
            { assertEquals(0, funcStats.failedInvocations) },
            { assertEquals(100.0, funcStats.waitTime.mean) },
            { assertEquals(1285.0, funcStats.activeTime.mean) }
        )
    }
}
