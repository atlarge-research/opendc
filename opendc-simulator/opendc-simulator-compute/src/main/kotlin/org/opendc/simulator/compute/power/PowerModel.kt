package org.opendc.simulator.compute.power

import org.opendc.simulator.compute.SimMachine

/**
 * A model for estimating the power usage of a [SimMachine].
 */
public interface PowerModel {
    /**
     * Computes CPU power consumption for each host.
     *
     * @param utilization The CPU utilization percentage.
     * @return A [Double] value of CPU power consumption.
     */
    public fun computePower(utilization: Double): Double
}
