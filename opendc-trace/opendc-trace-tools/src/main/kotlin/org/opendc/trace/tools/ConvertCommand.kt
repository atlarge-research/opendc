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

package org.opendc.trace.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.restrictTo
import mu.KotlinLogging
import org.opendc.trace.TableWriter
import org.opendc.trace.Trace
import org.opendc.trace.conv.RESOURCE_CPU_CAPACITY
import org.opendc.trace.conv.RESOURCE_CPU_COUNT
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.RESOURCE_MEM_CAPACITY
import org.opendc.trace.conv.RESOURCE_START_TIME
import org.opendc.trace.conv.RESOURCE_STATE_CPU_USAGE
import org.opendc.trace.conv.RESOURCE_STATE_CPU_USAGE_PCT
import org.opendc.trace.conv.RESOURCE_STATE_DURATION
import org.opendc.trace.conv.RESOURCE_STATE_MEM_USAGE
import org.opendc.trace.conv.RESOURCE_STATE_TIMESTAMP
import org.opendc.trace.conv.RESOURCE_STOP_TIME
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Random
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A [CliktCommand] that can convert between workload trace formats.
 */
internal class ConvertCommand : CliktCommand(name = "convert", help = "Convert between workload trace formats") {
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
    private val inputFormat by option("-f", "--input-format", help = "format of input trace")
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

    /**
     * The converter strategy to use.
     */
    private val converter by option("-c", "--converter", help = "converter strategy to use").groupChoice(
        "default" to DefaultTraceConverter(),
        "azure" to AzureTraceConverter()
    ).defaultByName("default")

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

        val selectedVms = metaWriter.use { converter.convertResources(inputTrace, it, samplingOptions) }

        if (selectedVms.isEmpty()) {
            logger.warn { "No VMs selected" }
            return
        }

        logger.info { "Wrote ${selectedVms.size} rows" }
        logger.info { "Building resource states table" }

        val writer = outputTrace.getTable(TABLE_RESOURCE_STATES)!!.newWriter()

        val statesCount = writer.use { converter.convertResourceStates(inputTrace, it, selectedVms) }
        logger.info { "Wrote $statesCount rows" }
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

    /**
     * A trace conversion strategy.
     */
    private sealed class TraceConverter(name: String) : OptionGroup(name) {
        /**
         * Convert the resources table for the trace.
         *
         * @param trace The trace to convert.
         * @param writer The table writer for the target format.
         * @param samplingOptions The sampling options to use.
         * @return The map of resources that have been selected.
         */
        abstract fun convertResources(trace: Trace, writer: TableWriter, samplingOptions: SamplingOptions?): Map<String, Resource>

        /**
         * Convert the resource states table for the trace.
         *
         * @param trace The trace to convert.
         * @param writer The table writer for the target format.
         * @param selected The set of virtual machines that have been selected.
         * @return The number of rows written.
         */
        abstract fun convertResourceStates(trace: Trace, writer: TableWriter, selected: Map<String, Resource>): Int

        /**
         * A resource in the resource table.
         */
        data class Resource(
            val id: String,
            val startTime: Instant,
            val stopTime: Instant,
            val cpuCount: Int,
            val cpuCapacity: Double,
            val memCapacity: Double
        )
    }

    /**
     * Default implementation of [TraceConverter].
     */
    private class DefaultTraceConverter : TraceConverter("default") {
        /**
         * The logger instance for the converter.
         */
        private val logger = KotlinLogging.logger {}

        /**
         * The interval at which the samples where taken.
         */
        private val SAMPLE_INTERVAL = Duration.ofMinutes(5)

        /**
         * The difference in CPU usage for the algorithm to cascade samples.
         */
        private val SAMPLE_CASCADE_DIFF = 0.1

