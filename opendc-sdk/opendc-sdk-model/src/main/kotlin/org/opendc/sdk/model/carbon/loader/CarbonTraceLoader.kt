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

package org.opendc.sdk.model.carbon.loader

import org.opendc.simulator.compute.carbon.CarbonFragment
import org.opendc.trace.Trace
import org.opendc.trace.conv.CARBON_INTENSITY
import org.opendc.trace.conv.CARBON_TIMESTAMP
import org.opendc.trace.conv.TABLE_CARBON
import java.io.File
import java.time.Instant

/**
 * A helper class for loading carbon intensity traces into memory.
 */
public class CarbonTraceLoader(private val pathToFile: File) {
    /**
     * Read the carbon intensity entries of the trace into fragments.
     */
    private fun parseCarbon(trace: Trace): List<CarbonFragment> {
        val reader = checkNotNull(trace.getTable(TABLE_CARBON)).newReader()

        val startTimeCol = reader.resolve(CARBON_TIMESTAMP)
        val carbonIntensityCol = reader.resolve(CARBON_INTENSITY)

        val builder = CarbonFragmentBuilder()

        try {
            while (reader.nextRow()) {
                val startTime = reader.getInstant(startTimeCol)!!
                val carbonIntensity = reader.getDouble(carbonIntensityCol)

                builder.add(startTime, carbonIntensity)
            }
        } finally {
            reader.close()
        }

        check(builder.fragments.isNotEmpty()) { "The carbon trace at $pathToFile contains no entries" }

        // Make sure the fragments are ordered by start time and cover the whole timeline
        builder.fixReportTimes()

        return builder.fragments
    }

    /**
     * Load the carbon trace into a list of [CarbonFragment]s, ordered by start time.
     */
    public fun load(): List<CarbonFragment> {
        require(pathToFile.exists()) { "The carbon trace cannot be found at $pathToFile" }

        val trace = Trace.open(pathToFile, "carbon")

        return parseCarbon(trace)
    }

    /**
     * A builder for the fragments of a carbon trace.
     */
    private class CarbonFragmentBuilder {
        /**
         * The fragments of the trace.
         */
        val fragments: MutableList<CarbonFragment> = mutableListOf()

        /**
         * Add a fragment to the trace.
         *
         * @param startTime Timestamp at which the fragment starts.
         * @param carbonIntensity The carbon intensity during this fragment.
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

        /**
         * Sort the fragments by start time and let each fragment last until the next one starts.
         * The first fragment is extended back to [Long.MIN_VALUE], so every timestamp is covered.
         */
        fun fixReportTimes() {
            fragments.sortBy { it.startTime }

            // For each fragment, set the end time to the start time of the next fragment
            for (i in 0..fragments.size - 2) {
                fragments[i].endTime = fragments[i + 1].startTime
            }

            // Extend the first fragment back to the minimum value
            fragments[0].startTime = Long.MIN_VALUE
        }
    }
}
