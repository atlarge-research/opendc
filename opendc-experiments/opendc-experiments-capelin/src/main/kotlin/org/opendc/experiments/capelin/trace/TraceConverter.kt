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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import me.tongfei.progressbar.ProgressBar
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.opendc.experiments.capelin.trace.sv.SvTraceFormat
import org.opendc.trace.*
import org.opendc.trace.bitbrains.BitbrainsTraceFormat
import org.opendc.trace.spi.TraceFormat
import org.opendc.trace.util.parquet.LocalOutputFile
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Represents the command for converting traces
 */
class TraceConverterCli : CliktCommand(name = "trace-converter") {
    /**
     * The directory where the trace should be stored.
     */
    private val outputPath by option("-O", "--output", help = "path to store the trace")
        .file(canBeFile = false, mustExist = false)
        .defaultLazy { File("output") }

    /**
     * The directory where the input trace is located.
     */
    private val inputPath by argument("input", help = "path to the input trace")
        .file(canBeFile = false)

    /**
     * The input type of the trace.
     */
    private val type by option("-t", "--type", help = "input type of trace").groupChoice(
        "solvinity" to SolvinityConversion(),
        "bitbrains" to BitbrainsConversion(),
        "azure" to AzureConversion()
    )

    override fun run() {
        val metaSchema = SchemaBuilder
            .record("meta")
            .namespace("org.opendc.format.sc20")
            .fields()
            .name("id").type().stringType().noDefault()
            .name("submissionTime").type().longType().noDefault()
            .name("endTime").type().longType().noDefault()
            .name("maxCores").type().intType().noDefault()
            .name("requiredMemory").type().longType().noDefault()
            .endRecord()
        val schema = SchemaBuilder
            .record("trace")
            .namespace("org.opendc.format.sc20")
            .fields()
            .name("id").type().stringType().noDefault()
            .name("time").type().longType().noDefault()
            .name("duration").type().longType().noDefault()
            .name("cores").type().intType().noDefault()
            .name("cpuUsage").type().doubleType().noDefault()
            .name("flops").type().longType().noDefault()
            .endRecord()

        val metaParquet = File(outputPath, "meta.parquet")
        val traceParquet = File(outputPath, "trace.parquet")

        if (metaParquet.exists()) {
            metaParquet.delete()
        }
        if (traceParquet.exists()) {
            traceParquet.delete()
        }

        val metaWriter = AvroParquetWriter.builder<GenericData.Record>(LocalOutputFile(metaParquet))
            .withSchema(metaSchema)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withPageSize(4 * 1024 * 1024) // For compression
            .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
            .build()

        val writer = AvroParquetWriter.builder<GenericData.Record>(LocalOutputFile(traceParquet))
            .withSchema(schema)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withPageSize(4 * 1024 * 1024) // For compression
            .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
            .build()

        try {
            val type = type ?: throw IllegalArgumentException("Invalid trace conversion")
            val allFragments = type.read(inputPath, metaSchema, metaWriter)
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
        } finally {
            writer.close()
            metaWriter.close()
        }
    }
}

/**
 * The supported trace conversions.
 */
sealed class TraceConversion(name: String) : OptionGroup(name) {
    /**
     * Read the fragments of the trace.
     */
    abstract fun read(
        traceDirectory: File,
        metaSchema: Schema,
        metaWriter: ParquetWriter<GenericData.Record>
    ): MutableList<Fragment>
}

/**
 * A [TraceConversion] that uses the Trace API to perform the conversion.
 */
abstract class AbstractConversion(name: String) : TraceConversion(name) {
    abstract val format: TraceFormat

    override fun read(
        traceDirectory: File,
        metaSchema: Schema,
        metaWriter: ParquetWriter<GenericData.Record>
    ): MutableList<Fragment> {
        val fragments = mutableListOf<Fragment>()
        val trace = format.open(traceDirectory.toURI().toURL())
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        var lastId: String? = null
        var maxCores = Int.MIN_VALUE
        var requiredMemory = Long.MIN_VALUE
        var minTime = Long.MAX_VALUE
        var maxTime = Long.MIN_VALUE
        var lastTimestamp = Long.MIN_VALUE

        while (reader.nextRow()) {
            val id = reader.get(RESOURCE_STATE_ID)

            if (lastId != null && lastId != id) {
                val metaRecord = GenericData.Record(metaSchema)
                metaRecord.put("id", lastId)
                metaRecord.put("submissionTime", minTime)
                metaRecord.put("endTime", maxTime)
                metaRecord.put("maxCores", maxCores)
                metaRecord.put("requiredMemory", requiredMemory)
                metaWriter.write(metaRecord)
            }
            lastId = id

            val timestamp = reader.get(RESOURCE_STATE_TIMESTAMP)
            val timestampMs = timestamp.toEpochMilli()
            val cpuUsage = reader.getDouble(RESOURCE_STATE_CPU_USAGE)
            val cores = reader.getInt(RESOURCE_STATE_NCPUS)
            val memCapacity = reader.getDouble(RESOURCE_STATE_MEM_CAPACITY)

            maxCores = max(maxCores, cores)
            requiredMemory = max(requiredMemory, (memCapacity / 1000).toLong())

            if (lastTimestamp < 0) {
                lastTimestamp = timestampMs - 5 * 60 * 1000L
                minTime = min(minTime, lastTimestamp)
            }

            if (fragments.isEmpty()) {
                val duration = 5 * 60 * 1000L
                val flops: Long = (cpuUsage * duration / 1000).toLong()
                fragments.add(Fragment(id, lastTimestamp, flops, duration, cpuUsage, cores))
            } else {
                val last = fragments.last()
                val duration = timestampMs - lastTimestamp
                val flops: Long = (cpuUsage * duration / 1000).toLong()

                // Perform run-length encoding
                if (last.id == id && (duration == 0L || last.usage == cpuUsage)) {
                    fragments[fragments.size - 1] = last.copy(duration = last.duration + duration)
                } else {
                    fragments.add(
                        Fragment(
                            id,
                            lastTimestamp,
                            flops,
                            duration,
                            cpuUsage,
                            cores
                        )
                    )
                }
            }

            val last = fragments.last()
            maxTime = max(maxTime, last.tick + last.duration)
            lastTimestamp = timestampMs
        }
        return fragments
    }
}

class SolvinityConversion : AbstractConversion("Solvinity") {
    override val format: TraceFormat = SvTraceFormat()
}

/**
 * Conversion of the Bitbrains public trace.
 */
class BitbrainsConversion : AbstractConversion("Bitbrains") {
    override val format: TraceFormat = BitbrainsTraceFormat()
}

/**
 * Conversion of the Azure public VM trace.
 */
class AzureConversion : TraceConversion("Azure") {
    private val seed by option(help = "seed for trace sampling")
        .long()
        .default(0)

    override fun read(
        traceDirectory: File,
        metaSchema: Schema,
        metaWriter: ParquetWriter<GenericData.Record>
    ): MutableList<Fragment> {
        val random = Random(seed)
        val fraction = 0.01

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

        for (i in ProgressBar.wrap((1..195).toList(), "Reading Trace")) {
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
                            cpuUsage = values[cpuUsageCol].trim().toDouble() * 3_000 // MHz
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
}

data class Fragment(
    val id: String,
    val tick: Long,
    val flops: Long,
    val duration: Long,
    val usage: Double,
    val cores: Int
)

class VmInfo(val cores: Int, val requiredMemory: Long, var minTime: Long, var maxTime: Long)

/**
 * A script to convert a trace in text format into a Parquet trace.
 */
fun main(args: Array<String>): Unit = TraceConverterCli().main(args)
