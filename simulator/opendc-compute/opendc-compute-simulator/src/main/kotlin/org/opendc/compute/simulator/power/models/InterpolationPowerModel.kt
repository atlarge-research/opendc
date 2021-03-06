package org.opendc.compute.simulator.power.models

import org.opendc.compute.simulator.power.api.CpuPowerModel
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The linear interpolation power model partially adapted from CloudSim.
 *
 * @param maxPower The maximum power draw in Watts of the server.
 * @param staticPowerPercent The static power percentage.
 * @property staticPower The static power consumption that is not dependent on resource usage.
 *                      It is the amount of energy consumed even when the host is idle.
 * @property constPower The constant power consumption for each fraction of resource used.
 */
public abstract class InterpolationPowerModel : CpuPowerModel {

    public override fun computeCpuPower(cpuUtil: Double): Double {
        require(cpuUtil in 0.0..1.0) { "CPU utilization must be in [0, 1]" }

        val cpuUtilFlr = floor(cpuUtil * 10).toInt()
        val cpuUtilCil = ceil(cpuUtil * 10).toInt()
        val power1: Double = getPowerData(cpuUtilFlr)
        val power2: Double = getPowerData(cpuUtilCil)
        val delta = (power2 - power1) / 10

        return if (cpuUtil % 0.1 == 0.0)
            getPowerData((cpuUtil * 10).toInt())
        else
            power1 + delta * (cpuUtil - cpuUtilFlr.toDouble() / 10) * 100
    }

    public abstract fun getPowerData(index: Int): Double
}
