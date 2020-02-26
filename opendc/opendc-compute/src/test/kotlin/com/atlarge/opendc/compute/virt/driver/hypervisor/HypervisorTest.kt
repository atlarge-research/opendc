/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.virt.driver.hypervisor

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.FlopsApplicationImage
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.PowerState
import com.atlarge.opendc.compute.metal.driver.SimpleBareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.ServiceLoader
import java.util.UUID

/**
 * Basic test-suite for the hypervisor.
 */
internal class HypervisorTest {
    /**
     * A smoke test for the bare-metal driver.
     */
    @Test
    fun smoke() {
        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider({ _ ->
            val vmm = HypervisorImage(object : HypervisorMonitor {
                override fun onSliceFinish(
                    time: Long,
                    requestedBurst: Long,
                    grantedBurst: Long,
                    numberOfDeployedImages: Int,
                    hostServer: Server
                ) {
                    println("Hello World!")
                }
            })
            val workloadA = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1_000_000, 1)
            val workloadB = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 2_000_000, 1)
            val monitor = object : ServerMonitor {
                override suspend fun onUpdate(server: Server, previousState: ServerState) {
                    println("[${processContext.clock.millis()}]: $server")
                }
            }
            val metalDriver = SimpleBareMetalDriver(UUID.randomUUID(), "test", listOf(ProcessingUnit("Intel", "Xeon", "amd64", 2000.0, 1)), emptyList())

            metalDriver.init(monitor)
            metalDriver.setImage(vmm)
            metalDriver.setPower(PowerState.POWER_ON)
            delay(5)

            val flavor = Flavor(1, 0)
            val vmDriver = metalDriver.refresh().server!!.serviceRegistry[VirtDriver]
            vmDriver.spawn(workloadA, monitor, flavor)
            vmDriver.spawn(workloadB, monitor, flavor)
        }, name = "sim")

        runBlocking {
            system.run()
            system.terminate()
        }
    }
}