        override fun convertResources(trace: Trace, writer: TableWriter, samplingOptions: SamplingOptions?): Map<String, Resource> {
            val random = samplingOptions?.let { Random(it.seed) }
            val samplingFraction = samplingOptions?.fraction ?: 1.0
            val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

            var hasNextRow = reader.nextRow()
            val selectedVms = mutableMapOf<String, Resource>()

            val idCol = reader.resolve(RESOURCE_ID)
            val timestampCol = reader.resolve(RESOURCE_STATE_TIMESTAMP)
            val cpuCountCol = reader.resolve(RESOURCE_CPU_COUNT)
            val cpuCapacityCol = reader.resolve(RESOURCE_CPU_CAPACITY)
            val memCapacityCol = reader.resolve(RESOURCE_MEM_CAPACITY)
            val memUsageCol = reader.resolve(RESOURCE_STATE_MEM_USAGE)

            while (hasNextRow) {
                var id: String
                var cpuCount = 0
                var cpuCapacity = 0.0
                var memCapacity = 0.0
                var memUsage = 0.0
                var startTime = Long.MAX_VALUE
                var stopTime = Long.MIN_VALUE

                do {
                    id = reader.getString(idCol)!!

                    val timestamp = reader.getInstant(timestampCol)!!.toEpochMilli()
                    startTime = min(startTime, timestamp)
                    stopTime = max(stopTime, timestamp)

                    cpuCount = max(cpuCount, reader.getInt(cpuCountCol))
                    cpuCapacity = max(cpuCapacity, reader.getDouble(cpuCapacityCol))
                    memCapacity = max(memCapacity, reader.getDouble(memCapacityCol))
                    if (memUsageCol > 0) {
                        memUsage = max(memUsage, reader.getDouble(memUsageCol))
                    }

                    hasNextRow = reader.nextRow()
                } while (hasNextRow && id == reader.getString(RESOURCE_ID))

                // Sample only a fraction of the VMs
                if (random != null && random.nextDouble() > samplingFraction) {
                    continue
                }

                logger.info { "Selecting VM $id" }

                val startInstant = Instant.ofEpochMilli(startTime) - SAMPLE_INTERVAL // Offset by sample interval
                val stopInstant = Instant.ofEpochMilli(stopTime)

                selectedVms.computeIfAbsent(id) {
                    Resource(it, startInstant, stopInstant, cpuCount, cpuCapacity, max(memCapacity, memUsage))
                }

                writer.startRow()
                writer.setString(RESOURCE_ID, id)
                writer.setInstant(RESOURCE_START_TIME, startInstant)
                writer.setInstant(RESOURCE_STOP_TIME, stopInstant)
                writer.setInt(RESOURCE_CPU_COUNT, cpuCount)
                writer.setDouble(RESOURCE_CPU_CAPACITY, cpuCapacity)
                writer.setDouble(RESOURCE_MEM_CAPACITY, max(memCapacity, memUsage))
                writer.endRow()
            }

            return selectedVms
        }

        override fun convertResourceStates(trace: Trace, writer: TableWriter, selected: Map<String, Resource>): Int {
            val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()
            val sampleInterval = SAMPLE_INTERVAL.toMillis()

            val idCol = reader.resolve(RESOURCE_ID)
            val timestampCol = reader.resolve(RESOURCE_STATE_TIMESTAMP)
            val cpuCountCol = reader.resolve(RESOURCE_CPU_COUNT)
            val cpuUsageCol = reader.resolve(RESOURCE_STATE_CPU_USAGE)

            var hasNextRow = reader.nextRow()
            var count = 0

            while (hasNextRow) {
                val id = reader.getString(idCol)!!
                val resource = selected[id]
                if (resource == null) {
                    hasNextRow = reader.nextRow()
                    continue
                }

                val cpuCount = reader.getInt(cpuCountCol)
                val cpuUsage = reader.getDouble(cpuUsageCol)

                val startTimestamp = reader.getInstant(timestampCol)!!.toEpochMilli()
                var timestamp: Long = startTimestamp
                var duration: Long = sampleInterval

                // Attempt to cascade further samples into one if they share the same CPU usage
                while (reader.nextRow().also { hasNextRow = it }) {
                    val shouldCascade = id == reader.getString(idCol) &&
                        abs(cpuUsage - reader.getDouble(cpuUsageCol)) < SAMPLE_CASCADE_DIFF &&
                        cpuCount == reader.getInt(cpuCountCol)

                    // Check whether the next sample can be cascaded with the current sample:
                    // (1) The VM identifier of both samples matches
                    // (2) The CPU usage is almost identical (lower than `SAMPLE_CASCADE_DIFF`
                    // (3) The CPU count of both samples is identical
                    if (!shouldCascade) {
                        break
                    }

                    val nextTimestamp = reader.getInstant(timestampCol)!!.toEpochMilli()

                    // Check whether the interval between both samples is not higher than `SAMPLE_INTERVAL`
                    if ((nextTimestamp - timestamp) > sampleInterval) {
                        break
                    }

                    duration += nextTimestamp - timestamp
                    timestamp = nextTimestamp
                }

                writer.startRow()
                writer.setString(RESOURCE_ID, id)
                writer.setInstant(RESOURCE_STATE_TIMESTAMP, Instant.ofEpochMilli(timestamp))
                writer.setDuration(RESOURCE_STATE_DURATION, Duration.ofMillis(duration))
                writer.setInt(RESOURCE_CPU_COUNT, cpuCount)
                writer.setDouble(RESOURCE_STATE_CPU_USAGE, cpuUsage)
                writer.endRow()

                count++
            }

            return count
        }
    }

    /**
     * Implementation of [TraceConverter] for the Azure trace format.
     */
    private class AzureTraceConverter : TraceConverter("default") {
        /**
         * The logger instance for the converter.
         */
        private val logger = KotlinLogging.logger {}

        /**
         * CPU capacity of the machines used by Azure.
         */
        private val CPU_CAPACITY = 2500.0

        /**
         * The interval at which the samples where taken.
         */
        private val SAMPLE_INTERVAL = Duration.ofMinutes(5)

