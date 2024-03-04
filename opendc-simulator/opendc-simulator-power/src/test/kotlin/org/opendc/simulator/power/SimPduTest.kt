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

import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [SimPdu] class.
 */
internal class SimPduTest {
    @Test
    fun testZeroOutlets() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val source = SimPowerSource(graph, 100.0f)
            val pdu = SimPdu(graph)
            source.connect(pdu)

            yield()

            assertEquals(0.0f, source.powerDraw)
        }

    @Test
    fun testSingleOutlet() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val source = SimPowerSource(graph, 100.0f)
            val pdu = SimPdu(graph)
            source.connect(pdu)
            pdu.newOutlet().connect(TestInlet(graph))

            yield()

            assertEquals(100.0f, source.powerDraw)
        }

    @Test
    fun testDoubleOutlet() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val source = SimPowerSource(graph, 200.0f)
            val pdu = SimPdu(graph)
            source.connect(pdu)

            pdu.newOutlet().connect(TestInlet(graph))
            pdu.newOutlet().connect(TestInlet(graph))

            yield()

            assertEquals(200.0f, source.powerDraw)
        }

    @Test
    fun testDisconnect() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val source = SimPowerSource(graph, 300.0f)
            val pdu = SimPdu(graph)
            source.connect(pdu)

            val outlet = pdu.newOutlet()
            outlet.connect(TestInlet(graph))
            outlet.disconnect()

            yield()

            assertEquals(0.0f, source.powerDraw)
        }

    @Test
    fun testLoss() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val source = SimPowerSource(graph, 500.0f)
            // https://download.schneider-electric.com/files?p_Doc_Ref=SPD_NRAN-66CK3D_EN
            val pdu = SimPdu(graph, 1.5f, 0.015f)
            source.connect(pdu)
            pdu.newOutlet().connect(TestInlet(graph))

            yield()

            assertEquals(251.5f, source.powerDraw, 0.01f)
        }

    @Test
    fun testOutletClose() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val source = SimPowerSource(graph, 100.0f)
            val pdu = SimPdu(graph)
            source.connect(pdu)
            val outlet = pdu.newOutlet()
            outlet.close()

            yield()

            assertThrows<IllegalStateException> {
                outlet.connect(TestInlet(graph))
            }
        }
}
