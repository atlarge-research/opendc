package org.opendc.simulator.compute.power

/**
 * A power model which produces a constant value [constant].
 */
public class ConstantPowerModel(private val constant: Double) : MachinePowerModel {
    public override fun computeCpuPower(cpuUtil: Double): Double = constant
}
