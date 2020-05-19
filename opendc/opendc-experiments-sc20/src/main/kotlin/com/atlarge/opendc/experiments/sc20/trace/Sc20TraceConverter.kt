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

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * A script to convert a trace in text format into a Parquet trace.
 */
fun main(args: Array<String>) {
    if (args.size < 3) {
        println("error: expected <OUTPUT> <INPUT> <TRACE-TYPE> [<SEED>]")
        return
    }

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

    val dest = File(args[0])
    val traceDirectory = File(args[1])
    val metaParquet = File(dest.absolutePath, "meta.parquet")
    val traceParquet = File(dest.absolutePath, "trace.parquet")

    if (metaParquet.exists()) {
        metaParquet.delete()
    }
    if (traceParquet.exists()) {
        traceParquet.delete()
    }

    val metaWriter = AvroParquetWriter.builder<GenericData.Record>(Path(metaParquet.toURI()))
        .withSchema(metaSchema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withPageSize(4 * 1024 * 1024) // For compression
        .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
        .build()

    val writer = AvroParquetWriter.builder<GenericData.Record>(Path(traceParquet.toURI()))
        .withSchema(schema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withPageSize(4 * 1024 * 1024) // For compression
        .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
        .build()

    val traceType = args[2]
    val startTime = System.currentTimeMillis()
    val allFragments = if (traceType == "solvinity") {
        readSolvinityTrace(traceDirectory, metaSchema, metaWriter)
    } else {
        val seed = args[3].toLong()
        readAzureTrace(traceDirectory, metaSchema, metaWriter, seed)
    }
    allFragments.sortWith(compareBy<Fragment> { it.tick }.thenBy { it.id })
    println("Reading trace took ${(System.currentTimeMillis() - startTime) / 1000} seconds")

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

data class Fragment(
    val id: String,
    val tick: Long,
    val flops: Long,
    val duration: Long,
    val usage: Double,
    val cores: Int
)

/**
 * Reads the confidential Solvinity trace.
 */
fun readSolvinityTrace(
    traceDirectory: File,
    metaSchema: Schema,
    metaWriter: ParquetWriter<GenericData.Record>
): MutableList<Fragment> {
    val timestampCol = 0
    val cpuUsageCol = 1
    val coreCol = 12
    val vmIdCol = 19
    val provisionedMemoryCol = 20
    val traceInterval = 5 * 60 * 1000L

    val allFragments = mutableListOf<Fragment>()

    traceDirectory.walk()
        .filterNot { it.isDirectory }
        .filter { it.extension == "csv" || it.extension == "txt" }
        .toList()
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
                                        Fragment(
                                            vmId,
                                            timestamp,
                                            flops,
                                            traceInterval,
                                            cpuUsage,
                                            cores
                                        )
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

    return allFragments
}

/**
 * Reads the Azure cloud trace.
 *
 * See https://github.com/Azure/AzurePublicDataset/ for a definition of the trace.
 */
fun readAzureTrace(
    traceDirectory: File,
    metaSchema: Schema,
    metaWriter: ParquetWriter<GenericData.Record>,
    seed: Long
): MutableList<Fragment> {
    val random = Random(seed)
    val fraction = 0.005

    // Read VM table
    val vmIdTableCol = 0
    val coreTableCol = 9
    val provisionedMemoryTableCol = 10

    var vmId: String
    var cores: Int
    var requiredMemory: Long

    val vmIds = mutableSetOf<String>()
    val vmIdToMetadata = mutableMapOf<String, VmInfo>()

    BufferedReader(FileReader(File(traceDirectory, "vmtable.csv"))).use { reader ->
        reader.lineSequence()
            .chunked(1024)
            .forEach { lines ->
                for (line in lines) {
                    // Ignore comments in the trace
                    if (line.startsWith("#") || line.isBlank()) {
                        continue
                    }
                    // Sample only a fraction of the VMs
                    if (random.nextDouble() > fraction) {
                        continue
                    }

                    val values = line.split(",")

                    // Exclude VMs with a large number of cores (not specified exactly)
                    if (values[coreTableCol].contains(">")) {
                        continue
                    }

                    vmId = values[vmIdTableCol].trim()
                    cores = values[coreTableCol].trim().toInt()
                    requiredMemory = values[provisionedMemoryTableCol].trim().toInt() * 1_000L // GB -> MB

                    vmIds.add(vmId)
                    vmIdToMetadata[vmId] = VmInfo(cores, requiredMemory, Long.MAX_VALUE, -1L)
                }
            }
    }

    // Read VM metric reading files
    val timestampCol = 0
    val vmIdCol = 1
    val cpuUsageCol = 4
    val traceInterval = 5 * 60 * 1000L

    val vmIdToFragments = mutableMapOf<String, MutableList<Fragment>>()
    val vmIdToLastFragment = mutableMapOf<String, Fragment?>()
    val allFragments = mutableListOf<Fragment>()

    for (i in 1..195) {
        val readingsFile = File(File(traceDirectory, "readings"), "readings-$i.csv")
        var timestamp: Long
        var cpuUsage: Double

        BufferedReader(FileReader(readingsFile)).use { reader ->
            reader.lineSequence()
                .chunked(128)
                .forEach { lines ->
                    for (line in lines) {
                        // Ignore comments in the trace
                        if (line.startsWith("#") || line.isBlank()) {
                            continue
                        }

                        val values = line.split(",")
                        vmId = values[vmIdCol].trim()

                        // Ignore readings for VMs not in the sample
                        if (!vmIds.contains(vmId)) {
                            continue
                        }

                        timestamp = values[timestampCol].trim().toLong() * 1000L
                        vmIdToMetadata[vmId]!!.minTime = min(vmIdToMetadata[vmId]!!.minTime, timestamp)
                        cpuUsage = values[cpuUsageCol].trim().toDouble() * 4_000 // MHz
                        vmIdToMetadata[vmId]!!.maxTime = max(vmIdToMetadata[vmId]!!.maxTime, timestamp)

                        val flops: Long = (cpuUsage * 5 * 60).toLong()
                        val lastFragment = vmIdToLastFragment[vmId]

                        vmIdToLastFragment[vmId] =
                            if (lastFragment != null && lastFragment.flops == 0L && flops == 0L) {
                                Fragment(
                                    vmId,
                                    lastFragment.tick,
                                    lastFragment.flops + flops,
                                    lastFragment.duration + traceInterval,
                                    cpuUsage,
                                    vmIdToMetadata[vmId]!!.cores
                                )
                            } else {
                                val fragment =
                                    Fragment(
                                        vmId,
                                        timestamp,
                                        flops,
                                        traceInterval,
                                        cpuUsage,
                                        vmIdToMetadata[vmId]!!.cores
                                    )
                                if (lastFragment != null) {
                                    if (vmIdToFragments[vmId] == null) {
                                        vmIdToFragments[vmId] = mutableListOf()
                                    }
                                    vmIdToFragments[vmId]!!.add(lastFragment)
                                    allFragments.add(lastFragment)
                                }
                                fragment
                            }
                    }
                }
        }
    }

    for (entry in vmIdToLastFragment) {
        if (entry.value != null) {
            if (vmIdToFragments[entry.key] == null) {
                vmIdToFragments[entry.key] = mutableListOf()
            }
            vmIdToFragments[entry.key]!!.add(entry.value!!)
        }
    }

    println("Read ${vmIdToLastFragment.size} VMs")

    for (entry in vmIdToMetadata) {
        val metaRecord = GenericData.Record(metaSchema)
        metaRecord.put("id", entry.key)
        metaRecord.put("submissionTime", entry.value.minTime)
        metaRecord.put("endTime", entry.value.maxTime)
        println("${entry.value.minTime} - ${entry.value.maxTime}")
        metaRecord.put("maxCores", entry.value.cores)
        metaRecord.put("requiredMemory", entry.value.requiredMemory)
        metaWriter.write(metaRecord)
    }

    return allFragments
}

class VmInfo(val cores: Int, val requiredMemory: Long, var minTime: Long, var maxTime: Long)
