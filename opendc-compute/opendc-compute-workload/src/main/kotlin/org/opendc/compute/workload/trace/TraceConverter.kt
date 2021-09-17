/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.workload.vm.trace

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import mu.KotlinLogging
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.opendc.compute.workload.trace.azure.AzureTraceFormat
import org.opendc.compute.workload.trace.bp.BP_RESOURCES_SCHEMA
import org.opendc.compute.workload.trace.bp.BP_RESOURCE_STATES_SCHEMA
import org.opendc.compute.workload.trace.sv.SvTraceFormat
import org.opendc.trace.*
import org.opendc.trace.bitbrains.BitbrainsTraceFormat
import org.opendc.trace.util.parquet.LocalOutputFile
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * A script to convert a trace in text format into a Parquet trace.
 */
public fun main(args: Array<String>): Unit = TraceConverterCli().main(args)

/**
 * Represents the command for converting traces
 */
internal class TraceConverterCli : CliktCommand(name = "trace-converter") {
    /**
     * The logger instance for the converter.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The directory where the trace should be stored.
     */
    private val output by option("-O", "--output", help = "path to store the trace")
        .file(canBeFile = false, mustExist = false)
        .defaultLazy { File("output") }

    /**
     * The directory where the input trace is located.
     */
    private val input by argument("input", help = "path to the input trace")
        .file(canBeFile = false)

    /**
     * The input format of the trace.
     */
    private val format by option("-f", "--format", help = "input format of trace")
        .choice(
            "solvinity" to SvTraceFormat(),
            "bitbrains" to BitbrainsTraceFormat(),
            "azure" to AzureTraceFormat()
        )
        .required()

    /**
     * The sampling options.
     */
    private val samplingOptions by SamplingOptions().cooccurring()

    override fun run() {
        val metaParquet = File(output, "meta.parquet")
        val traceParquet = File(output, "trace.parquet")

        if (metaParquet.exists()) {
            metaParquet.delete()
        }
        if (traceParquet.exists()) {
            traceParquet.delete()
        }

        val trace = format.open(input.toURI().toURL())

        logger.info { "Building resources table" }

        val metaWriter = AvroParquetWriter.builder<GenericData.Record>(LocalOutputFile(metaParquet))
            .withSchema(BP_RESOURCES_SCHEMA)
            .withCompressionCodec(CompressionCodecName.ZSTD)
            .enablePageWriteChecksum()
            .build()

        val selectedVms = metaWriter.use { convertResources(trace, it) }

        logger.info { "Wrote ${selectedVms.size} rows" }
        logger.info { "Building resource states table" }

        val writer = AvroParquetWriter.builder<GenericData.Record>(LocalOutputFile(traceParquet))
            .withSchema(BP_RESOURCE_STATES_SCHEMA)
            .withCompressionCodec(CompressionCodecName.ZSTD)
            .enableDictionaryEncoding()
            .enablePageWriteChecksum()
            .withBloomFilterEnabled("id", true)
            .withBloomFilterNDV("id", selectedVms.size.toLong())
            .build()

        val statesCount = writer.use { convertResourceStates(trace, it, selectedVms) }
        logger.info { "Wrote $statesCount rows" }
    }

    /**
     * Convert the resources table for the trace.
     */
    private fun convertResources(trace: Trace, writer: ParquetWriter<GenericData.Record>): Set<String> {
        val random = samplingOptions?.let { Random(it.seed) }
        val samplingFraction = samplingOptions?.fraction ?: 1.0
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        var hasNextRow = reader.nextRow()
        val selectedVms = mutableSetOf<String>()

        while (hasNextRow) {
            var id: String
            var numCpus = Int.MIN_VALUE
            var memCapacity = Double.MIN_VALUE
            var memUsage = Double.MIN_VALUE
            var startTime = Long.MAX_VALUE
            var stopTime = Long.MIN_VALUE

            do {
                id = reader.get(RESOURCE_STATE_ID)

                val timestamp = reader.get(RESOURCE_STATE_TIMESTAMP).toEpochMilli()
                startTime = min(startTime, timestamp)
                stopTime = max(stopTime, timestamp)

                numCpus = max(numCpus, reader.getInt(RESOURCE_STATE_NCPUS))

                memCapacity = max(memCapacity, reader.getDouble(RESOURCE_STATE_MEM_CAPACITY))
                if (reader.hasColumn(RESOURCE_STATE_MEM_USAGE)) {
                    memUsage = max(memUsage, reader.getDouble(RESOURCE_STATE_MEM_USAGE))
                }

                hasNextRow = reader.nextRow()
            } while (hasNextRow && id == reader.get(RESOURCE_STATE_ID))

            // Sample only a fraction of the VMs
            if (random != null && random.nextDouble() > samplingFraction) {
                continue
            }

            val builder = GenericRecordBuilder(BP_RESOURCES_SCHEMA)

            builder["id"] = id
            builder["submissionTime"] = startTime
            builder["endTime"] = stopTime
            builder["maxCores"] = numCpus
            builder["requiredMemory"] = max(memCapacity, memUsage).roundToLong()

            logger.info { "Selecting VM $id" }

            writer.write(builder.build())
            selectedVms.add(id)
        }

        return selectedVms
    }

    /**
     * Convert the resource states table for the trace.
     */
    private fun convertResourceStates(trace: Trace, writer: ParquetWriter<GenericData.Record>, selectedVms: Set<String>): Int {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        var hasNextRow = reader.nextRow()
        var count = 0

        while (hasNextRow) {
            var lastTimestamp = Long.MIN_VALUE

            do {
                val id = reader.get(RESOURCE_STATE_ID)

                if (id !in selectedVms) {
                    hasNextRow = reader.nextRow()
                    continue
                }

                val builder = GenericRecordBuilder(BP_RESOURCE_STATES_SCHEMA)
                builder["id"] = id

                val timestamp = reader.get(RESOURCE_STATE_TIMESTAMP).toEpochMilli()
                if (lastTimestamp < 0) {
                    lastTimestamp = timestamp - 5 * 60 * 1000L
                }

                val duration = timestamp - lastTimestamp
                val cores = reader.getInt(RESOURCE_STATE_NCPUS)
                val cpuUsage = reader.getDouble(RESOURCE_STATE_CPU_USAGE)
                val flops = (cpuUsage * duration / 1000.0).roundToLong()

                builder["time"] = timestamp
                builder["duration"] = duration
                builder["cores"] = cores
                builder["cpuUsage"] = cpuUsage
                builder["flops"] = flops

                writer.write(builder.build())

                lastTimestamp = timestamp
                hasNextRow = reader.nextRow()
            } while (hasNextRow && id == reader.get(RESOURCE_STATE_ID))

            count++
        }

        return count
    }

    /**
     * Options for sampling the workload trace.
     */
    private class SamplingOptions : OptionGroup() {
        /**
         * The fraction of VMs to sample
         */
        val fraction by option("--sampling-fraction", help = "fraction of the workload to sample")
            .double()
            .restrictTo(0.0001, 1.0)
            .required()

        /**
         * The seed for sampling the trace.
         */
        val seed by option("--sampling-seed", help = "seed for sampling the workload")
            .long()
            .default(0)
    }
}
