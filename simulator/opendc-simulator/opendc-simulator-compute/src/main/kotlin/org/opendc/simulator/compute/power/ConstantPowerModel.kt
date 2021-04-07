package org.opendc.simulator.compute.power

/**
 * A power model which produces a constant value [power].
 */
public class ConstantPowerModel(private val power: Double) : PowerModel {
    public override fun computePower(utilization: Double): Double = power

    override fun toString(): String = "ConstantPowerModel[power=$power]"
}
