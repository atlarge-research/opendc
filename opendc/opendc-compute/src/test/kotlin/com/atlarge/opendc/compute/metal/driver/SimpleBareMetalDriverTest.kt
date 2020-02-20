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

package com.atlarge.opendc.compute.metal.driver

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerFlavor
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.FlopsApplicationImage
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.PowerState
import com.atlarge.opendc.core.resource.TagContainerImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.ServiceLoader
import java.util.UUID

internal class SimpleBareMetalDriverTest {
    /**
     * A smoke test for the bare-metal driver.
     */
    @Test
    fun smoke() {
        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider({ _ ->
            val flavor = ServerFlavor(listOf(ProcessingUnit("Intel", "Xeon", "amd64", 2300.0, 4)))
            val image = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", TagContainerImpl(), 1000, 2)
            val monitor = object : ServerMonitor {
                override suspend fun onUpdate(server: Server, previousState: ServerState) {
                    println(server)
                }
            }
            val driver = SimpleBareMetalDriver(UUID.randomUUID(), "test", flavor)

            driver.init(monitor)
            driver.setImage(image)
            driver.setPower(PowerState.POWER_ON)
            delay(5)
            println(driver.refresh())
        }, name = "sim")

        runBlocking {
            system.run()
            system.terminate()
        }
    }
}
