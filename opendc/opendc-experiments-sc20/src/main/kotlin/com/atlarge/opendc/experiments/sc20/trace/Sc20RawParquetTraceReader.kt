/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.trace

import com.atlarge.opendc.compute.core.image.FlopsHistoryFragment
import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.core.User
import com.atlarge.opendc.format.trace.TraceEntry
import com.atlarge.opendc.format.trace.TraceReader
import mu.KotlinLogging
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param path The directory of the traces.
 */
@OptIn(ExperimentalStdlibApi::class)
class Sc20RawParquetTraceReader(private val path: File) {
    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(path: File): Map<String, List<FlopsHistoryFragment>> {
        val reader = AvroParquetReader.builder<GenericData.Record>(Path(path.absolutePath, "trace.parquet"))
            .disableCompatibility()
            .build()

        val fragments = mutableMapOf<String, MutableList<FlopsHistoryFragment>>()

        return try {
            while (true) {
                val record = reader.read() ?: break

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
    private fun parseMeta(path: File, fragments: Map<String, List<FlopsHistoryFragment>>): List<TraceEntryImpl> {
        val metaReader = AvroParquetReader.builder<GenericData.Record>(Path(path.absolutePath, "meta.parquet"))
            .disableCompatibility()
            .build()

        var counter = 0
        val entries = mutableListOf<TraceEntryImpl>()

        return try {
            while (true) {
                val record = metaReader.read() ?: break
                val id = record["id"].toString()
                val submissionTime = record["submissionTime"] as Long
                val endTime = record["endTime"] as Long
                val maxCores = record["maxCores"] as Int
                val requiredMemory = record["requiredMemory"] as Long
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())

                logger.info { "VM $id" }

                val vmFragments = fragments.getValue(id).asSequence()
                val totalLoad = vmFragments.sumByDouble { it.usage } * 5 * 60 // avg MHz * duration = MFLOPs
                val vmWorkload = VmWorkload(
                    uid, id,
                    UnnamedUser,
                    VmImage(
                        uid,
                        id,
                        mapOf(
                            "submit-time" to submissionTime,
                            "end-time" to endTime,
                            "total-load" to totalLoad
                        ),
                        vmFragments,
                        maxCores,
                        requiredMemory
                    )
                )
                entries.add(TraceEntryImpl(submissionTime, vmWorkload))
            }

            entries
        } finally {
            metaReader.close()
        }
    }

    /**
     * The entries in the trace.
     */
    private val entries: List<TraceEntryImpl>

    init {
        val fragments = parseFragments(path)
        entries = parseMeta(path, fragments)
    }

    /**
     * Read the entries in the trace.
     */
    public fun read(): List<TraceEntry<VmWorkload>> = entries

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
    internal data class TraceEntryImpl(
        override var submissionTime: Long,
        override val workload: VmWorkload
    ) : TraceEntry<VmWorkload>

    /**
     * A load cache entry.
     */
    data class LoadCacheEntry(val vm: String, val totalLoad: Double, val start: Long, val end: Long)
}
