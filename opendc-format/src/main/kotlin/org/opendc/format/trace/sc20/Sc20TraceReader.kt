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

package org.opendc.format.trace.sc20

import org.opendc.format.trace.TraceEntry
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.interference.IMAGE_PERF_INTERFERENCE_MODEL
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param traceDirectory The directory of the traces.
 * @param performanceInterferenceModel The performance model covering the workload in the VM trace.
 */
public class Sc20TraceReader(
    traceDirectory: File,
    performanceInterferenceModel: PerformanceInterferenceModel,
    selectedVms: List<String>,
    random: Random
) : TraceReader<SimWorkload> {
    /**
     * The internal iterator to use for this reader.
     */
    private val iterator: Iterator<TraceEntry<SimWorkload>>

    /**
     * Initialize the reader.
     */
    init {
        val entries = mutableMapOf<UUID, TraceEntry<SimWorkload>>()

        val timestampCol = 0
        val cpuUsageCol = 1
        val coreCol = 12
        val provisionedMemoryCol = 20
        val traceInterval = 5 * 60 * 1000L

        val vms = if (selectedVms.isEmpty()) {
            traceDirectory.walk()
                .filterNot { it.isDirectory }
                .filter { it.extension == "csv" || it.extension == "txt" }
                .toList()
        } else {
            selectedVms.map {
                File(traceDirectory, it)
            }
        }

        vms
            .forEachIndexed { idx, vmFile ->
                println(vmFile)

                var vmId = ""
                var maxCores = -1
                var requiredMemory = -1L
                var timestamp: Long
                var cores = -1
                var minTime = Long.MAX_VALUE

                BufferedReader(FileReader(vmFile)).use { reader ->
                    reader.lineSequence()
                        .filter { line ->
                            // Ignore comments in the trace
                            !line.startsWith("#") && line.isNotBlank()
                        }
                        .forEach { line ->
                            val values = line.split("    ")

                            vmId = vmFile.name
                            timestamp = (values[timestampCol].trim().toLong() - 5 * 60) * 1000L
                            cores = values[coreCol].trim().toInt()
                            requiredMemory = max(requiredMemory, values[provisionedMemoryCol].trim().toLong())
                            maxCores = max(maxCores, cores)
                            minTime = min(minTime, timestamp)
                        }
                }

                val flopsFragments = sequence {
                    var last: SimTraceWorkload.Fragment? = null

                    BufferedReader(FileReader(vmFile)).use { reader ->
                        reader.lineSequence()
                            .chunked(128)
                            .forEach { lines ->
                                for (line in lines) {
                                    // Ignore comments in the trace
                                    if (line.startsWith("#") || line.isBlank()) {
                                        continue
                                    }

                                    val values = line.split("    ")
                                    val cpuUsage = values[cpuUsageCol].trim().toDouble() // MHz
                                    requiredMemory = max(requiredMemory, values[provisionedMemoryCol].trim().toLong())
                                    maxCores = max(maxCores, cores)

                                    last = if (last != null && last!!.usage == 0.0 && cpuUsage == 0.0) {
                                        val oldFragment = last!!
                                        SimTraceWorkload.Fragment(
                                            oldFragment.duration + traceInterval,
                                            cpuUsage,
                                            cores
                                        )
                                    } else {
                                        val fragment =
                                            SimTraceWorkload.Fragment(traceInterval, cpuUsage, cores)
                                        if (last != null) {
                                            yield(last!!)
                                        }
                                        fragment
                                    }
                                }
                            }

                        if (last != null) {
                            yield(last!!)
                        }
                    }
                }

                val uuid = UUID(0, idx.toLong())

                val relevantPerformanceInterferenceModelItems =
                    PerformanceInterferenceModel(
                        performanceInterferenceModel.items.filter { it.workloadNames.contains(vmId) }.toSortedSet(),
                        Random(random.nextInt())
                    )
                val workload = SimTraceWorkload(flopsFragments.asSequence())
                entries[uuid] = TraceEntry(
                    uuid,
                    vmId,
                    minTime,
                    workload,
                    mapOf(
                        IMAGE_PERF_INTERFERENCE_MODEL to relevantPerformanceInterferenceModelItems,
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
