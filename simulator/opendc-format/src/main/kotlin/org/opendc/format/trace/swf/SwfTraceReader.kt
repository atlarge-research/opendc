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

package org.opendc.format.trace.swf

import org.opendc.compute.core.workload.VmWorkload
import org.opendc.compute.simulator.SimWorkloadImage
import org.opendc.core.User
import org.opendc.format.trace.TraceEntry
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.workload.SimTraceWorkload
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

/**
 * A [TraceReader] for reading SWF traces into VM-modeled workloads.
 *
 * The standard is defined by the PWA, see here: https://www.cse.huji.ac.il/labs/parallel/workload/swf.html
 *
 * @param file The trace file.
 */
public class SwfTraceReader(
    file: File,
    maxNumCores: Int = -1
) : TraceReader<VmWorkload> {
    /**
     * The internal iterator to use for this reader.
     */
    private val iterator: Iterator<TraceEntry<VmWorkload>>

    /**
     * Initialize the reader.
     */
    init {
        val entries = mutableMapOf<Long, TraceEntry<VmWorkload>>()

        val jobNumberCol = 0
        val submitTimeCol = 1 // seconds (begin of trace is 0)
        val waitTimeCol = 2 // seconds
        val runTimeCol = 3 // seconds
        val numAllocatedCoresCol = 4 // We assume that single-core processors were used at the time
        val requestedMemoryCol = 9 // KB per processor/core (-1 if not specified)

        val sliceDuration = 5 * 60L

        var jobNumber: Long
        var submitTime: Long
        var waitTime: Long
        var runTime: Long
        var cores: Int
        var memory: Long
        var slicedWaitTime: Long
        var flopsPerSecond: Long
        var flopsPartialSlice: Long
        var flopsFullSlice: Long
        var runtimePartialSliceRemainder: Long

        BufferedReader(FileReader(file)).use { reader ->
            reader.lineSequence()
                .filter { line ->
                    // Ignore comments in the trace
                    !line.startsWith(";") && line.isNotBlank()
                }
                .forEach { line ->
                    val values = line.trim().split("\\s+".toRegex())

                    jobNumber = values[jobNumberCol].trim().toLong()
                    submitTime = values[submitTimeCol].trim().toLong()
                    waitTime = values[waitTimeCol].trim().toLong()
                    runTime = values[runTimeCol].trim().toLong()
                    cores = values[numAllocatedCoresCol].trim().toInt()
                    memory = values[requestedMemoryCol].trim().toLong()

                    if (maxNumCores != -1 && cores > maxNumCores) {
                        println("Skipped a task due to processor count ($cores > $maxNumCores).")
                        return@forEach
                    }

                    if (memory == -1L) {
                        memory = 1000L * cores // assume 1GB of memory per processor if not specified
                    } else {
                        memory /= 1000 // convert KB to MB
                    }

                    val flopsHistory = mutableListOf<SimTraceWorkload.Fragment>()

                    // Insert waiting time slices

                    // We ignore wait time remainders under one
                    slicedWaitTime = 0L
                    if (waitTime >= sliceDuration) {
                        for (tick in submitTime until (submitTime + waitTime - sliceDuration) step sliceDuration) {
                            flopsHistory.add(
                                SimTraceWorkload.Fragment(
                                    sliceDuration * 1000L,
                                    0.0,
                                    cores
                                )
                            )
                            slicedWaitTime += sliceDuration
                        }
                    }

                    // Insert run time slices

                    flopsPerSecond = 4_000L * cores
                    runtimePartialSliceRemainder = runTime % sliceDuration
                    flopsPartialSlice = flopsPerSecond * runtimePartialSliceRemainder
                    flopsFullSlice = flopsPerSecond * runTime - flopsPartialSlice

                    for (
                        tick in (submitTime + slicedWaitTime)
                            until (submitTime + slicedWaitTime + runTime - sliceDuration)
                            step sliceDuration
                    ) {
                        flopsHistory.add(
                            SimTraceWorkload.Fragment(
                                sliceDuration * 1000L,
                                1.0,
                                cores
                            )
                        )
                    }

                    if (runtimePartialSliceRemainder > 0) {
                        flopsHistory.add(
                            SimTraceWorkload.Fragment(
                                sliceDuration,
                                runtimePartialSliceRemainder / sliceDuration.toDouble(),
                                cores
                            )
                        )
                    }

                    val uuid = UUID(0L, jobNumber)
                    val vmWorkload = VmWorkload(
                        uuid,
                        "SWF Workload $jobNumber",
                        UnnamedUser,
                        SimWorkloadImage(
                            uuid,
                            jobNumber.toString(),
                            mapOf(
                                "cores" to cores,
                                "required-memory" to memory
                            ),
                            SimTraceWorkload(flopsHistory.asSequence())
                        )
                    )

                    entries[jobNumber] = TraceEntryImpl(submitTime, vmWorkload)
                }
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
