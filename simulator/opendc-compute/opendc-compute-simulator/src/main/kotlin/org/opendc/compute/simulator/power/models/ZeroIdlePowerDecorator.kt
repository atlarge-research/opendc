package org.opendc.compute.simulator.power.models

import org.opendc.compute.simulator.power.api.CpuPowerModel

/**
 * A decorator for ignoring the idle power when computing energy consumption of components.
 *
 * @param cpuModelWrappee The wrappe of a [CpuPowerModel].
 */
public class ZeroIdlePowerDecorator(private val cpuModelWrappee: CpuPowerModel) : CpuPowerModel {
    override fun computeCpuPower(cpuUtil: Double): Double {
        return if (cpuUtil == 0.0) 0.0 else cpuModelWrappee.computeCpuPower(cpuUtil)
    }
}
