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

package org.opendc.simulator.power

import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceEvent
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.simulator.resources.consumer.SimWorkConsumer

/**
 * Test suite for the [SimPdu] class.
 */
internal class SimPduTest {
    @Test
    fun testZeroOutlets() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val source = SimPowerSource(interpreter, capacity = 100.0)
        val pdu = SimPdu(interpreter)
        source.connect(pdu)

        assertEquals(0.0, source.powerDraw)
    }

    @Test
    fun testSingleOutlet() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val source = SimPowerSource(interpreter, capacity = 100.0)
        val pdu = SimPdu(interpreter)
        source.connect(pdu)
        pdu.newOutlet().connect(SimpleInlet())

        assertEquals(50.0, source.powerDraw)
    }

    @Test
    fun testDoubleOutlet() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val source = SimPowerSource(interpreter, capacity = 100.0)
        val pdu = SimPdu(interpreter)
        source.connect(pdu)

        pdu.newOutlet().connect(SimpleInlet())
        pdu.newOutlet().connect(SimpleInlet())

        assertEquals(100.0, source.powerDraw)
    }

    @Test
    fun testDisconnect() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val source = SimPowerSource(interpreter, capacity = 100.0)
        val pdu = SimPdu(interpreter)
        source.connect(pdu)
        val consumer = spyk(SimWorkConsumer(100.0, utilization = 1.0))
        val inlet = object : SimPowerInlet() {
            override fun createConsumer(): SimResourceConsumer = consumer
        }

        val outlet = pdu.newOutlet()
        outlet.connect(inlet)
        outlet.disconnect()

        verify { consumer.onEvent(any(), SimResourceEvent.Exit) }
    }

    @Test
    fun testLoss() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val source = SimPowerSource(interpreter, capacity = 100.0)
        // https://download.schneider-electric.com/files?p_Doc_Ref=SPD_NRAN-66CK3D_EN
        val pdu = SimPdu(interpreter, idlePower = 1.5, lossCoefficient = 0.015)
        source.connect(pdu)
        pdu.newOutlet().connect(SimpleInlet())
        assertEquals(89.0, source.powerDraw, 0.01)
    }

    @Test
    fun testOutletClose() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val source = SimPowerSource(interpreter, capacity = 100.0)
        val pdu = SimPdu(interpreter)
        source.connect(pdu)
        val outlet = pdu.newOutlet()
        outlet.close()

        assertThrows<IllegalStateException> {
            outlet.connect(SimpleInlet())
        }
    }

    class SimpleInlet : SimPowerInlet() {
        override fun createConsumer(): SimResourceConsumer = SimWorkConsumer(100.0, utilization = 0.5)
    }
}
