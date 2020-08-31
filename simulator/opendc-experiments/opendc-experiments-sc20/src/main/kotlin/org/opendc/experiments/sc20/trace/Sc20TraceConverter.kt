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

package org.opendc.experiments.sc20.trace

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import me.tongfei.progressbar.ProgressBar
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.opendc.format.trace.sc20.Sc20VmPlacementReader
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Represents the command for converting traces
 */
public class TraceConverterCli : CliktCommand(name = "trace-converter") {
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
public sealed class TraceConversion(name: String) : OptionGroup(name) {
    /**
     * Read the fragments of the trace.
     */
    public abstract fun read(
        traceDirectory: File,
        metaSchema: Schema,
        metaWriter: ParquetWriter<GenericData.Record>
    ): MutableList<Fragment>
}

public class SolvinityConversion : TraceConversion("Solvinity") {
    private val clusters by option()
        .split(",")

    private val vmPlacements by option("--vm-placements", help = "file containing the VM placements")
        .file(canBeDir = false)
        .convert { it.inputStream().buffered().use { Sc20VmPlacementReader(it).construct() } }
        .required()

    override fun read(
        traceDirectory: File,
        metaSchema: Schema,
        metaWriter: ParquetWriter<GenericData.Record>
    ): MutableList<Fragment> {
        val clusters = clusters?.toSet() ?: emptySet()
        val timestampCol = 0
        val cpuUsageCol = 1
        val coreCol = 12
        val provisionedMemoryCol = 20
        val traceInterval = 5 * 60 * 1000L

        // Identify start time of the entire trace
        var minTimestamp = Long.MAX_VALUE
        traceDirectory.walk()
            .filterNot { it.isDirectory }
            .filter { it.extension == "csv" || it.extension == "txt" }
            .toList()
            .forEach { vmFile ->
                BufferedReader(FileReader(vmFile)).use { reader ->
                    reader.lineSequence()
                        .chunked(128)
                        .forEachIndexed { idx, lines ->
                            for (line in lines) {
                                // Ignore comments in the trace
                                if (line.startsWith("#") || line.isBlank()) {
                                    continue
                                }

                                val vmId = vmFile.name

                                // Check if VM in topology
                                val clusterName = vmPlacements[vmId]
                                if (clusterName == null || !clusters.contains(clusterName)) {
                                    continue
                                }

                                val values = line.split("\t")
                                val timestamp = (values[timestampCol].trim().toLong() - 5 * 60) * 1000L

                                if (timestamp < minTimestamp) {
                                    minTimestamp = timestamp
                                }
                                return@forEach
                            }
                        }
                }
            }

        println("Start of trace at $minTimestamp")

        val allFragments = mutableListOf<Fragment>()

        val begin = 15 * 24 * 60 * 60 * 1000L
        val end = 45 * 24 * 60 * 60 * 1000L

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

                                    val values = line.split("\t")

                                    vmId = vmFile.name

                                    // Check if VM in topology
                                    val clusterName = vmPlacements[vmId]
                                    if (clusterName == null || !clusters.contains(clusterName)) {
                                        continue
                                    }

                                    val timestamp =
                                        (values[timestampCol].trim().toLong() - 5 * 60) * 1000L - minTimestamp
                                    if (begin > timestamp || timestamp > end) {
                                        continue
                                    }

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
                flopsFragments.filter { it.tick in begin until end }.forEach { fragment ->
                    allFragments.add(fragment)
                    maxTime = max(maxTime, fragment.tick)
                }

                if (minTime in begin until end) {
                    val metaRecord = GenericData.Record(metaSchema)
                    metaRecord.put("id", vmId)
                    metaRecord.put("submissionTime", minTime)
                    metaRecord.put("endTime", maxTime)
                    metaRecord.put("maxCores", maxCores)
                    metaRecord.put("requiredMemory", requiredMemory)
                    metaWriter.write(metaRecord)
                }
            }

        return allFragments
    }
}

/**
 * Conversion of the Bitbrains public trace.
 */
public class BitbrainsConversion : TraceConversion("Bitbrains") {
    override fun read(
        traceDirectory: File,
        metaSchema: Schema,
        metaWriter: ParquetWriter<GenericData.Record>
    ): MutableList<Fragment> {
        val timestampCol = 0
        val cpuUsageCol = 3
        val coreCol = 1
        val provisionedMemoryCol = 5
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
                            .drop(1)
                            .chunked(128)
                            .forEach { lines ->
                                for (line in lines) {
                                    // Ignore comments in the trace
                                    if (line.startsWith("#") || line.isBlank()) {
                                        continue
                                    }

                                    val values = line.split(";\t")

                                    vmId = vmFile.name

                                    val timestamp = (values[timestampCol].trim().toLong() - 5 * 60) * 1000L

                                    cores = values[coreCol].trim().toInt()
                                    requiredMemory =
                                        max(requiredMemory, values[provisionedMemoryCol].trim().toDouble().toLong())
                                    maxCores = max(maxCores, cores)
                                    minTime = min(minTime, timestamp)
                                    val cpuUsage = values[cpuUsageCol].trim().toDouble() // MHz

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
}

/**
 * Conversion of the Azure public VM trace.
 */
public class AzureConversion : TraceConversion("Azure") {
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

public data class Fragment(
    public val id: String,
    public val tick: Long,
    public val flops: Long,
    public val duration: Long,
    public val usage: Double,
    public val cores: Int
)

public class VmInfo(public val cores: Int, public val requiredMemory: Long, public var minTime: Long, public var maxTime: Long)

/**
 * A script to convert a trace in text format into a Parquet trace.
 */
public fun main(args: Array<String>): Unit = TraceConverterCli().main(args)
