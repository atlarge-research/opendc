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
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.workload.trace.TraceWorkload
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy
import org.opendc.trace.Trace
import org.opendc.trace.conv.FRAGMENT_CPU_USAGE
import org.opendc.trace.conv.FRAGMENT_DURATION
import org.opendc.trace.conv.FRAGMENT_GPU_USAGE
import org.opendc.trace.conv.TABLE_FRAGMENTS
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_CPU_CAPACITY
import org.opendc.trace.conv.TASK_CPU_COUNT
import org.opendc.trace.conv.TASK_DEADLINE
import org.opendc.trace.conv.TASK_DEFERRABLE
import org.opendc.trace.conv.TASK_DURATION
import org.opendc.trace.conv.TASK_GPU_CAPACITY
import org.opendc.trace.conv.TASK_GPU_COUNT
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_MEM_CAPACITY
import org.opendc.trace.conv.TASK_NAME
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_SUBMISSION_TIME
import java.io.File
import java.lang.ref.SoftReference
import java.time.Duration
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
    private val cache = ConcurrentHashMap<File, SoftReference<List<ServiceTask>>>()

    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(trace: Trace): Map<Int, Builder> {
        val reader = checkNotNull(trace.getTable(TABLE_FRAGMENTS)).newReader()

        val idCol = reader.resolve(TASK_ID)
        val durationCol = reader.resolve(FRAGMENT_DURATION)
        val usageCol = reader.resolve(FRAGMENT_CPU_USAGE)
        val resourceGpuCapacityCol = reader.resolve(FRAGMENT_GPU_USAGE)

        val fragments = mutableMapOf<Int, Builder>()

        return try {
            while (reader.nextRow()) {
                val id = reader.getInt(idCol)
                val durationMs = reader.getDuration(durationCol)!!
                val cpuUsage = reader.getDouble(usageCol)
                val gpuUsage =
                    if (reader.getDouble(
                            resourceGpuCapacityCol,
                        ).isNaN()
                    ) {
                        0.0
                    } else {
                        reader.getDouble(resourceGpuCapacityCol) // Default to 0 if not present
                    }
                val gpuMemory = 0 // Default to 0 if not present

                val builder =
                    fragments.computeIfAbsent(
                        id,
                    ) { Builder(checkpointInterval, checkpointDuration, checkpointIntervalScaling, scalingPolicy, id) }
                builder.add(durationMs, cpuUsage, gpuUsage, gpuMemory)
            }

            fragments
        } finally {
            reader.close()
        }
    }

    /**
     * Read the metadata into a workload.
     */
    private fun parseTasks(
        trace: Trace,
        fragments: Map<Int, Builder>,
    ): List<ServiceTask> {
        val reader = checkNotNull(trace.getTable(TABLE_TASKS)).newReader()

        val idCol = reader.resolve(TASK_ID)
        val idName = reader.resolve(TASK_NAME)
        val submissionTimeCol = reader.resolve(TASK_SUBMISSION_TIME)
        val durationCol = reader.resolve(TASK_DURATION)
        val cpuCountCol = reader.resolve(TASK_CPU_COUNT)
        val cpuCapacityCol = reader.resolve(TASK_CPU_CAPACITY)
        val memCol = reader.resolve(TASK_MEM_CAPACITY)
        val gpuCapacityCol = reader.resolve(TASK_GPU_CAPACITY) // Assuming GPU capacity is also present
        val gpuCoreCountCol = reader.resolve(TASK_GPU_COUNT) // Assuming GPU cores are also present
        val parentsCol = reader.resolve(TASK_PARENTS)
        val childrenCol = reader.resolve(TASK_CHILDREN)
        val deferrableCol = reader.resolve(TASK_DEFERRABLE)
        val deadlineCol = reader.resolve(TASK_DEADLINE)

        val entries = mutableListOf<ServiceTask>()

        return try {
            while (reader.nextRow()) {
                val id = reader.getInt(idCol)
                var name = reader.getString(idName)

                if (!fragments.containsKey(id)) {
                    continue
                }

                val submissionTime = reader.getInstant(submissionTimeCol)!!.toEpochMilli()
                val duration = reader.getLong(durationCol)
                val cpuCoreCount = reader.getInt(cpuCountCol)
                val cpuCapacity = reader.getDouble(cpuCapacityCol)
                val memUsage = reader.getDouble(memCol) / 1000.0 // Convert from KB to MB
                val gpuCapacity =
                    if (reader.getDouble(
                            gpuCapacityCol,
                        ).isNaN()
                    ) {
                        0.0
                    } else {
                        reader.getDouble(gpuCapacityCol) // Default to 0 if not present// Default to 0 if not present
                    }
                val gpuCoreCount = reader.getInt(gpuCoreCountCol) // Default to 0 if not present
                val gpuMemory = 0L // currently not implemented

                var parents = reader.getSet(parentsCol, Int::class.java) // No dependencies in the trace
                var children = reader.getSet(childrenCol, Int::class.java) // No dependencies in the trace

                var parentsOutput: ArrayList<Int>? = null

                if (parents?.isEmpty() == true) {
                    parentsOutput = null
                    children = null
                } else {
                    parentsOutput = ArrayList(parents!!)
                }

                var deferrable = reader.getBoolean(deferrableCol)
                var deadline = reader.getLong(deadlineCol)
                if (deferAll) {
                    deferrable = true
                    deadline = submissionTime + (3 * duration)
                }

                val builder = fragments.getValue(id) // Get all fragments related to this VM
                val totalLoad = builder.totalLoad

                entries.add(
                    ServiceTask(
                        id,
                        name,
                        submissionTime,
                        duration,
                        cpuCoreCount,
                        cpuCapacity,
                        totalLoad,
                        memUsage.roundToLong(),
                        gpuCoreCount,
                        gpuCapacity,
                        gpuMemory,
                        builder.build(),
                        deferrable,
                        deadline,
                        parentsOutput,
                        children,
                    ),
                )
            }

            // Make sure the virtual machines are ordered by start time
            entries.sortBy { it.submittedAt }

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
    override fun load(): List<ServiceTask> {
        val trace = Trace.open(pathToFile, "workload")
        val fragments = parseFragments(trace)
        val vms = parseTasks(trace, fragments)

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
        taskId: Int,
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
                taskId,
            )

        /**
         * Add a fragment to the trace.
         *
         * @param duration The duration of the fragment (in epoch millis).
         * @param cpuUsage CPU usage of this fragment.
         * @param gpuUsage GPU usage of this fragment.
         * @param gpuMemoryUsage GPU memory usage of this fragment.
         */
        fun add(
            duration: Duration,
            cpuUsage: Double,
            gpuUsage: Double = 0.0,
            gpuMemoryUsage: Int = 0,
        ) {
            totalLoad += ((cpuUsage * duration.toMillis()) + (gpuUsage * duration.toMillis())) / 1000 // avg MHz * duration = MFLOPs

            builder.add(duration.toMillis(), cpuUsage, gpuUsage, gpuMemoryUsage)
        }

        /**
         * Build the trace.
         */
        fun build(): TraceWorkload = builder.build()
    }
}
