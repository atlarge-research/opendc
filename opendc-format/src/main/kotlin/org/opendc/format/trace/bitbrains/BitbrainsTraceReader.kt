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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
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

        var timestampCol = 0
        var coreCol = 0
        var cpuUsageCol = 0
        var provisionedMemoryCol = 0
        val traceInterval = 5 * 60 * 1000L

        traceDirectory.walk()
            .filterNot { it.isDirectory }
            .forEach { vmFile ->
                println(vmFile)
                val flopsHistory = mutableListOf<SimTraceWorkload.Fragment>()
                var vmId = -1L
                var cores = -1
                var requiredMemory = -1L
                var startTime = -1L

                BufferedReader(FileReader(vmFile)).use { reader ->
                    reader.lineSequence()
                        .filter { line ->
                            // Ignore comments in the trace
                            !line.startsWith("#") && line.isNotBlank()
                        }
                        .forEachIndexed { idx, line ->
                            val values = line.split(";\t")

                            // Parse GWF header
                            if (idx == 0) {
                                val header = values.mapIndexed { col, name -> Pair(name.trim(), col) }.toMap()
                                timestampCol = header["Timestamp [ms]"]!!
                                coreCol = header["CPU cores"]!!
                                cpuUsageCol = header["CPU usage [MHZ]"]!!
                                provisionedMemoryCol = header["Memory capacity provisioned [KB]"]!!
                                return@forEachIndexed
                            }

                            vmId = vmFile.nameWithoutExtension.trim().toLong()
                            startTime = min(startTime, values[timestampCol].trim().toLong() - 5 * 60)
                            cores = values[coreCol].trim().toInt()
                            val cpuUsage = values[cpuUsageCol].trim().toDouble() // MHz
                            requiredMemory = (values[provisionedMemoryCol].trim().toDouble() / 1000).toLong()

                            if (flopsHistory.isEmpty()) {
                                flopsHistory.add(SimTraceWorkload.Fragment(traceInterval, cpuUsage, cores))
                            } else {
                                if (flopsHistory.last().usage != cpuUsage) {
                                    flopsHistory.add(
                                        SimTraceWorkload.Fragment(
                                            traceInterval,
                                            cpuUsage,
                                            cores
                                        )
                                    )
                                } else {
                                    val oldFragment = flopsHistory.removeAt(flopsHistory.size - 1)
                                    flopsHistory.add(
                                        SimTraceWorkload.Fragment(
                                            oldFragment.duration + traceInterval,
                                            cpuUsage,
                                            cores
                                        )
                                    )
                                }
                            }
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
                        "cores" to cores,
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
