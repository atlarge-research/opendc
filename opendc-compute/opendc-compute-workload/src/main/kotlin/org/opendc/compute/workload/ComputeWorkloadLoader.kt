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

package org.opendc.compute.workload

import mu.KotlinLogging
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.trace.*
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * A helper class for loading compute workload traces into memory.
 *
 * @param baseDir The directory containing the traces.
 */
public class ComputeWorkloadLoader(private val baseDir: File) {
    /**
     * The logger for this instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The cache of workloads.
     */
    private val cache = ConcurrentHashMap<String, List<VirtualMachine>>()

    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(trace: Trace): Map<String, Builder> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        val idCol = reader.resolve(RESOURCE_ID)
        val timestampCol = reader.resolve(RESOURCE_STATE_TIMESTAMP)
        val durationCol = reader.resolve(RESOURCE_STATE_DURATION)
        val coresCol = reader.resolve(RESOURCE_CPU_COUNT)
        val usageCol = reader.resolve(RESOURCE_STATE_CPU_USAGE)

        val fragments = mutableMapOf<String, Builder>()

        return try {
            while (reader.nextRow()) {
                val id = reader.get(idCol) as String
                val time = reader.get(timestampCol) as Instant
                val duration = reader.get(durationCol) as Duration
                val cores = reader.getInt(coresCol)
                val cpuUsage = reader.getDouble(usageCol)

                val timeMs = time.toEpochMilli()
                val deadlineMs = timeMs + duration.toMillis()
                val builder = fragments.computeIfAbsent(id) { Builder() }
                builder.add(timeMs, deadlineMs, cpuUsage, cores)
            }

            fragments
        } finally {
            reader.close()
        }
    }

    /**
     * Read the metadata into a workload.
     */
    private fun parseMeta(trace: Trace, fragments: Map<String, Builder>): List<VirtualMachine> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCES)).newReader()

        val idCol = reader.resolve(RESOURCE_ID)
        val startTimeCol = reader.resolve(RESOURCE_START_TIME)
        val stopTimeCol = reader.resolve(RESOURCE_STOP_TIME)
        val cpuCountCol = reader.resolve(RESOURCE_CPU_COUNT)
        val cpuCapacityCol = reader.resolve(RESOURCE_CPU_CAPACITY)
        val memCol = reader.resolve(RESOURCE_MEM_CAPACITY)

        var counter = 0
        val entries = mutableListOf<VirtualMachine>()

        return try {
            while (reader.nextRow()) {

                val id = reader.get(idCol) as String
                if (!fragments.containsKey(id)) {
                    continue
                }

                val submissionTime = reader.get(startTimeCol) as Instant
                val endTime = reader.get(stopTimeCol) as Instant
                val cpuCount = reader.getInt(cpuCountCol)
                val cpuCapacity = reader.getDouble(cpuCapacityCol)
                val memCapacity = reader.getDouble(memCol) / 1000.0 // Convert from KB to MB
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())

                val builder = fragments.getValue(id)
                val totalLoad = builder.totalLoad

                entries.add(
                    VirtualMachine(
                        uid,
                        id,
                        cpuCount,
                        cpuCapacity,
                        memCapacity.roundToLong(),
                        totalLoad,
                        submissionTime,
                        endTime,
                        builder.build()
                    )
                )
            }

            // Make sure the virtual machines are ordered by start time
            entries.sortBy { it.startTime }

            entries
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
    public fun get(name: String, format: String): List<VirtualMachine> {
        return cache.computeIfAbsent(name) {
            val path = baseDir.resolve(it)

            logger.info { "Loading trace $it at $path" }

            val trace = Trace.open(path, format)
            val fragments = parseFragments(trace)
            parseMeta(trace, fragments)
        }
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
    private class Builder {
        /**
         * The total load of the trace.
         */
        @JvmField var totalLoad: Double = 0.0

        /**
         * The internal builder for the trace.
         */
        private val builder = SimTrace.builder()

        /**
         * Add a fragment to the trace.
         *
         * @param timestamp Timestamp at which the fragment starts (in epoch millis).
         * @param deadline Timestamp at which the fragment ends (in epoch millis).
         * @param usage CPU usage of this fragment.
         * @param cores Number of cores used.
         */
        fun add(timestamp: Long, deadline: Long, usage: Double, cores: Int) {
            val duration = max(0, deadline - timestamp)
            totalLoad += (usage * duration) / 1000.0 // avg MHz * duration = MFLOPs
            builder.add(timestamp, deadline, usage, cores)
        }

        /**
         * Build the trace.
         */
        fun build(): SimTrace = builder.build()
    }
}
