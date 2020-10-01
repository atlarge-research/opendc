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

package org.opendc.compute.metal.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.jupiter.api.Test
import org.opendc.compute.core.ProcessingNode
import org.opendc.compute.core.ProcessingUnit
import org.opendc.compute.core.image.FlopsApplicationImage
import org.opendc.compute.metal.driver.SimpleBareMetalDriver
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.util.UUID

/**
 * Test suite for the [SimpleProvisioningService].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimpleProvisioningServiceTest {
    /**
     * A basic smoke test.
     */
    @Test
    fun smoke() {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)

        testScope.launch {
            val image = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1000, 2)

            val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 4)
            val cpus = List(4) { ProcessingUnit(cpuNode, it, 2400.0) }
            val driver = SimpleBareMetalDriver(this, clock, UUID.randomUUID(), "test", emptyMap(), cpus, emptyList())

            val provisioner = SimpleProvisioningService()
            provisioner.create(driver)
            delay(5)
            val nodes = provisioner.nodes()
            val node = provisioner.deploy(nodes.first(), image)
            node.server!!.events.collect { println(it) }
        }

        testScope.advanceUntilIdle()
    }
}
