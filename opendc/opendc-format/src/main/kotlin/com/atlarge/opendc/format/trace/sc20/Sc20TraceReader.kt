/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.format.trace.sc20

import com.atlarge.opendc.compute.core.image.FlopsHistoryFragment
import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.core.User
import com.atlarge.opendc.compute.core.workload.IMAGE_PERF_INTERFERENCE_MODEL
import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import com.atlarge.opendc.format.trace.TraceEntry
import com.atlarge.opendc.format.trace.TraceReader
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param traceDirectory The directory of the traces.
 * @param performanceInterferenceModel The performance model covering the workload in the VM trace.
 */
class Sc20TraceReader(
    traceDirectory: File,
    performanceInterferenceModel: PerformanceInterferenceModel,
    selectedVms: List<String>,
    random: Random
) : TraceReader<VmWorkload> {
    /**
     * The internal iterator to use for this reader.
     */
    private val iterator: Iterator<TraceEntry<VmWorkload>>

    /**
     * Initialize the reader.
     */
    init {
        val entries = mutableMapOf<UUID, TraceEntry<VmWorkload>>()

        val timestampCol = 0
        val cpuUsageCol = 1
        val coreCol = 12
        val vmIdCol = 19
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
                var timestamp = -1L
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
                    var last: FlopsHistoryFragment? = null

                    BufferedReader(FileReader(vmFile)).use { reader ->
                        reader.lineSequence()
                            .chunked(128)
                            .forEach { lines ->
                                // val res = ArrayList<FlopsHistoryFragment>(lines.size)
                                for (line in lines) {
                                    // Ignore comments in the trace
                                    if (line.startsWith("#") || line.isBlank()) {
                                        continue
                                    }

                                    val values = line.split("    ")
                                    val cpuUsage = values[cpuUsageCol].trim().toDouble() // MHz
                                    requiredMemory = max(requiredMemory, values[provisionedMemoryCol].trim().toLong())
                                    maxCores = max(maxCores, cores)

                                    val flops: Long = (cpuUsage * 5 * 60).toLong()

                                    last = if (last != null && last!!.flops == 0L && flops == 0L) {
                                        val oldFragment = last!!
                                        FlopsHistoryFragment(
                                            oldFragment.tick,
                                            oldFragment.flops + flops,
                                            oldFragment.duration + traceInterval,
                                            cpuUsage,
                                            cores
                                        )
                                    } else {
                                        val fragment =
                                            FlopsHistoryFragment(timestamp, flops, traceInterval, cpuUsage, cores)
                                        if (last != null) {
                                            yield(last!!)
                                        }
                                        fragment
                                    }
                                }
                                // yieldAll(res)
                            }

                        if (last != null) {
                            yield(last!!)
                        }
                    }
                }

                val uuid = UUID(0, idx.toLong())

                val relevantPerformanceInterferenceModelItems =
                    PerformanceInterferenceModel(
                        performanceInterferenceModel.items.filter { it.workloadNames.contains(vmId) }.toSet(),
                        Random(random.nextInt())
                    )
                val vmWorkload = VmWorkload(
                    uuid, "VM Workload $vmId", UnnamedUser,
                    VmImage(
                        uuid,
                        vmId,
                        mapOf(IMAGE_PERF_INTERFERENCE_MODEL to relevantPerformanceInterferenceModelItems),
                        flopsFragments.asSequence(),
                        maxCores,
                        requiredMemory
                    )
                )
                entries[uuid] = TraceEntryImpl(
                    minTime,
                    vmWorkload
                )
            }

        // Create the entry iterator
        iterator = entries.values.sortedBy { it.submissionTime }.iterator()
    }

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): TraceEntry<VmWorkload> = iterator.next()

    override fun close() {}

    /**
     * An unnamed user.
     */
    private object UnnamedUser : User {
        override val name: String = "<unnamed>"
        override val uid: UUID = UUID.randomUUID()
    }

    /**
     * An entry in the trace.
     */
    private data class TraceEntryImpl(
        override var submissionTime: Long,
        override val workload: VmWorkload
    ) : TraceEntry<VmWorkload>
}
