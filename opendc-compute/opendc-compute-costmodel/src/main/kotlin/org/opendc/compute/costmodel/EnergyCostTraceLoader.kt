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

package org.opendc.compute.costmodel

import org.opendc.simulator.compute.costmodel.EnergyCostFragment
import org.opendc.trace.Trace
import org.opendc.trace.conv.ENERGY_PRICE_COST
import org.opendc.trace.conv.ENERGY_PRICE_TIMESTAMP
import org.opendc.trace.conv.TABLE_COSTMODEL
import java.io.File
import java.lang.ref.SoftReference
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

public class EnergyCostTraceLoader {
    private val cache = ConcurrentHashMap<String, SoftReference<List<EnergyCostFragment>>>()

    private val builder = EnergyCostFragmentNewBuilder()

    private fun parseEnergy(trace: Trace): List<EnergyCostFragment> {
        val reader = checkNotNull(trace.getTable(TABLE_COSTMODEL)).newReader()

        val startTimeCol = reader.resolve(ENERGY_PRICE_TIMESTAMP)
        val energyPriceCol = reader.resolve(ENERGY_PRICE_COST)

        try {
            while (reader.nextRow()) {
                val startTime = reader.getInstant(startTimeCol)!!
                val energyPriceCost = reader.getDouble(energyPriceCol)

                builder.add(startTime, energyPriceCost)
            }

            builder.fixReportTimes()

            return builder.fragments
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            reader.close()
        }
    }

    public fun get(pathToFile: File): List<EnergyCostFragment> {
        val trace = Trace.open(pathToFile, "costmodel")

        return parseEnergy(trace)
    }

    public fun reset() {
        cache.clear()
    }

    private class EnergyCostFragmentNewBuilder {
        val fragments: MutableList<EnergyCostFragment> = mutableListOf()

        /**
         * Add a fragment to the trace.
         *
         * @param startTime Timestamp at which the fragment starts (in epoch millis).
         * @param energyPriceCost the price per kwh
         */
        fun add(
            startTime: Instant,
            energyPriceCost: Double,
        ) {
            fragments.add(
                EnergyCostFragment(
                    startTime.toEpochMilli(),
                    Long.MAX_VALUE,
                    energyPriceCost,
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
