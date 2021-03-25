package org.opendc.simulator.compute.power

import kotlin.math.pow

/**
 * The cubic power model partially adapted from CloudSim.
 *
 * @param maxPower The maximum power draw in Watts of the server.
 * @param staticPowerPercent The static power percentage.
 * @property staticPower The static power consumption that is not dependent on resource usage.
 *                      It is the amount of energy consumed even when the host is idle.
 * @property constPower The constant power consumption for each fraction of resource used.
 */
public class CubicPowerModel(
    private var maxPower: Double,
    staticPowerPercent: Double
) : MachinePowerModel {
    private var staticPower: Double = staticPowerPercent * maxPower
    private var constPower: Double = (maxPower - staticPower) / 100.0.pow(3)

    public override fun computeCpuPower(cpuUtil: Double): Double {
        require(cpuUtil in 0.0..1.0) { "CPU utilization must be in [0, 1]" }
        return staticPower + constPower * (cpuUtil * 100).pow(3)
    }
}
