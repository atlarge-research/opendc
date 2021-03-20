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

package org.opendc.serverless.simulator

import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.serverless.service.ServerlessService
import org.opendc.serverless.service.router.RandomRoutingPolicy
import org.opendc.serverless.simulator.workload.SimServerlessWorkload
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.SimMemoryUnit
import org.opendc.simulator.compute.model.SimProcessingNode
import org.opendc.simulator.compute.model.SimProcessingUnit
import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter

/**
 * A test suite for the [ServerlessService] implementation under simulated conditions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimServerlessServiceTest {

    private lateinit var machineModel: SimMachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = SimProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = SimMachineModel(
            cpus = List(cpuNode.coreCount) { SimProcessingUnit(cpuNode, it, 1000.0) },
            memory = List(4) { SimMemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    @Test
    fun testSmoke() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val workload = spyk(object : SimServerlessWorkload {
            override fun onInvoke(): SimWorkload = SimFlopsWorkload(1000)
        })
        val deployer = SimFunctionDeployer(clock, this, machineModel) { workload }
        val service = ServerlessService(coroutineContext, clock, deployer, RandomRoutingPolicy())

        val client = service.newClient()

        val function = client.newFunction("test")
        function.invoke()
        delay(2000)

        service.close()

        yield()

        assertAll(
            { verify { workload.onStart() } },
            { verify { workload.onInvoke() } },
            { verify { workload.onStop() } }
        )
    }
}
