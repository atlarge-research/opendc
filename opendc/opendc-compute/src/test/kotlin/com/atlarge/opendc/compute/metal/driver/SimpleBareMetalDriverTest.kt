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
import com.atlarge.opendc.compute.core.ProcessingNode
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.FlopsApplicationImage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.ServiceLoader
import java.util.UUID

internal class SimpleBareMetalDriverTest {
    /**
     * A smoke test for the bare-metal driver.
     */
    @Test
    fun smoke() {
        var finalState: ServerState = ServerState.BUILD
        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider("sim")
        val root = system.newDomain(name = "root")
        root.launch {
            val dom = root.newDomain(name = "driver")
            val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 4)
            val cpus = List(4) { ProcessingUnit(cpuNode, it, 2400.0) }
            val driver = SimpleBareMetalDriver(dom, UUID.randomUUID(), "test", emptyMap(), cpus, emptyList())
            val image = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1_000, 2)

            // Batch driver commands
            withContext(dom.coroutineContext) {
                driver.init()
                driver.setImage(image)
                val server = driver.start().server!!
                server.events.collect { event ->
                    when (event) {
                        is ServerEvent.StateChanged -> { println(event); finalState = event.server.state }
                    }
                }
            }
        }

        runBlocking {
            system.run()
            system.terminate()
        }

        assertEquals(ServerState.SHUTOFF, finalState)
    }
}
