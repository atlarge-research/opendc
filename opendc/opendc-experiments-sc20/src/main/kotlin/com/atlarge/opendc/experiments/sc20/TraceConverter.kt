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

package com.atlarge.opendc.experiments.sc20

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.math.max
import kotlin.math.min

/**
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
fun main() {
    val metaSchema = SchemaBuilder
        .record("meta")
        .namespace("com.atlarge.opendc.format.sc20")
        .fields()
        .name("id").type().stringType().noDefault()
        .name("submissionTime").type().longType().noDefault()
        .name("endTime").type().longType().noDefault()
        .name("maxCores").type().intType().noDefault()
        .name("requiredMemory").type().longType().noDefault()
        .endRecord()
    val schema = SchemaBuilder
        .record("trace")
        .namespace("com.atlarge.opendc.format.sc20")
        .fields()
        .name("id").type().stringType().noDefault()
        .name("time").type().longType().noDefault()
        .name("duration").type().longType().noDefault()
        .name("cores").type().intType().noDefault()
        .name("cpuUsage").type().doubleType().noDefault()
        .name("flops").type().longType().noDefault()
        .endRecord()

    val timestampCol = 0
    val cpuUsageCol = 1
    val coreCol = 12
    val vmIdCol = 19
    val provisionedMemoryCol = 20
    val traceInterval = 5 * 60 * 1000L

    val dest = File("../traces/solvinity/small-parquet")
    val traceDirectory = File("../traces/solvinity/small")
    val vms =
        traceDirectory.walk()
            .filterNot { it.isDirectory }
            .filter { it.extension == "csv" || it.extension == "txt" }
            .toList()

    val metaWriter = AvroParquetWriter.builder<GenericData.Record>(Path(dest.absolutePath, "meta.parquet"))
        .withSchema(metaSchema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withPageSize(4 * 1024 * 1024) // For compression
        .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
        .build()

    val allFragments = mutableListOf<Fragment>()

    vms
        .forEachIndexed { idx, vmFile ->
            println(vmFile)

            var vmId = ""
            var maxCores = -1
            var requiredMemory = -1L
            var cores = -1
            var minTime = Long.MAX_VALUE

            val flopsFragments = sequence {
                var last: Fragment? = null

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

                                vmId = vmFile.name
                                val timestamp = (values[timestampCol].trim().toLong() - 5 * 60) * 1000L
                                cores = values[coreCol].trim().toInt()
                                requiredMemory = max(requiredMemory, values[provisionedMemoryCol].trim().toLong())
                                maxCores = max(maxCores, cores)
                                minTime = min(minTime, timestamp)
                                val cpuUsage = values[cpuUsageCol].trim().toDouble() // MHz
                                requiredMemory = max(requiredMemory, values[provisionedMemoryCol].trim().toLong())
                                maxCores = max(maxCores, cores)

                                val flops: Long = (cpuUsage * 5 * 60).toLong()

                                last = if (last != null && last!!.flops == 0L && flops == 0L) {
                                    val oldFragment = last!!
                                    Fragment(
                                        vmId,
                                        oldFragment.tick,
                                        oldFragment.flops + flops,
                                        oldFragment.duration + traceInterval,
                                        cpuUsage,
                                        cores
                                    )
                                } else {
                                    val fragment =
                                        Fragment(vmId, timestamp, flops, traceInterval, cpuUsage, cores)
                                    if (last != null) {
                                        yield(last!!)
                                    }
                                    fragment
                                }
                            }
                        }
                }

                if (last != null) {
                    yield(last!!)
                }
            }

            var maxTime = Long.MIN_VALUE
            flopsFragments.forEach { fragment ->
                allFragments.add(fragment)
                maxTime = max(maxTime, fragment.tick)
            }

            val metaRecord = GenericData.Record(metaSchema)
            metaRecord.put("id", vmId)
            metaRecord.put("submissionTime", minTime)
            metaRecord.put("endTime", maxTime)
            metaRecord.put("maxCores", maxCores)
            metaRecord.put("requiredMemory", requiredMemory)
            metaWriter.write(metaRecord)
        }

    val writer = AvroParquetWriter.builder<GenericData.Record>(Path(dest.absolutePath, "trace.parquet"))
        .withSchema(schema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withPageSize(4 * 1024 * 1024) // For compression
        .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
        .build()

    allFragments.sortWith(compareBy<Fragment> { it.tick }.thenBy { it.id })

    for (fragment in allFragments) {
        val record = GenericData.Record(schema)
        record.put("id", fragment.id)
        record.put("time", fragment.tick)
        record.put("duration", fragment.duration)
        record.put("cores", fragment.cores)
        record.put("cpuUsage", fragment.usage)
        record.put("flops", fragment.flops)

        writer.write(record)
    }

    writer.close()
    metaWriter.close()
}

data class Fragment(val id: String, val tick: Long, val flops: Long, val duration: Long, val usage: Double, val cores: Int)
