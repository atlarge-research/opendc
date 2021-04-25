package org.opendc.simulator.compute.power

import kotlin.math.sqrt

/**
 * The square root power model partially adapted from CloudSim.
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param idlePower The power draw of the server at its lowest utilization level in W.
 */
public class SqrtPowerModel(private val maxPower: Double, private val idlePower: Double) : PowerModel {
    private val factor: Double = (maxPower - idlePower) / sqrt(100.0)

    override fun computePower(utilization: Double): Double {
        return idlePower + factor * sqrt(utilization * 100)
    }

    override fun toString(): String = "SqrtPowerModel[max=$maxPower,idle=$idlePower]"
}
