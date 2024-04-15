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

import mu.KotlinLogging
import org.opendc.trace.Trace
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.resourceCarbonIntensity
import org.opendc.trace.conv.resourceStartTime
import java.io.File
import java.lang.ref.SoftReference
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A helper class for loading compute workload traces into memory.
 *
 * @param baseDir The directory containing the traces.
 */
public class CarbonTraceLoader {
    /**
     * The logger for this instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The cache of workloads.
     */
    private val cache = ConcurrentHashMap<String, SoftReference<List<CarbonIntensityFragment>>>()

    private val builder = CarbonFragmentBuilder();
    /**
     * Read the metadata into a workload.
     */
    private fun parseCarbon(
        trace: Trace
    ): List<CarbonIntensityFragment> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCES)).newReader()

        val startTimeCol = reader.resolve(resourceStartTime)
        val carbonIntensityCol = reader.resolve(resourceCarbonIntensity)

        val entries = mutableListOf<CarbonIntensityFragment>()

        try {
            while (reader.nextRow()) {
                val startTime = reader.getInstant(startTimeCol)!!
                val carbonIntensity = reader.getDouble(carbonIntensityCol)

                builder.add(startTime, carbonIntensity)
            }

            // Make sure the virtual machines are ordered by start time
            builder.fixReportTimes()

            return entries
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            reader.close()
        }
    }

    /**
     * Load the trace with the specified [name] and [format].
     */
    public fun get(
        pathToFile: File,
    ): List<CarbonIntensityFragment> {

        val trace = Trace.open(pathToFile, "gwf")


//        val ref =
//            cache.compute(name) { key, oldVal ->
//                val inst = oldVal?.get()
//                if (inst == null) {
//                    val path = baseDir.resolve(key)
//
//                    logger.info { "Loading trace $key at $path" }
//
//                    val trace = Trace.open(path, format)
//                    val carbonReports = parseCarbon(trace)
//
//                    SoftReference(carbonReports)
//                } else {
//                    oldVal
//                }
//            }

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
    private class CarbonFragmentBuilder {
        /**
         * The total load of the trace.
         */
        private val reports: MutableList<CarbonIntensityFragment> = mutableListOf<CarbonIntensityFragment>();

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
            reports.add(
                CarbonIntensityFragment(startTime.toEpochMilli(), Long.MAX_VALUE, carbonIntensity)
            )
        }

        fun fixReportTimes() {
            reports.sortBy{it.startTime}

            // For each report, set the end time to the start time of the next report
            for (i in 0..reports.size - 2) {
                reports[i].endTime = reports[i+1].startTime
            }

            // Set the start time of each report to the minimum value
            reports[0].startTime = Long.MIN_VALUE
        }
    }
}
