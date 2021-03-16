package org.opendc.compute.simulator.power.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.opendc.simulator.compute.SimMachine

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
     * @param machine The [SimMachine] that the model is measuring.
     * @return A [Flow] of values representing the server power draw.
     */
    public fun getPowerDraw(machine: SimMachine): Flow<Double> =
        machine.usage.map {
            computeCpuPower(it)
        }
}
