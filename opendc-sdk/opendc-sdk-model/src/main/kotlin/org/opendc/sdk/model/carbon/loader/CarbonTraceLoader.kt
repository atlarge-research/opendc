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

import org.opendc.common.units.Timestamp
import org.opendc.sdk.model.carbon.CarbonIntensitySpec
import org.opendc.simulator.compute.carbon.CarbonFragment
import org.opendc.trace.Trace
import org.opendc.trace.conv.CARBON_INTENSITY
import org.opendc.trace.conv.CARBON_TIMESTAMP
import org.opendc.trace.conv.TABLE_CARBON
import java.io.File

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

        val intensities = mutableListOf<CarbonIntensitySpec>()

        try {
            while (reader.nextRow()) {
                val startTime = reader.getInstant(startTimeCol)!!
                val carbonIntensity = reader.getDouble(carbonIntensityCol)

                intensities.add(CarbonIntensitySpec(Timestamp.ofInstant(startTime), carbonIntensity))
            }
        } finally {
            reader.close()
        }

        check(intensities.isNotEmpty()) { "The carbon trace at $pathToFile contains no entries" }

        return intensities.toCarbonFragments()
    }

    /**
     * Load the carbon trace into a list of [CarbonFragment]s, ordered by start time.
     */
    public fun load(): List<CarbonFragment> {
        require(pathToFile.exists()) { "The carbon trace cannot be found at $pathToFile" }

        val trace = Trace.open(pathToFile, "carbon")

        return parseCarbon(trace)
    }
}

/**
 * Turn the entries of a carbon trace into [CarbonFragment]s, ordered by start time, that together cover the whole
 * timeline. Each fragment lasts until the next one starts. The first fragment is extended back to [Long.MIN_VALUE], and
 * the last one lasts until [Long.MAX_VALUE].
 */
public fun List<CarbonIntensitySpec>.toCarbonFragments(): List<CarbonFragment> {
    require(isNotEmpty()) { "A carbon trace needs at least one entry" }

    val sorted = sortedBy { it.timestamp.toEpochMs() }
    return sorted.mapIndexed { i, entry ->
        val startTime = if (i == 0) Long.MIN_VALUE else entry.timestamp.toEpochMs().toLong()
        val endTime = sorted.getOrNull(i + 1)?.timestamp?.toEpochMs()?.toLong() ?: Long.MAX_VALUE
        CarbonFragment(startTime, endTime, entry.carbonIntensity)
    }
}
