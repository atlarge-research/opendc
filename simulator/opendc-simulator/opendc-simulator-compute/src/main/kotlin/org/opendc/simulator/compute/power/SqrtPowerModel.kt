package org.opendc.simulator.compute.power

import kotlin.math.sqrt

/**
 * The square root power model partially adapted from CloudSim.
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param staticPower The power draw of the server in halting state in W.
 */
public class SqrtPowerModel(private val maxPower: Double, private val staticPower: Double) : PowerModel {
    private val factor: Double = (maxPower - staticPower) / sqrt(100.0)

    override fun computePower(utilization: Double): Double {
        return staticPower + factor * sqrt(utilization * 100)
    }

    override fun toString(): String = "SqrtPowerModel[max=$maxPower,static=$staticPower]"
}
