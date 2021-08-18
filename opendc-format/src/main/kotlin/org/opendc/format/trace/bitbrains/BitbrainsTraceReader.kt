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

package org.opendc.format.trace.bitbrains

import org.opendc.format.trace.TraceEntry
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * A [TraceReader] for the public VM workload trace format.
 *
 * @param traceDirectory The directory of the traces.
 */
public class BitbrainsTraceReader(traceDirectory: File) : TraceReader<SimWorkload> {
    /**
     * The internal iterator to use for this reader.
     */
    private val iterator: Iterator<TraceEntry<SimWorkload>>

    /**
     * Initialize the reader.
     */
    init {
        val entries = mutableMapOf<Long, TraceEntry<SimWorkload>>()
        val traceInterval = 5 * 60 * 1000L

        traceDirectory.walk()
            .filterNot { it.isDirectory }
            .filter { it.extension == "csv" }
            .forEach { vmFile ->
                val flopsHistory = mutableListOf<SimTraceWorkload.Fragment>()
                var vmId = -1L
                var maxCores = Int.MIN_VALUE
                var requiredMemory = Long.MIN_VALUE
                var startTime = Long.MAX_VALUE
                var lastTimestamp = Long.MIN_VALUE

                BitbrainsRawTraceReader(FileInputStream(vmFile)).use { reader ->
                    reader.forEach { entry ->
                        val timestamp = entry.timestamp * 1000L
                        val cpuUsage = entry.cpuUsage
                        vmId = vmFile.nameWithoutExtension.trim().toLong()
                        val cores = entry.cpuCores
                        maxCores = max(maxCores, cores)
                        requiredMemory = max(requiredMemory, (entry.memCapacity / 1000).toLong())

                        if (lastTimestamp < 0) {
                            lastTimestamp = timestamp - 5 * 60 * 1000L
                            startTime = min(startTime, lastTimestamp)
                        }

                        if (flopsHistory.isEmpty()) {
                            flopsHistory.add(SimTraceWorkload.Fragment(lastTimestamp, traceInterval, cpuUsage, cores))
                        } else {
                            val last = flopsHistory.last()
                            val duration = timestamp - lastTimestamp
                            // Perform run-length encoding
                            if (duration == 0L || last.usage == cpuUsage) {
                                flopsHistory[flopsHistory.size - 1] = last.copy(duration = last.duration + duration)
                            } else {
                                flopsHistory.add(
                                    SimTraceWorkload.Fragment(
                                        lastTimestamp,
                                        duration,
                                        cpuUsage,
                                        cores
                                    )
                                )
                            }
                        }

                        lastTimestamp = timestamp
                    }
                }

                val uuid = UUID(0L, vmId)

                val workload = SimTraceWorkload(flopsHistory.asSequence())
                entries[vmId] = TraceEntry(
                    uuid,
                    vmId.toString(),
                    startTime,
                    workload,
                    mapOf(
                        "cores" to maxCores,
                        "required-memory" to requiredMemory,
                        "workload" to workload
                    )
                )
            }

        // Create the entry iterator
        iterator = entries.values.sortedBy { it.start }.iterator()
    }

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): TraceEntry<SimWorkload> = iterator.next()

    override fun close() {}
}
