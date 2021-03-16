package org.opendc.compute.simulator.power

import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.compute.simulator.power.models.PStatePowerModel
import org.opendc.simulator.compute.SimBareMetalMachine
import java.time.Clock

internal class PStatePowerModelTest {
    @Test
    fun `update CPU power meter with P-states`() {
        val p0Power = 8.0
        val p3Power = 94.0
        val p4Power = 103.0
        val expectedP0Power = 8.0 * 10
        val expectedP0P4Power = expectedP0Power + 103.0 * 10

        val clock = mockkClass(Clock::class)
        val machine = mockkClass(SimBareMetalMachine::class)
        every { clock.millis() } returnsMany listOf(0L, 0L, 10_000L, 20_000L)
        every { machine.speed } returns
            listOf(2.8, 2.8, 2.8, 2.8).map { it * 1000 } andThen // Max. 2.8MHz covered by P0
            listOf(1.5, 3.1, 3.3, 3.6).map { it * 1000 } andThen // Max. 3.6MHz covered by P4
            listOf(1.5, 3.1, 3.1, 3.3).map { it * 1000 } // Max. 3.3MHz covered by P3

        // Power meter initialization.
        val pStatePowerModel = PStatePowerModel(machine, clock)
        verify(exactly = 2) { clock.millis() }
        verify(exactly = 1) { machine.speed }
        assertEquals(p0Power, pStatePowerModel.getInstantCpuPower())

        // The first measure.
        pStatePowerModel.updateCpuPowerMeter()
        assertEquals(p4Power, pStatePowerModel.getInstantCpuPower())
        assertEquals(expectedP0Power, pStatePowerModel.getAccumulatedCpuPower())

        // The second measure.
        pStatePowerModel.updateCpuPowerMeter()
        assertEquals(p3Power, pStatePowerModel.getInstantCpuPower())
        assertEquals(expectedP0P4Power, pStatePowerModel.getAccumulatedCpuPower())

        verify(exactly = 4) { clock.millis() }
    }
}
