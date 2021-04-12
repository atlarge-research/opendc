package org.opendc.simulator.compute.power

import kotlin.math.pow

/**
 * The cubic power model partially adapted from CloudSim.
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param staticPower The power draw of the server in halting state in W.
 */
public class CubicPowerModel(private val maxPower: Double, private val staticPower: Double) : PowerModel {
    private val factor: Double = (maxPower - staticPower) / 100.0.pow(3)

    public override fun computePower(utilization: Double): Double {
        return staticPower + factor * (utilization * 100).pow(3)
    }

    override fun toString(): String = "CubicPowerModel[max=$maxPower,static=$staticPower]"
}
