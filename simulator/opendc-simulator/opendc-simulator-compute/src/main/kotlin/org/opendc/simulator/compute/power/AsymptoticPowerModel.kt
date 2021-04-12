package org.opendc.simulator.compute.power

import kotlin.math.E
import kotlin.math.pow

/**
 * The asymptotic power model partially adapted from GreenCloud.
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param staticPower The power draw of the server in halting state in W.
 * @param asymUtil A utilization level at which the server attains asymptotic,
 *              i.e., close to linear power consumption versus the offered load.
 *              For most of the CPUs,a is in [0.2, 0.5].
 * @param isDvfsEnabled A flag indicates whether DVFS is enabled.
 */
public class AsymptoticPowerModel(
    private val maxPower: Double,
    private val staticPower: Double,
    private val asymUtil: Double,
    private val isDvfsEnabled: Boolean,
) : PowerModel {
    private val factor: Double = (maxPower - staticPower) / 100

    public override fun computePower(utilization: Double): Double =
        if (isDvfsEnabled)
            staticPower + (factor * 100) / 2 * (1 + utilization.pow(3) - E.pow(-utilization.pow(3) / asymUtil))
        else
            staticPower + (factor * 100) / 2 * (1 + utilization - E.pow(-utilization / asymUtil))

    override fun toString(): String = "AsymptoticPowerModel[max=$maxPower,static=$staticPower,asymptotic=$asymUtil]"
}