        /**
         * The difference in CPU usage for the algorithm to cascade samples.
         */
        private val SAMPLE_CASCADE_DIFF = 0.1

        override fun convertResources(trace: Trace, writer: TableWriter, samplingOptions: SamplingOptions?): Map<String, Resource> {
            val random = samplingOptions?.let { Random(it.seed) }
            val samplingFraction = samplingOptions?.fraction ?: 1.0
            val reader = checkNotNull(trace.getTable(TABLE_RESOURCES)).newReader()

            val idCol = reader.resolve(RESOURCE_ID)
            val startTimeCol = reader.resolve(RESOURCE_START_TIME)
            val stopTimeCol = reader.resolve(RESOURCE_STOP_TIME)
            val cpuCountCol = reader.resolve(RESOURCE_CPU_COUNT)
            val memCapacityCol = reader.resolve(RESOURCE_MEM_CAPACITY)

            val selectedVms = mutableMapOf<String, Resource>()

            while (reader.nextRow()) {
                // Sample only a fraction of the VMs
                if (random != null && random.nextDouble() > samplingFraction) {
                    continue
                }

                val id = reader.getString(idCol)!!
                val startTime = reader.getInstant(startTimeCol)!!.toEpochMilli()
                val stopTime = reader.getInstant(stopTimeCol)!!.toEpochMilli()
                val cpuCount = reader.getInt(cpuCountCol)
                val memCapacity = reader.getDouble(memCapacityCol)

                logger.info { "Selecting VM $id" }

                val startInstant = Instant.ofEpochMilli(startTime)
                val stopInstant = Instant.ofEpochMilli(stopTime)
                val cpuCapacity = cpuCount * CPU_CAPACITY

                selectedVms.computeIfAbsent(id) {
                    Resource(it, startInstant, stopInstant, cpuCount, cpuCapacity, memCapacity)
                }

                writer.startRow()
                writer.setString(RESOURCE_ID, id)
                writer.setInstant(RESOURCE_START_TIME, startInstant)
                writer.setInstant(RESOURCE_STOP_TIME, stopInstant)
                writer.setInt(RESOURCE_CPU_COUNT, cpuCount)
                writer.setDouble(RESOURCE_CPU_CAPACITY, cpuCapacity)
                writer.setDouble(RESOURCE_MEM_CAPACITY, memCapacity)
                writer.endRow()
            }

            return selectedVms
        }

        override fun convertResourceStates(trace: Trace, writer: TableWriter, selected: Map<String, Resource>): Int {
            val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()
            val states = HashMap<String, State>()
            val sampleInterval = SAMPLE_INTERVAL.toMillis()

            val idCol = reader.resolve(RESOURCE_ID)
            val timestampCol = reader.resolve(RESOURCE_STATE_TIMESTAMP)
            val cpuUsageCol = reader.resolve(RESOURCE_STATE_CPU_USAGE_PCT)

            var count = 0

            while (reader.nextRow()) {
                val id = reader.getString(idCol)!!
                val resource = selected[id] ?: continue

                val cpuUsage = reader.getDouble(cpuUsageCol) * resource.cpuCapacity // MHz
                val state = states.computeIfAbsent(id) { State(resource, cpuUsage, sampleInterval) }
                val timestamp = reader.getInstant(timestampCol)!!.toEpochMilli()
                val delta = (timestamp - state.time)

                // Check whether the next sample can be cascaded with the current sample:
                // (1) The CPU usage is almost identical (lower than `SAMPLE_CASCADE_DIFF`)
                // (2) The interval between both samples is not higher than `SAMPLE_INTERVAL`
                if (abs(cpuUsage - state.cpuUsage) <= SAMPLE_CASCADE_DIFF && delta <= sampleInterval) {
                    state.time = timestamp
                    state.duration += delta
                    continue
                }

                state.write(writer)
                // Reset the state fields
                state.time = timestamp
                state.duration = sampleInterval
                // Count write
                count++
            }

            for ((_, state) in states) {
                state.write(writer)
                count++
            }

            return count
        }

        private class State(@JvmField val resource: Resource, @JvmField var cpuUsage: Double, @JvmField var duration: Long) {
            @JvmField var time: Long = resource.startTime.toEpochMilli()
            private var lastWrite: Long = Long.MIN_VALUE

            fun write(writer: TableWriter) {
                // Check whether this timestamp was already written
                if (lastWrite == time) {
                    return
                }
                lastWrite = time

                writer.startRow()
                writer.setString(RESOURCE_ID, resource.id)
                writer.setInstant(RESOURCE_STATE_TIMESTAMP, Instant.ofEpochMilli(time))
                writer.setDuration(RESOURCE_STATE_DURATION, Duration.ofMillis(duration))
                writer.setDouble(RESOURCE_STATE_CPU_USAGE, cpuUsage)
                writer.setInt(RESOURCE_CPU_COUNT, resource.cpuCount)
                writer.endRow()
            }
        }
    }
}
