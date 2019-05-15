/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.model.workload.application

import com.atlarge.odcsim.testkit.BehaviorTestKit
import com.atlarge.opendc.model.User
import com.atlarge.opendc.model.resources.compute.Machine
import com.atlarge.opendc.model.resources.compute.ProcessingElement
import com.atlarge.opendc.model.resources.compute.ProcessingUnit
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * A test suite for the [FlopsApplication].
 */
@DisplayName("FlopsApplication")
internal class FlopsApplicationTest {
    private val flops = 10000L
    private val cores = 2
    private val machine: Machine = mock()
    private val user: User = mock()
    private val cpu: ProcessingUnit = ProcessingUnit("Intel", 6, 8600, "Intel(R) Core(TM) i7-6920HQ CPU @ 2.90GHz", 2900.0, 1)

    private lateinit var application: FlopsApplication

    @BeforeEach
    fun setUp() {
        application = FlopsApplication(UUID.randomUUID(), "java", user, cores, flops)
    }

    @Test
    fun `should become ready after triggering installation`() {
        val test = BehaviorTestKit(application())
        val inbox = test.createInbox<ProcessEvent>()
        test.run(ProcessMessage.Setup(machine, inbox.ref))
        inbox.expectMessage(ProcessEvent.Ready(test.ref))
    }

    @Test
    fun `should not respond to setup request after being created`() {
        val test = BehaviorTestKit(application())
        val inbox = test.createInbox<ProcessEvent>()

        // Setup Machine
        test.run(ProcessMessage.Setup(machine, inbox.ref))
        inbox.clear()

        // Try again
        assertFalse(test.run(ProcessMessage.Setup(machine, inbox.ref)))
    }

    @Test
    fun `should respond to allocation with consumption`() {
        val test = BehaviorTestKit(application())
        val inbox = test.createInbox<ProcessEvent>()

        val allocation = ProcessMessage.Allocation(mapOf(ProcessingElement(0, cpu) to 0.5), until = 10.0)

        // Setup Machine
        test.run(ProcessMessage.Setup(machine, inbox.ref))
        inbox.clear()

        // Test allocation
        test.run(allocation)
        val msg = inbox.receiveMessage()
        assertTrue(msg is ProcessEvent.Consume)
    }

    @Test
    fun `should inform the machine that it finished processing`() {
        val test = BehaviorTestKit(application())
        val inbox = test.createInbox<ProcessEvent>()
        val allocation = ProcessMessage.Allocation(mapOf(ProcessingElement(0, cpu) to 0.5), until = 10.0)

        // Setup
        test.run(ProcessMessage.Setup(machine, inbox.ref))
        test.run(allocation)
        test.runTo(10.0)
        inbox.clear()

        test.run(allocation)
        inbox.expectMessage(ProcessEvent.Exit(test.ref, 0))
    }

    @Test
    fun `should be able to update its utilization`() {
        val test = BehaviorTestKit(application())
        val inbox = test.createInbox<ProcessEvent>()
        val allocation1 = ProcessMessage.Allocation(mapOf(ProcessingElement(0, cpu) to 0.5), until = 10.0)
        val allocation2 = ProcessMessage.Allocation(mapOf(ProcessingElement(0, cpu) to 0.25), until = 10.0)

        // Setup
        test.run(ProcessMessage.Setup(machine, inbox.ref))
        test.run(allocation1)
        test.runTo(5.0)
        inbox.clear()

        test.run(allocation2)
        assertTrue(inbox.receiveMessage() is ProcessEvent.Consume)
    }
}
