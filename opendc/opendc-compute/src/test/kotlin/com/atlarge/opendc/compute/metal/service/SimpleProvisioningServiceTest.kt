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

package com.atlarge.opendc.compute.metal.service

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.core.ProcessingNode
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.FlopsApplicationImage
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.driver.SimpleBareMetalDriver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.ServiceLoader
import java.util.UUID

/**
 * Test suite for the [SimpleProvisioningService].
 */
internal class SimpleProvisioningServiceTest {
    /**
     * A basic smoke test.
     */
    @Test
    fun smoke() {
        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider("sim")
        val root = system.newDomain(name = "root")
        root.launch {
            val image = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1000, 2)
            val monitor = object : ServerMonitor {
                override suspend fun onUpdate(server: Server, previousState: ServerState) {
                    println(server)
                }
            }

            val dom = root.newDomain("provisioner")

            val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 4)
            val cpus = List(5) { ProcessingUnit(cpuNode, it, 2400.0) }
            val driver = SimpleBareMetalDriver(UUID.randomUUID(), "test", cpus, emptyList(), dom)

            val provisioner = SimpleProvisioningService(dom)
            provisioner.create(driver)
            delay(5)
            val nodes = provisioner.nodes()
            provisioner.deploy(nodes.first(), image, monitor)
        }

        runBlocking {
            system.run()
            system.terminate()
        }
    }
}
