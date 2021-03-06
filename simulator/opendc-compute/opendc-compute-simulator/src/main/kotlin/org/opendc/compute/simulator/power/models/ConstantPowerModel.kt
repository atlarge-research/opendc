package org.opendc.compute.simulator.power.models

import org.opendc.compute.simulator.power.api.CpuPowerModel

/**
 * A power model which produces a constant value [constant].
 */
public class ConstantPowerModel(private val constant: Double) : CpuPowerModel {
    public override fun computeCpuPower(cpuUtil: Double): Double = constant
}
