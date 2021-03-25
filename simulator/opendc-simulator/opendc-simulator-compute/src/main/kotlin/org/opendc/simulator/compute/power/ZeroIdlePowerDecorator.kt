package org.opendc.simulator.compute.power

/**
 * A decorator for ignoring the idle power when computing energy consumption of components.
 *
 * @param delegate The [MachinePowerModel] to delegate to.
 */
public class ZeroIdlePowerDecorator(private val delegate: MachinePowerModel) : MachinePowerModel {
    override fun computeCpuPower(cpuUtil: Double): Double {
        return if (cpuUtil == 0.0) 0.0 else delegate.computeCpuPower(cpuUtil)
    }
}
