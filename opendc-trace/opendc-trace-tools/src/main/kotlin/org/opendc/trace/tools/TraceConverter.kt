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

@file:JvmName("TraceConverter")
package org.opendc.trace.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import mu.KotlinLogging
import org.opendc.trace.*
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A script to convert a trace in text format into a Parquet trace.
 */
fun main(args: Array<String>): Unit = TraceConverterCli().main(args)

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
    private val inputFormat by option("-f", "--input-format", help = "format of output trace")
        .required()

    /**
     * The format of the output trace.
     */
    private val outputFormat by option("--output-format", help = "format of output trace")
        .default("opendc-vm")

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

        val inputTrace = Trace.open(input, format = inputFormat)
        val outputTrace = Trace.create(output, format = outputFormat)

        logger.info { "Building resources table" }

        val metaWriter = outputTrace.getTable(TABLE_RESOURCES)!!.newWriter()

        val selectedVms = metaWriter.use { convertResources(inputTrace, it) }

        if (selectedVms.isEmpty()) {
            logger.warn { "No VMs selected" }
            return
        }

        logger.info { "Wrote ${selectedVms.size} rows" }
        logger.info { "Building resource states table" }

        val writer = outputTrace.getTable(TABLE_RESOURCE_STATES)!!.newWriter()

        val statesCount = writer.use { convertResourceStates(inputTrace, it, selectedVms) }
        logger.info { "Wrote $statesCount rows" }
    }

    /**
     * Convert the resources table for the trace.
     */
    private fun convertResources(trace: Trace, writer: TableWriter): Set<String> {
        val random = samplingOptions?.let { Random(it.seed) }
        val samplingFraction = samplingOptions?.fraction ?: 1.0
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        var hasNextRow = reader.nextRow()
        val selectedVms = mutableSetOf<String>()

        while (hasNextRow) {
            var id: String
            var cpuCount = 0
            var cpuCapacity = 0.0
            var memCapacity = 0.0
            var memUsage = 0.0
            var startTime = Long.MAX_VALUE
            var stopTime = Long.MIN_VALUE

            do {
                id = reader.get(RESOURCE_ID)

                val timestamp = reader.get(RESOURCE_STATE_TIMESTAMP).toEpochMilli()

                startTime = min(startTime, timestamp)
                stopTime = max(stopTime, timestamp)
                cpuCount = max(cpuCount, reader.getInt(RESOURCE_CPU_COUNT))
                cpuCapacity = max(cpuCapacity, reader.getDouble(RESOURCE_CPU_CAPACITY))
                memCapacity = max(memCapacity, reader.getDouble(RESOURCE_MEM_CAPACITY))

                if (reader.hasColumn(RESOURCE_STATE_MEM_USAGE)) {
                    memUsage = max(memUsage, reader.getDouble(RESOURCE_STATE_MEM_USAGE))
                }

                hasNextRow = reader.nextRow()
            } while (hasNextRow && id == reader.get(RESOURCE_ID))

            // Sample only a fraction of the VMs
            if (random != null && random.nextDouble() > samplingFraction) {
                continue
            }

            logger.info { "Selecting VM $id" }
            selectedVms.add(id)

            writer.startRow()
            writer.set(RESOURCE_ID, id)
            writer.set(RESOURCE_START_TIME, Instant.ofEpochMilli(startTime))
            writer.set(RESOURCE_STOP_TIME, Instant.ofEpochMilli(stopTime))
            writer.setInt(RESOURCE_CPU_COUNT, cpuCount)
            writer.setDouble(RESOURCE_CPU_CAPACITY, cpuCapacity)
            writer.setDouble(RESOURCE_MEM_CAPACITY, max(memCapacity, memUsage))
            writer.endRow()
        }

        return selectedVms
    }

    /**
     * Convert the resource states table for the trace.
     */
    private fun convertResourceStates(trace: Trace, writer: TableWriter, selectedVms: Set<String>): Int {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        var hasNextRow = reader.nextRow()
        var count = 0
        var lastId: String? = null
        var lastTimestamp = 0L

        while (hasNextRow) {
            val id = reader.get(RESOURCE_ID)

            if (id !in selectedVms) {
                hasNextRow = reader.nextRow()
                continue
            }

            val cpuCount = reader.getInt(RESOURCE_CPU_COUNT)
            val cpuUsage = reader.getDouble(RESOURCE_STATE_CPU_USAGE)

            val startTimestamp = reader.get(RESOURCE_STATE_TIMESTAMP).toEpochMilli()
            var timestamp = startTimestamp
            var duration: Long

            // Check whether the previous entry is from a different VM
            if (id != lastId) {
                lastTimestamp = timestamp - 5 * 60 * 1000L
            }

            do {
                timestamp = reader.get(RESOURCE_STATE_TIMESTAMP).toEpochMilli()

                duration = timestamp - lastTimestamp
                hasNextRow = reader.nextRow()

                if (!hasNextRow) {
                    break
                }

                val shouldContinue = id == reader.get(RESOURCE_ID) &&
                    abs(cpuUsage - reader.getDouble(RESOURCE_STATE_CPU_USAGE)) < 0.01 &&
                    cpuCount == reader.getInt(RESOURCE_CPU_COUNT)
            } while (shouldContinue)

            writer.startRow()
            writer.set(RESOURCE_ID, id)
            writer.set(RESOURCE_STATE_TIMESTAMP, Instant.ofEpochMilli(startTimestamp))
            writer.set(RESOURCE_STATE_DURATION, Duration.ofMillis(duration))
            writer.setInt(RESOURCE_CPU_COUNT, cpuCount)
            writer.setDouble(RESOURCE_STATE_CPU_USAGE, cpuUsage)
            writer.endRow()

            count++

            lastId = id
            lastTimestamp = timestamp
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
