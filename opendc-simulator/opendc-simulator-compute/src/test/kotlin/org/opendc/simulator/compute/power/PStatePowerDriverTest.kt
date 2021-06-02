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

package org.opendc.simulator.compute.power

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimProcessingUnit

/**
 * Test suite for [PStatePowerDriver].
 */
internal class PStatePowerDriverTest {
    @Test
    fun testPowerBaseline() {
        val machine = mockk<SimBareMetalMachine>()

        val driver = PStatePowerDriver(
            sortedMapOf(
                2800.0 to ConstantPowerModel(200.0),
                3300.0 to ConstantPowerModel(300.0),
                3600.0 to ConstantPowerModel(350.0),
            )
        )

        val logic = driver.createLogic(machine, emptyList())
        assertEquals(200.0, logic.computePower())
    }

    @Test
    fun testPowerWithSingleCpu() {
        val machine = mockk<SimBareMetalMachine>()
        val cpu = mockk<SimProcessingUnit>()

        every { cpu.capacity } returns 3200.0
        every { cpu.speed } returns 1200.0

        val driver = PStatePowerDriver(
            sortedMapOf(
                2800.0 to ConstantPowerModel(200.0),
                3300.0 to ConstantPowerModel(300.0),
                3600.0 to ConstantPowerModel(350.0),
            )
        )

        val logic = driver.createLogic(machine, listOf(cpu))

        assertEquals(300.0, logic.computePower())
    }

    @Test
    fun testPowerWithMultipleCpus() {
        val machine = mockk<SimBareMetalMachine>()
        val cpus = listOf(
            mockk<SimProcessingUnit>(),
            mockk()
        )

        every { cpus[0].capacity } returns 1000.0
        every { cpus[0].speed } returns 1200.0

        every { cpus[1].capacity } returns 3500.0
        every { cpus[1].speed } returns 1200.0

        val driver = PStatePowerDriver(
            sortedMapOf(
                2800.0 to ConstantPowerModel(200.0),
                3300.0 to ConstantPowerModel(300.0),
                3600.0 to ConstantPowerModel(350.0),
            )
        )

        val logic = driver.createLogic(machine, cpus)

        assertEquals(350.0, logic.computePower())
    }

    @Test
    fun testPowerBasedOnUtilization() {
        val machine = mockk<SimBareMetalMachine>()
        val cpu = mockk<SimProcessingUnit>()

        every { cpu.model.frequency } returns 4200.0

        val driver = PStatePowerDriver(
            sortedMapOf(
                2800.0 to LinearPowerModel(200.0, 100.0),
                3300.0 to LinearPowerModel(250.0, 150.0),
                4000.0 to LinearPowerModel(300.0, 200.0),
            )
        )

        val logic = driver.createLogic(machine, listOf(cpu))

        every { cpu.speed } returns 1400.0
        every { cpu.capacity } returns 1400.0
        assertEquals(150.0, logic.computePower())

        every { cpu.speed } returns 1400.0
        every { cpu.capacity } returns 4000.0
        assertEquals(235.0, logic.computePower())
    }
}
