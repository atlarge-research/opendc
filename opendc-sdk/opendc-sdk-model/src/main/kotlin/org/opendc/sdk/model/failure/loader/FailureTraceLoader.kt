/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.model.failure.loader

import org.opendc.simulator.failure.models.Failure
import org.opendc.trace.Trace
import org.opendc.trace.conv.FAILURE_DURATION
import org.opendc.trace.conv.FAILURE_INTENSITY
import org.opendc.trace.conv.FAILURE_INTERVAL
import org.opendc.trace.conv.TABLE_FAILURES
import java.io.File

/**
 * A helper class for loading failure traces into memory.
 */
public class FailureTraceLoader(private val pathToFile: File) {
    /**
     * Read the entries of the trace into [Failure]s, in trace order.
     *
     * @throws IllegalArgumentException if an entry is not a valid [Failure]
     */
    private fun parseFailures(trace: Trace): List<Failure> {
        val reader = checkNotNull(trace.getTable(TABLE_FAILURES)).newReader()

        val failureIntervalCol = reader.resolve(FAILURE_INTERVAL)
        val failureDurationCol = reader.resolve(FAILURE_DURATION)
        val failureIntensityCol = reader.resolve(FAILURE_INTENSITY)

        val failures = mutableListOf<Failure>()

        try {
            while (reader.nextRow()) {
                val failureInterval = reader.getLong(failureIntervalCol)
                val failureDuration = reader.getLong(failureDurationCol)
                val failureIntensity = reader.getDouble(failureIntensityCol)

                val failure =
                    try {
                        Failure(failureInterval, failureDuration, failureIntensity)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("Invalid failure in row ${failures.size} of $pathToFile: ${e.message}", e)
                    }

                failures.add(failure)
            }
        } finally {
            reader.close()
        }

        return failures
    }

    /**
     * Load the failure trace into a list of [Failure]s, rotated so that it begins at [startPoint].
     *
     * @param startPoint Relative position in the trace, in the range [0.0, 1.0), of the first failure.
     * The failures before it are moved to the end of the list.
     */
    public fun load(startPoint: Double = 0.0): List<Failure> {
        require(startPoint >= 0.0 && startPoint < 1.0) { "The start point must be in [0.0, 1.0), but was $startPoint" }
        require(pathToFile.exists()) { "The failure trace cannot be found at $pathToFile" }

        val failures = parseFailures(Trace.open(pathToFile, "failure"))

        val startIndex = (failures.size * startPoint).toInt()
        return failures.subList(startIndex, failures.size) + failures.subList(0, startIndex)
    }
}
