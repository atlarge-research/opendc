/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.compute.carbon

import java.time.Instant

/**
 * A virtual machine workload.
 *
 * @param uid The unique identifier of the virtual machine.
 * @param name The name of the virtual machine.
 * @param cpuCapacity The required CPU capacity for the VM in MHz.
 * @param cpuCount The number of vCPUs in the VM.
 * @param memCapacity The provisioned memory for the VM in MB.
 * @param startTime The start time of the VM.
 * @param stopTime The stop time of the VM.
 * @param trace The trace that belong to this VM.
 * @param interferenceProfile The interference profile of this virtual machine.
 */
public data class CarbonFragment(
    var startTime: Long,
    var endTime: Long,
    var carbonIntensity: Float,
) {
    init {
        require(endTime > startTime) {
            "The end time of a report should be higher than the start time -> start time: $startTime, end time: $endTime"
        }
        require(carbonIntensity >= 0.0) { "carbon intensity cannot be negative" }
    }
}

public class CarbonTrace(reports: List<CarbonFragment>? = null) {
    private var index: Int = 0
    private val numberOfReports = reports?.size
    private val reports = reports?.sortedBy { it.startTime }

    private fun hasPreviousReport(): Boolean {
        return index > 0
    }

    private fun hasNextReport(): Boolean {
        if (numberOfReports == null) {
            return false
        }

        return index < numberOfReports
    }

    public fun getCarbonIntensity(timestamp: Instant): Float {
        return getCarbonIntensity(timestamp.toEpochMilli())
    }

    /**
     * Get the carbon intensity of the energy at a given timestamp
     * Returns the carbon intensity of the first or last [CarbonFragment]
     * if the given timestamp is outside the information
     *
     * @param timestamp
     * @return The carbon intensity at the given timestamp in gCO2/kWh
     */
    public fun getCarbonIntensity(timestamp: Long): Float {
        if (reports == null) {
            return 0.0f
        }

        var currentFragment: CarbonFragment

        while (true) {
            currentFragment = reports[index]

            if (currentFragment.startTime > timestamp) {
                if (hasPreviousReport()) {
                    index--
                    continue
                }
                break
            }

            if (currentFragment.endTime <= timestamp) {
                if (hasNextReport()) {
                    index++
                    continue
                }
                break
            }

            break
        }

        return currentFragment.carbonIntensity
    }
}
