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

package com.atlarge.opendc.compute.virt

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ProcessingNode
import com.atlarge.opendc.compute.core.image.FlopsApplicationImage
import com.atlarge.opendc.compute.metal.driver.SimpleBareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun smoke() {
        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider("test")
        val root = system.newDomain("root")

        root.launch {
            val vmm = HypervisorImage
            val workloadA = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1_000, 1)
            val workloadB = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 2_000, 1)

            val driverDom = root.newDomain("driver")

            val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 1)
            val cpus = List(1) { ProcessingUnit(cpuNode, it, 2000.0) }
            val metalDriver = SimpleBareMetalDriver(driverDom, UUID.randomUUID(), "test", emptyMap(), cpus, emptyList())

            metalDriver.init()
            metalDriver.setImage(vmm)
            val node = metalDriver.start()
            node.server?.events?.onEach { println(it) }?.launchIn(this)

            delay(5)

            val flavor = Flavor(1, 0)
            val vmDriver = metalDriver.refresh().server!!.services[VirtDriver]
            vmDriver.events.onEach { println(it) }.launchIn(this)
            val vmA = vmDriver.spawn("a", workloadA, flavor)
            vmA.events.onEach { println(it) }.launchIn(this)
            val vmB = vmDriver.spawn("b", workloadB, flavor)
            vmB.events.onEach { println(it) }.launchIn(this)
        }

        runBlocking {
            system.run()
            system.terminate()
        }
    }
}
