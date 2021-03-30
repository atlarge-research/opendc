package org.opendc.simulator.compute.power

import org.opendc.simulator.compute.SimMachine

/**
 * A model for estimating the power usage of a [SimMachine].
 */
public interface PowerModel {
    /**
     * Compute the power consumption of the CPU based on its utilization.
     *
     * @param utilization The CPU utilization percentage in [0, 1].
     * @return The power consumption of the CPU in terms of watts (W).
     */
    public fun computePower(utilization: Double): Double
}
