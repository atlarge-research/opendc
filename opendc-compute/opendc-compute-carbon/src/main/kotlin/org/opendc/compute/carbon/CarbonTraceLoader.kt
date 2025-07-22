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

import org.opendc.simulator.compute.power.CarbonFragment
import org.opendc.trace.Trace
import org.opendc.trace.conv.CARBON_INTENSITY
import org.opendc.trace.conv.CARBON_TIMESTAMP
import org.opendc.trace.conv.TABLE_CARBON
import java.io.File
import java.lang.ref.SoftReference
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A helper class for loading compute workload traces into memory.
 *
 */
public class CarbonTraceLoader {
    /**
     * The cache of workloads.
     */
    private val cache = ConcurrentHashMap<String, SoftReference<List<CarbonFragment>>>()

    private val builder = CarbonFragmentNewBuilder()

    /**
     * Read the metadata into a workload.
     */
    private fun parseCarbon(trace: Trace): List<CarbonFragment> {
        val reader = checkNotNull(trace.getTable(TABLE_CARBON)).newReader()

        val startTimeCol = reader.resolve(CARBON_TIMESTAMP)
        val carbonIntensityCol = reader.resolve(CARBON_INTENSITY)

        try {
            while (reader.nextRow()) {
                val startTime = reader.getInstant(startTimeCol)!!
                val carbonIntensity = reader.getDouble(carbonIntensityCol)

                builder.add(startTime, carbonIntensity)
            }

            // Make sure the virtual machines are ordered by start time
            builder.fixReportTimes()

            return builder.fragments
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            reader.close()
        }
    }

    /**
     * Load the Carbon Trace at the given path.
     */
    public fun get(pathToFile: File): List<CarbonFragment> {
        val trace = Trace.open(pathToFile, "carbon")

        return parseCarbon(trace)
    }

    /**
     * Clear the workload cache.
     */
    public fun reset() {
        cache.clear()
    }

    /**
     * A builder for a VM trace.
     */
    private class CarbonFragmentNewBuilder {
        /**
         * The total load of the trace.
         */
        val fragments: MutableList<CarbonFragment> = mutableListOf()

        /**
         * Add a fragment to the trace.
         *
         * @param startTime Timestamp at which the fragment starts (in epoch millis).
         * @param carbonIntensity The carbon intensity during this fragment
         */
        fun add(
            startTime: Instant,
            carbonIntensity: Double,
        ) {
            fragments.add(
                CarbonFragment(
                    startTime.toEpochMilli(),
                    Long.MAX_VALUE,
                    carbonIntensity,
                ),
            )
        }

        fun fixReportTimes() {
            fragments.sortBy { it.startTime }

            // For each report, set the end time to the start time of the next report
            for (i in 0..fragments.size - 2) {
                fragments[i].endTime = fragments[i + 1].startTime
            }

            // Set the start time of each report to the minimum value
            fragments[0].startTime = Long.MIN_VALUE
        }
    }
}
