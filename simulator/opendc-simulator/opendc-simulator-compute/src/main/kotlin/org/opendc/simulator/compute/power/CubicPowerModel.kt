package org.opendc.simulator.compute.power

import kotlin.math.pow

/**
 * The cubic power model partially adapted from CloudSim.
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param idlePower The power draw of the server in idle state in W.
 */
public class CubicPowerModel(private val maxPower: Double, private val idlePower: Double) : PowerModel {
    private val factor: Double = (maxPower - idlePower) / 100.0.pow(3)

    public override fun computePower(utilization: Double): Double {
        return idlePower + factor * (utilization * 100).pow(3)
    }

    override fun toString(): String = "CubicPowerModel[max=$maxPower,idle=$idlePower]"
}
