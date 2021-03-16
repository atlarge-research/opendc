package org.opendc.compute.simulator.power.models

import org.opendc.simulator.compute.SimBareMetalMachine
import java.time.Clock
import java.util.*

/**
 * The CPU power model derived from the iCanCloud simulator.
 *
 * @param machine The [SimBareMetalMachine] that the model is measuring.
 * @param clock The virtual [Clock] to track the time spent at each p-state.
 * @property cpuPowerMeter A [MutableMap] that contains CPU frequencies ([Double]) in GHz
 *                      as keys and the time ([Long]) spent in that frequency range in seconds.
 * @property pStatesToPower A [TreeMap] that contains the frequency ([Double]) of corresponding p-state in GHz
 *                      as keys and the energy ([Double]) consumption of that state in Watts.
 * @property pStatesRange A [Pair] in which the fist and second elements are the lower and upper bounds of the
 *                      consumption values of the p-states respectively.
 * @property lastMeasureTime The last update time of the [cpuPowerMeter].
 * @property currPState The p-state that the model is currently in.
 */
public class PStatePowerModel(
    private val machine: SimBareMetalMachine,
    private val clock: Clock,
) {
    // TODO: Extract the power meter out of the model.
    private val cpuPowerMeter = mutableMapOf<Double, Long>()
    private val pStatesToPower = TreeMap<Double, Double>()
    private val pStatesRange: Pair<Double, Double>
    private var lastMeasureTime: Long
    private var currPState: Double

    init {
        loadPStates(this)
        pStatesRange = Pair(pStatesToPower.keys.first(), pStatesToPower.keys.last())
        pStatesToPower.keys.forEach { cpuPowerMeter[it] = 0L }
        currPState = pStatesRange.first
        lastMeasureTime = getClockInstant()
        updateCpuPowerMeter()
    }

    /** Recorde the elapsed time to the corresponding p-state. */
    public fun updateCpuPowerMeter() {
        val newMeasureTime = getClockInstant()
        val newMaxFreq: Double = getMaxCpuSpeedInGHz()
        assert(newMaxFreq in pStatesRange.first..pStatesRange.second) {
            "The maximum frequency $newMaxFreq is not in the range of the P-state frequency " +
                "from ${pStatesRange.first} to ${pStatesRange.second}."
        }

        // Update the current p-state level on which the CPU is running.
        val newPState = pStatesToPower.ceilingKey(newMaxFreq)

        // Add the time elapsed to the previous state.
        cpuPowerMeter.merge(currPState, newMeasureTime - lastMeasureTime, Long::plus)

        // Update the current states.
        currPState = newPState
        lastMeasureTime = newMeasureTime
    }

    /** Get the power value of the energy consumption level at which the CPU is working. */
    public fun getInstantCpuPower(): Double =
        pStatesToPower.getOrDefault(currPState, 0.0)

    /** Get the accumulated power consumption up until now. */
    public fun getAccumulatedCpuPower(): Double =
        pStatesToPower.keys
            .map {
                pStatesToPower.getOrDefault(it, 0.0) *
                    cpuPowerMeter.getOrDefault(it, 0.0).toDouble()
            }.sum()

    private fun getClockInstant() = clock.millis() / 1000

    /** Get the maximum frequency of the CPUs in GHz as that of the package.
     * @see <a href="https://www.intel.vn/content/dam/www/public/us/en/documents/datasheets/10th-gen-core-families-datasheet-vol-1-datasheet.pdf">
     *     on page 34.
     */
    private fun getMaxCpuSpeedInGHz() = (machine.speed.maxOrNull() ?: 0.0) / 1000

    public companion object PStatesLoader {
        private fun loadPStates(pStatePowerModel: PStatePowerModel) {
            // TODO: Dynamically load configuration.
            // See P4 of https://www.intel.com/content/dam/support/us/en/documents/motherboards/server/sb/power_management_of_intel_architecture_servers.pdf
            pStatePowerModel.pStatesToPower.putAll(
                sortedMapOf(
                    3.6 to 103.0,
                    3.4 to 94.0,
                    3.2 to 85.0,
                    3.0 to 76.0,
                    2.8 to 8.0,
                )
            )
        }
    }
}
