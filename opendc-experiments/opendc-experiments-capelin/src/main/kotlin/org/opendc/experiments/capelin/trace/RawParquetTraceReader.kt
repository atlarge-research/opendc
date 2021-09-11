/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.capelin.trace

import org.opendc.experiments.capelin.trace.bp.BPTraceFormat
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.trace.*
import java.io.File
import java.util.UUID

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param path The directory of the traces.
 */
class RawParquetTraceReader(private val path: File) {
    /**
     * The [Trace] that represents this trace.
     */
    private val trace = BPTraceFormat().open(path.toURI().toURL())

    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(): Map<String, List<SimTraceWorkload.Fragment>> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        val fragments = mutableMapOf<String, MutableList<SimTraceWorkload.Fragment>>()

        return try {
            while (reader.nextRow()) {
                val id = reader.get(RESOURCE_STATE_ID)
                val time = reader.get(RESOURCE_STATE_TIMESTAMP)
                val duration = reader.get(RESOURCE_STATE_DURATION)
                val cores = reader.getInt(RESOURCE_STATE_NCPUS)
                val cpuUsage = reader.getDouble(RESOURCE_STATE_CPU_USAGE)

                val fragment = SimTraceWorkload.Fragment(
                    time.toEpochMilli(),
                    duration.toMillis(),
                    cpuUsage,
                    cores
                )

                fragments.getOrPut(id) { mutableListOf() }.add(fragment)
            }

            fragments
        } finally {
            reader.close()
        }
    }

    /**
     * Read the metadata into a workload.
     */
    private fun parseMeta(fragments: Map<String, List<SimTraceWorkload.Fragment>>): List<TraceEntry<SimWorkload>> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCES)).newReader()

        var counter = 0
        val entries = mutableListOf<TraceEntry<SimWorkload>>()

        return try {
            while (reader.nextRow()) {

                val id = reader.get(RESOURCE_ID)
                if (!fragments.containsKey(id)) {
                    continue
                }

                val submissionTime = reader.get(RESOURCE_START_TIME)
                val endTime = reader.get(RESOURCE_STOP_TIME)
                val maxCores = reader.getInt(RESOURCE_NCPUS)
                val requiredMemory = reader.getDouble(RESOURCE_MEM_CAPACITY) / 1000.0 // Convert from KB to MB
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())

                val vmFragments = fragments.getValue(id).asSequence()
                val totalLoad = vmFragments.sumOf { it.usage } * 5 * 60 // avg MHz * duration = MFLOPs
                val workload = SimTraceWorkload(vmFragments)
                entries.add(
                    TraceEntry(
                        uid, id, submissionTime.toEpochMilli(), workload,
                        mapOf(
                            "submit-time" to submissionTime.toEpochMilli(),
                            "end-time" to endTime.toEpochMilli(),
                            "total-load" to totalLoad,
                            "cores" to maxCores,
                            "required-memory" to requiredMemory.toLong(),
                            "workload" to workload
                        )
                    )
                )
            }

            entries
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            reader.close()
        }
    }

    /**
     * The entries in the trace.
     */
    private val entries: List<TraceEntry<SimWorkload>>

    init {
        val fragments = parseFragments()
        entries = parseMeta(fragments)
    }

    /**
     * Read the entries in the trace.
     */
    fun read(): List<TraceEntry<SimWorkload>> = entries
}
