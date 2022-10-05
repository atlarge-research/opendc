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

package org.opendc.simulator.compute.device

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.power.PowerDriver
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.kotlin.runSimulation
import org.opendc.simulator.power.SimPowerSource

/**
 * Test suite for [SimPsu]
 */
internal class SimPsuTest {

    @Test
    fun testInvalidInput() {
        assertThrows<IllegalArgumentException> { SimPsu(1.0, emptyMap()) }
    }

    @Test
    fun testDoubleConnect() {
        val psu = SimPsu(1.0, mapOf(0.0 to 1.0))
        val cpuLogic = mockk<PowerDriver.Logic>()
        psu.connect(cpuLogic)
        assertThrows<IllegalStateException> { psu.connect(mockk()) }
    }

    @Test
    fun testPsuIdle() = runSimulation {
        val ratedOutputPower = 240.0
        val energyEfficiency = mapOf(0.0 to 1.0)

        val engine = FlowEngine(coroutineContext, clock)
        val source = SimPowerSource(engine, capacity = ratedOutputPower)

        val cpuLogic = mockk<PowerDriver.Logic>()
        every { cpuLogic.computePower() } returns 0.0

        val psu = SimPsu(ratedOutputPower, energyEfficiency)
        psu.connect(cpuLogic)
        source.connect(psu)

        assertEquals(0.0, source.powerDraw, 0.01)
    }

    @Test
    fun testPsuPowerLoss() = runSimulation {
        val ratedOutputPower = 240.0
        // Efficiency of 80 Plus Titanium PSU
        val energyEfficiency = sortedMapOf(
            0.3 to 0.9,
            0.7 to 0.92,
            1.0 to 0.94,
        )

        val engine = FlowEngine(coroutineContext, clock)
        val source = SimPowerSource(engine, capacity = ratedOutputPower)

        val cpuLogic = mockk<PowerDriver.Logic>()
        every { cpuLogic.computePower() } returnsMany listOf(50.0, 100.0, 150.0, 200.0)

        val psu = SimPsu(ratedOutputPower, energyEfficiency)
        psu.connect(cpuLogic)
        source.connect(psu)

        assertEquals(55.55, source.powerDraw, 0.01)

        psu.update()
        assertEquals(108.695, source.powerDraw, 0.01)

        psu.update()
        assertEquals(163.043, source.powerDraw, 0.01)

        psu.update()
        assertEquals(212.765, source.powerDraw, 0.01)
    }
}
