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

package org.opendc.compute.simulator.failure.models

import org.opendc.trace.Trace
import org.opendc.trace.conv.FAILURE_DURATION
import org.opendc.trace.conv.FAILURE_INTENSITY
import org.opendc.trace.conv.FAILURE_START
import org.opendc.trace.conv.TABLE_FAILURES
import java.io.File
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

/**
 * A helper class for loading compute workload traces into memory.
 *
 * @param baseDir The directory containing the traces.
 */
public class FailureTraceLoader {
    /**
     * The cache of workloads.
     */
    private val cache = ConcurrentHashMap<String, SoftReference<List<Failure>>>()

    /**
     * Read the metadata into a workload.
     */
    private fun parseFailure(trace: Trace): List<Failure> {
        val reader = checkNotNull(trace.getTable(TABLE_FAILURES)).newReader()

        val failureStartTimeCol = reader.resolve(FAILURE_START)
        val failureDurationCol = reader.resolve(FAILURE_DURATION)
        val failureIntensityCol = reader.resolve(FAILURE_INTENSITY)

        val entries = mutableListOf<Failure>()

        try {
            while (reader.nextRow()) {
                val failureStartTime = reader.getLong(failureStartTimeCol)
                val failureDuration = reader.getLong(failureDurationCol)
                val failureIntensity = reader.getDouble(failureIntensityCol)

                entries.add(Failure(failureStartTime, failureDuration, failureIntensity))
            }

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
    public fun get(pathToFile: File): List<Failure> {
        val trace = Trace.open(pathToFile, "failure")

        return parseFailure(trace)
    }

    /**
     * Clear the workload cache.
     */
    public fun reset() {
        cache.clear()
    }
}
