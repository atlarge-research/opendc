/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.compute.metal.driver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.compute.core.ProcessingNode
import org.opendc.compute.core.ProcessingUnit
import org.opendc.compute.core.ServerEvent
import org.opendc.compute.core.ServerState
import org.opendc.compute.core.image.FlopsApplicationImage
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
internal class SimpleBareMetalDriverTest {
    /**
     * A smoke test for the bare-metal driver.
     */
    @Test
    fun smoke() {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)

        var finalState: ServerState = ServerState.BUILD
        testScope.launch {
            val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 4)
            val cpus = List(4) { ProcessingUnit(cpuNode, it, 2400.0) }
            val driver = SimpleBareMetalDriver(this, clock, UUID.randomUUID(), "test", emptyMap(), cpus, emptyList())
            val image = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1_000, 2)

            // Batch driver commands
            withContext(coroutineContext) {
                driver.init()
                driver.setImage(image)
                val server = driver.start().server!!
                driver.usage
                    .onEach { println("${clock.millis()} $it") }
                    .launchIn(this)
                server.events.collect { event ->
                    when (event) {
                        is ServerEvent.StateChanged -> {
                            println("${clock.millis()} $event")
                            finalState = event.server.state
                        }
                    }
                }
            }
        }

        testScope.advanceUntilIdle()
        assertEquals(ServerState.SHUTOFF, finalState)
    }
}
