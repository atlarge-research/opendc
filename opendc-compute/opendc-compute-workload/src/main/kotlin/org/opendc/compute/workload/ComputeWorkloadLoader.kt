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

package org.opendc.compute.workload

import mu.KotlinLogging
import org.opendc.simulator.compute.workload.trace.TraceWorkload
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy
import org.opendc.trace.Trace
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.conv.resourceCpuCapacity
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceDeadline
import org.opendc.trace.conv.resourceDuration
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceNature
import org.opendc.trace.conv.resourceStateAccelUsage
import org.opendc.trace.conv.resourceStateCpuUsage
import org.opendc.trace.conv.resourceStateDuration
import org.opendc.trace.conv.resourceStateIsGpu
import org.opendc.trace.conv.resourceSubmissionTime
import java.io.File
import java.lang.ref.SoftReference
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * A helper class for loading compute workload traces into memory.
 *
 */
public class ComputeWorkloadLoader(
    private val pathToFile: File,
    private val subMissionTime: String? = null,
    private val checkpointInterval: Long = 0L,
    private val checkpointDuration: Long = 0L,
    private val checkpointIntervalScaling: Double = 1.0,
    private val scalingPolicy: ScalingPolicy = NoDelayScaling(),
    private val deferAll: Boolean = false,
) : WorkloadLoader(subMissionTime) {
    /**
     * The logger for this instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The cache of workloads.
     */
    private val cache = ConcurrentHashMap<File, SoftReference<List<Task>>>()

    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(trace: Trace): Map<String, Builder> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        val idCol = reader.resolve(resourceID)
        val durationCol = reader.resolve(resourceStateDuration)
        val coresCol = reader.resolve(resourceCpuCount)
        val usageCol = reader.resolve(resourceStateCpuUsage)
        val accelUsageCol = reader.resolve(resourceStateAccelUsage)
        val isGpuCol = reader.resolve(resourceStateIsGpu)

        val fragments = mutableMapOf<String, Builder>()

        return try {
            while (reader.nextRow()) {
                val id = reader.getString(idCol)!!
                val durationMs = reader.getDuration(durationCol)!!
                val cores = reader.getInt(coresCol)
                val cpuUsage = reader.getDouble(usageCol)
                val accelUsage = reader.getDouble(accelUsageCol)
                val isGpu = reader.getBoolean(isGpuCol)

                val builder =
                    fragments.computeIfAbsent(
                        id,
                    ) { Builder(checkpointInterval, checkpointDuration, checkpointIntervalScaling, scalingPolicy, id) }
                builder.add(durationMs, cpuUsage, cores, accelUsage, isGpu)
            }

            fragments
        } finally {
            reader.close()
        }
    }

    /**
     * Read the metadata into a workload.
     */
    private fun parseMeta(
        trace: Trace,
        fragments: Map<String, Builder>,
    ): List<Task> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCES)).newReader()

        val idCol = reader.resolve(resourceID)
        val submissionTimeCol = reader.resolve(resourceSubmissionTime)
        val durationCol = reader.resolve(resourceDuration)
        val cpuCountCol = reader.resolve(resourceCpuCount)
        val cpuCapacityCol = reader.resolve(resourceCpuCapacity)
        val memCol = reader.resolve(resourceMemCapacity)
        val natureCol = reader.resolve(resourceNature)
        val deadlineCol = reader.resolve(resourceDeadline)

        var counter = 0
        val entries = mutableListOf<Task>()

        return try {
            while (reader.nextRow()) {
                val id = reader.getString(idCol)!!
                if (!fragments.containsKey(id)) {
                    continue
                }

                val submissionTime = reader.getInstant(submissionTimeCol)!!.toEpochMilli()
                val duration = reader.getLong(durationCol)
                val cpuCount = reader.getInt(cpuCountCol)
                val cpuCapacity = reader.getDouble(cpuCapacityCol)
                val memCapacity = reader.getDouble(memCol) / 1000.0 // Convert from KB to MB
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())
                var nature = reader.getString(natureCol)
                var deadline = reader.getLong(deadlineCol)
                if (deferAll) {
                    nature = "deferrable"
                    deadline = submissionTime + (3 * duration)
                }

                val builder = fragments.getValue(id) // Get all fragments related to this VM
                val totalLoad = builder.totalLoad

                entries.add(
                    Task(
                        uid,
                        id,
                        cpuCount,
                        cpuCapacity,
                        memCapacity.roundToLong(),
                        totalLoad,
                        submissionTime,
                        duration,
                        nature,
                        deadline,
                        builder.build(),
                    ),
                )
            }

            // Make sure the virtual machines are ordered by start time
            entries.sortBy { it.submissionTime }

            entries
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            reader.close()
        }
    }

    /**
     * Load the trace at the specified [pathToFile].
     */
    override fun load(): List<Task> {
        val trace = Trace.open(pathToFile, "opendc-vm")
        val fragments = parseFragments(trace)
        val vms = parseMeta(trace, fragments)

        return vms
    }

    /**
     * Clear the workload cache.
     */
    public fun reset() {
        cache.clear()
    }

    /**
     * A builder for a VM trace.
     */
    private class Builder(
        checkpointInterval: Long,
        checkpointDuration: Long,
        checkpointIntervalScaling: Double,
        scalingPolicy: ScalingPolicy,
        taskName: String,
    ) {
        /**
         * The total load of the trace.
         */
        @JvmField var totalLoad: Double = 0.0

        /**
         * The internal builder for the trace.
         */
        private val builder =
            TraceWorkload.builder(
                checkpointInterval,
                checkpointDuration,
                checkpointIntervalScaling,
                scalingPolicy,
                taskName,
            )

        /**
         * Add a fragment to the trace.
         *
         * @param duration The duration of the fragment (in epoch millis).
         * @param usage CPU usage of this fragment.
         * @param cores Number of cores used.
         */
        fun add(
            duration: Duration,
            usage: Double,
            cores: Int,
            accelUsage: Double,
            isGpu: Boolean,
        ) {
            totalLoad += (usage * duration.toMillis()) / 1000 // avg MHz * duration = MFLOPs

            builder.add(duration.toMillis(), usage, cores, accelUsage, isGpu)
        }

        /**
         * Build the trace.
         */
        fun build(): TraceWorkload = builder.build()
    }
}
