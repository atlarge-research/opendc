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

package com.atlarge.opendc.experiments.sc20

import com.atlarge.opendc.compute.core.image.FlopsHistoryFragment
import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.compute.core.workload.IMAGE_PERF_INTERFERENCE_MODEL
import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.core.User
import com.atlarge.opendc.format.trace.TraceEntry
import com.atlarge.opendc.format.trace.TraceReader
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.filter2.predicate.FilterApi
import org.apache.parquet.filter2.predicate.Statistics
import org.apache.parquet.filter2.predicate.UserDefinedPredicate
import org.apache.parquet.io.api.Binary
import java.io.File
import java.io.Serializable
import java.util.Deque
import java.util.SortedSet
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param traceFile The directory of the traces.
 * @param performanceInterferenceModel The performance model covering the workload in the VM trace.
 */
@OptIn(ExperimentalStdlibApi::class)
class Sc20ParquetTraceReader(
    traceFile: File,
    performanceInterferenceModel: PerformanceInterferenceModel,
    selectedVms: List<String>,
    random: Random
) : TraceReader<VmWorkload> {
    /**
     * The internal iterator to use for this reader.
     */
    private val iterator: Iterator<TraceEntry<VmWorkload>>

    /**
     * The intermediate buffer to store the read records in.
     */
    private val queue = ArrayBlockingQueue<Pair<String, FlopsHistoryFragment>>(128)

    /**
     * An optional filter for filtering the selected VMs
     */
    private val filter =
        if (selectedVms.isEmpty())
            null
        else
            FilterCompat.get(FilterApi.userDefined(FilterApi.binaryColumn("id"), SelectedVmFilter(TreeSet(selectedVms))))

    /**
     * A poisonous fragment.
     */
    private val poison = Pair("\u0000", FlopsHistoryFragment(0, 0, 0, 0.0, 0))

    /**
     * The thread to read the records in.
     */
    private val readerThread = thread(start = true, name = "sc20-reader") {
        val reader = AvroParquetReader.builder<GenericData.Record>(Path(traceFile.absolutePath, "trace.parquet"))
            .disableCompatibility()
            .run { if (filter != null) withFilter(filter) else this }
            .build()

        try {
            while (true) {
                val record = reader.read()

                if (record == null) {
                    queue.put(poison)
                    break
                }

                val id = record["id"].toString()
                val tick = record["time"] as Long
                val duration = record["duration"] as Long
                val cores = record["cores"] as Int
                val cpuUsage = record["cpuUsage"] as Double
                val flops = record["flops"] as Long

                val fragment = FlopsHistoryFragment(
                    tick,
                    flops,
                    duration,
                    cpuUsage,
                    cores
                )

                queue.put(id to fragment)
            }
        } catch (e: InterruptedException) {
            // Do not rethrow this
        } finally {
            reader.close()
        }
    }

    /**
     * Fill the buffers with the VMs
     */
    private fun pull(buffers: Map<String, Deque<FlopsHistoryFragment>>) {
        if (!hasNext) {
            return
        }

        repeat(16) {
            val (id, fragment) = queue.take()

            if (id == poison.first) {
                hasNext = false
                return
            }

            buffers[id]?.add(fragment)
        }
    }

    /**
     * A flag to indicate whether the reader has more entries.
     */
    private var hasNext: Boolean = true

    /**
     * Initialize the reader.
     */
    init {
        val entries = mutableMapOf<UUID, TraceEntry<VmWorkload>>()
        val buffers = mutableMapOf<String, Deque<FlopsHistoryFragment>>()

        val metaReader = AvroParquetReader.builder<GenericData.Record>(Path(traceFile.absolutePath, "meta.parquet"))
            .disableCompatibility()
            .run { if (filter != null) withFilter(filter) else this }
            .build()

        var idx = 0
        while (true) {
            val record = metaReader.read() ?: break
            val id = record["id"].toString()
            val submissionTime = record["submissionTime"] as Long
            val endTime = record["endTime"] as Long
            val maxCores = record["maxCores"] as Int
            val requiredMemory = record["requiredMemory"] as Long
            val uuid = UUID(0, (idx++).toLong())
            println(id)

            val buffer = LinkedBlockingDeque<FlopsHistoryFragment>()
            buffers[id] = buffer
            val fragments = sequence<FlopsHistoryFragment> {
                while (true) {
                    if (buffer.isEmpty()) {
                        if (hasNext) {
                            pull(buffers)
                            continue
                        } else {
                            break
                        }
                    }

                    val fragment = buffer.poll()
                    yield(fragment)

                    if (fragment.tick >= endTime) {
                        break
                    }
                }

                buffers.remove(id)
            }
            val relevantPerformanceInterferenceModelItems =
                PerformanceInterferenceModel(
                    performanceInterferenceModel.items.filter { it.workloadNames.contains(id) }.toSet(),
                    Random(random.nextInt())
                )
            val vmWorkload = VmWorkload(
                uuid, "VM Workload $id", UnnamedUser,
                VmImage(
                    uuid,
                    id,
                    mapOf(IMAGE_PERF_INTERFERENCE_MODEL to relevantPerformanceInterferenceModelItems),
                    fragments,
                    maxCores,
                    requiredMemory
                )
            )

            entries[uuid] = TraceEntryImpl(
                submissionTime,
                vmWorkload
            )
        }

        metaReader.close()

        // Create the entry iterator
        iterator = entries.values.sortedBy { it.submissionTime }.iterator()
    }

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): TraceEntry<VmWorkload> = iterator.next()

    override fun close() {
        readerThread.interrupt()
    }

    private class SelectedVmFilter(val selectedVms: SortedSet<String>) : UserDefinedPredicate<Binary>(), Serializable {
        override fun keep(value: Binary?): Boolean = value != null && selectedVms.contains(value.toStringUsingUTF8())

        override fun canDrop(statistics: Statistics<Binary>): Boolean {
            val min = statistics.min
            val max = statistics.max

            return selectedVms.subSet(min.toStringUsingUTF8(), max.toStringUsingUTF8() + "\u0000").isEmpty()
        }

        override fun inverseCanDrop(statistics: Statistics<Binary>): Boolean {
            val min = statistics.min
            val max = statistics.max

            return selectedVms.subSet(min.toStringUsingUTF8(), max.toStringUsingUTF8() + "\u0000").isNotEmpty()
        }
    }

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
