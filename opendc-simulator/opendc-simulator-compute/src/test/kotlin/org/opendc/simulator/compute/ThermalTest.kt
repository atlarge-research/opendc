package org.opendc.simulator.compute

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.compute.power.CpuPowerModels

class ThermalTest {
    private val maxPower = 130.0
    private val idlePower = 12.0
    private val staticPower = 0.001 * 1.2

    private val powerModel = CpuPowerModels.linear(maxPower, idlePower)

    @Test
    fun testThermalPower() {
        val utilizationVals = listOf(0.25, 0.50, 0.75, 1.0) // Define the range of utilization values
        val expectedThermalPower = listOf(53.5012, 83.0012, 112.5012, 142.0012) // Expected thermal power dissipation for each utilization

        utilizationVals.forEachIndexed { i, u ->
            assertEquals(expectedThermalPower[i], (idlePower + staticPower + powerModel.computePower(u)))
        }

    }
}
