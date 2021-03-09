package org.opendc.compute.simulator.power.models

import org.opendc.compute.simulator.power.api.CpuPowerModel

/**
 * A decorator for ignoring the idle power when computing energy consumption of components.
 *
 * @param delegate The [CpuPowerModel] to delegate to.
 */
public class ZeroIdlePowerDecorator(private val delegate: CpuPowerModel) : CpuPowerModel {
    override fun computeCpuPower(cpuUtil: Double): Double {
        return if (cpuUtil == 0.0) 0.0 else delegate.computeCpuPower(cpuUtil)
    }
}
