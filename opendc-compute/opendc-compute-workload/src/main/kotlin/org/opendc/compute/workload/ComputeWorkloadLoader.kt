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
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.trace.*
import org.opendc.trace.opendc.OdcVmTraceFormat
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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
     * The [OdcVmTraceFormat] instance to load the traces
     */
    private val format = OdcVmTraceFormat()

    /**
     * The cache of workloads.
     */
    private val cache = ConcurrentHashMap<String, List<VirtualMachine>>()

    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(trace: Trace): Map<String, List<SimTraceWorkload.Fragment>> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        val fragments = mutableMapOf<String, MutableList<SimTraceWorkload.Fragment>>()

        return try {
            while (reader.nextRow()) {
                val id = reader.get(RESOURCE_ID)
                val time = reader.get(RESOURCE_STATE_TIMESTAMP)
                val duration = reader.get(RESOURCE_STATE_DURATION)
                val cores = reader.getInt(RESOURCE_CPU_COUNT)
                val cpuUsage = reader.getDouble(RESOURCE_STATE_CPU_USAGE)

                val fragment = SimTraceWorkload.Fragment(
                    time.toEpochMilli(),
                    duration.toMillis(),
                    cpuUsage,
                    cores
                )

                fragments.computeIfAbsent(id) { mutableListOf() }.add(fragment)
            }

            fragments
        } finally {
            reader.close()
        }
    }

    /**
     * Read the metadata into a workload.
     */
    private fun parseMeta(trace: Trace, fragments: Map<String, List<SimTraceWorkload.Fragment>>): List<VirtualMachine> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCES)).newReader()

        var counter = 0
        val entries = mutableListOf<VirtualMachine>()

        return try {
            while (reader.nextRow()) {

                val id = reader.get(RESOURCE_ID)
                if (!fragments.containsKey(id)) {
                    continue
                }

                val submissionTime = reader.get(RESOURCE_START_TIME)
                val endTime = reader.get(RESOURCE_STOP_TIME)
                val maxCores = reader.getInt(RESOURCE_CPU_COUNT)
                val requiredMemory = reader.getDouble(RESOURCE_MEM_CAPACITY) / 1000.0 // Convert from KB to MB
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())

                val vmFragments = fragments.getValue(id).asSequence()
                val totalLoad = vmFragments.sumOf { (it.usage * it.duration) / 1000.0 } // avg MHz * duration = MFLOPs

                entries.add(
                    VirtualMachine(
                        uid,
                        id,
                        maxCores,
                        requiredMemory.roundToLong(),
                        totalLoad,
                        submissionTime,
                        endTime,
                        vmFragments
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
     * Load the trace with the specified [name].
     */
    public fun get(name: String): List<VirtualMachine> {
        return cache.computeIfAbsent(name) {
            val path = baseDir.resolve(it)

            logger.info { "Loading trace $it at $path" }

            val trace = format.open(path.toURI().toURL())
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
}
