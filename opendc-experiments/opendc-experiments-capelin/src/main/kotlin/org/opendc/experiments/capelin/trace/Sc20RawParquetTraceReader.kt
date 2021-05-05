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

import mu.KotlinLogging
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.opendc.format.trace.TraceEntry
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param path The directory of the traces.
 */
@OptIn(ExperimentalStdlibApi::class)
public class Sc20RawParquetTraceReader(private val path: File) {
    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(path: File): Map<String, List<SimTraceWorkload.Fragment>> {
        @Suppress("DEPRECATION")
        val reader = AvroParquetReader.builder<GenericData.Record>(Path(path.absolutePath, "trace.parquet"))
            .disableCompatibility()
            .build()

        val fragments = mutableMapOf<String, MutableList<SimTraceWorkload.Fragment>>()

        return try {
            while (true) {
                val record = reader.read() ?: break

                val id = record["id"].toString()
                val duration = record["duration"] as Long
                val cores = record["cores"] as Int
                val cpuUsage = record["cpuUsage"] as Double

                val fragment = SimTraceWorkload.Fragment(
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
    private fun parseMeta(path: File, fragments: Map<String, List<SimTraceWorkload.Fragment>>): List<TraceEntry<SimWorkload>> {
        @Suppress("DEPRECATION")
        val metaReader = AvroParquetReader.builder<GenericData.Record>(Path(path.absolutePath, "meta.parquet"))
            .disableCompatibility()
            .build()

        var counter = 0
        val entries = mutableListOf<TraceEntry<SimWorkload>>()

        return try {
            while (true) {
                val record = metaReader.read() ?: break

                val id = record["id"].toString()
                if (!fragments.containsKey(id)) {
                    continue
                }

                val submissionTime = record["submissionTime"] as Long
                val endTime = record["endTime"] as Long
                val maxCores = record["maxCores"] as Int
                val requiredMemory = record["requiredMemory"] as Long
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())

                val vmFragments = fragments.getValue(id).asSequence()
                val totalLoad = vmFragments.sumOf { it.usage } * 5 * 60 // avg MHz * duration = MFLOPs
                val workload = SimTraceWorkload(vmFragments)
                entries.add(
                    TraceEntry(
                        uid, id, submissionTime, workload,
                        mapOf(
                            "submit-time" to submissionTime,
                            "end-time" to endTime,
                            "total-load" to totalLoad,
                            "cores" to maxCores,
                            "required-memory" to requiredMemory,
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
            metaReader.close()
        }
    }

    /**
     * The entries in the trace.
     */
    private val entries: List<TraceEntry<SimWorkload>>

    init {
        val fragments = parseFragments(path)
        entries = parseMeta(path, fragments)
    }

    /**
     * Read the entries in the trace.
     */
    public fun read(): List<TraceEntry<SimWorkload>> = entries

    /**
     * Create a [TraceReader] instance.
     */
    public fun createReader(): TraceReader<SimWorkload> {
        return object : TraceReader<SimWorkload>, Iterator<TraceEntry<SimWorkload>> by entries.iterator() {
            override fun close() {}
        }
    }
}
