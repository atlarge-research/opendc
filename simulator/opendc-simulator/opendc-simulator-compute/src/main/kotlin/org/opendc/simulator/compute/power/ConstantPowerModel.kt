package org.opendc.simulator.compute.power

/**
 * A power model which produces a constant value [constant].
 */
public class ConstantPowerModel(private val constant: Double) : PowerModel {
    public override fun computePower(utilization: Double): Double = constant
}
