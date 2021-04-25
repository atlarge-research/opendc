package org.opendc.simulator.compute.power

/**
 * A decorator for ignoring the idle power when computing energy consumption of components.
 *
 * @param delegate The [PowerModel] to delegate to.
 */
public class ZeroIdlePowerDecorator(private val delegate: PowerModel) : PowerModel {
    override fun computePower(utilization: Double): Double {
        return if (utilization == 0.0)
            0.0
        else
            delegate.computePower(utilization)
    }

    override fun toString(): String = "ZeroIdlePowerDecorator[delegate=$delegate]"
}
