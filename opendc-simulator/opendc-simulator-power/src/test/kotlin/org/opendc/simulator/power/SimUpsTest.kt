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
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [SimUps] class.
 */
internal class SimUpsTest {
    @Test
    fun testSingleInlet() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val source = SimPowerSource(graph, /*capacity*/ 200.0f)
        val ups = SimUps(graph)
        source.connect(ups.newInlet())
        ups.connect(TestInlet(graph))

        yield()

        assertEquals(100.0f, source.powerDraw)
    }

    @Test
    fun testDoubleInlet() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val source1 = SimPowerSource(graph, /*capacity*/ 200.0f)
        val source2 = SimPowerSource(graph, /*capacity*/ 200.0f)
        val ups = SimUps(graph)
        source1.connect(ups.newInlet())
        source2.connect(ups.newInlet())

        ups.connect(TestInlet(graph))

        yield()

        assertAll(
            { assertEquals(50.0f, source1.powerDraw) },
            { assertEquals(50.0f, source2.powerDraw) }
        )
    }

    @Test
    fun testLoss() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val source = SimPowerSource(graph, /*capacity*/ 500.0f)
        // https://download.schneider-electric.com/files?p_Doc_Ref=SPD_NRAN-66CK3D_EN
        val ups = SimUps(graph, /*idlePower*/ 4.0f, /*lossCoefficient*/ 0.05f)
        source.connect(ups.newInlet())
        ups.connect(TestInlet(graph))

        yield()

        assertEquals(109.0f, source.powerDraw, 0.01f)
    }

    @Test
    fun testDisconnect() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val source1 = SimPowerSource(graph, /*capacity*/ 200.0f)
        val source2 = SimPowerSource(graph, /*capacity*/ 200.0f)
        val ups = SimUps(graph)
        source1.connect(ups.newInlet())
        source2.connect(ups.newInlet())

        val inlet = TestInlet(graph)

        ups.connect(inlet)
        ups.disconnect()

        yield()

        assertEquals(0.0f, inlet.flowOutlet.capacity)
    }
}
