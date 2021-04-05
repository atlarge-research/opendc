package org.opendc.simulator.compute.power

import org.yaml.snakeyaml.Yaml
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The linear interpolation power model partially adapted from CloudSim.
 * This model is developed to adopt the <a href="http://www.spec.org/power_ssj2008/">SPECpower benchmark</a>.
 *
 * @param hardwareName The name of the hardware vendor.
 * @see <a href="http://www.spec.org/power_ssj2008/results/res2011q1/">Machines used in the SPEC benchmark</a>
 */
public class InterpolationPowerModel(hardwareName: String) : PowerModel {
    /**
     * A [List] of average active power measured by the power analyzer(s) and accumulated by the
     * PTDaemon (Power and Temperature Daemon) for this measurement interval, displayed as watts (W).
     */
    private val averagePowerValues: List<Double> = loadAveragePowerValue(hardwareName)

    public override fun computePower(utilization: Double): Double {
        val clampedUtilization = min(1.0, max(0.0, utilization))
        val utilizationFlr = floor(clampedUtilization * 10).toInt()
        val utilizationCil = ceil(clampedUtilization * 10).toInt()
        val powerFlr: Double = getAveragePowerValue(utilizationFlr)
        val powerCil: Double = getAveragePowerValue(utilizationCil)
        val delta = (powerCil - powerFlr) / 10

        return if (utilization % 0.1 == 0.0)
            getAveragePowerValue((clampedUtilization * 10).toInt())
        else
            powerFlr + delta * (clampedUtilization - utilizationFlr.toDouble() / 10) * 100
    }

    override fun toString(): String = "InterpolationPowerModel[entries=${averagePowerValues.size}]"

    /**
     * Gets the power consumption for a given utilization percentage.
     *
     * @param index the utilization percentage in the scale from [0 to 10],
     * where 10 means 100% of utilization.
     * @return the power consumption for the given utilization percentage
     */
    private fun getAveragePowerValue(index: Int): Double = averagePowerValues[index]

    private fun loadAveragePowerValue(hardwareName: String, path: String = "spec_machines.yml"): List<Double> {
        val content = this::class
            .java.classLoader
            .getResourceAsStream(path)
        val hardwareToAveragePowerValues: Map<String, List<Double>> = Yaml().load(content)
        return hardwareToAveragePowerValues.getOrDefault(hardwareName, listOf())
    }
}
