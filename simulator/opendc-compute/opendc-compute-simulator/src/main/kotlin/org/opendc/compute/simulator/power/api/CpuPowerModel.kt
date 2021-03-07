package org.opendc.compute.simulator.power.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.opendc.metal.driver.BareMetalDriver

public interface CpuPowerModel {
    /**
     * Computes CPU power consumption for each host.
     *
     * @param cpuUtil The CPU utilization percentage.
     * @return A [Double] value of CPU power consumption.
     * @throws IllegalArgumentException Will throw an error if [cpuUtil] is out of range.
     */
    @Throws(IllegalArgumentException::class)
    public fun computeCpuPower(cpuUtil: Double): Double

    /**
     * Emits the values of power consumption for servers.
     *
     * @param driver A [BareMetalDriver] that offers host CPU utilization.
     * @param withoutIdle A [Boolean] flag indicates whether (false) add a constant
     *                  power consumption value when the server is idle or (true) not
     *                  with a default value being false.
     * @return A [Flow] of values representing the server power draw.
     */
    public fun getPowerDraw(driver: BareMetalDriver, withoutIdle: Boolean = false): Flow<Double> =
        driver.usage.map {
            computeCpuPower(it)
        }
}
