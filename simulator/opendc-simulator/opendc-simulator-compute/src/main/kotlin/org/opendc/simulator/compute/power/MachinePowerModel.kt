package org.opendc.simulator.compute.power

import org.opendc.simulator.compute.SimMachine

/**
 * A model for estimating the power usage of a [SimMachine].
 */
public interface MachinePowerModel {
    /**
     * Computes CPU power consumption for each host.
     *
     * @param cpuUtil The CPU utilization percentage.
     * @return A [Double] value of CPU power consumption.
     * @throws IllegalArgumentException Will throw an error if [cpuUtil] is out of range.
     */
    @Throws(IllegalArgumentException::class)
    public fun computeCpuPower(cpuUtil: Double): Double
}
