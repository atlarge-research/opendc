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

package org.opendc.sdk.model.workload.loader

import mu.KotlinLogging
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.workload.Workload
import org.opendc.simulator.compute.workload.trace.TraceWorkload
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy
import org.opendc.trace.TableReader
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
import org.opendc.trace.conv.TASK_NUM_FRAGMENTS
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
public class EfficientWorkloadLoader(
    private val pathToFile: File,
    private val submissionTime: String? = null,
    private val checkpointInterval: Long = 0L,
    private val checkpointDuration: Long = 0L,
    private val checkpointIntervalScaling: Double = 1.0,
    private val scalingPolicy: ScalingPolicy = NoDelayScaling(),
    private val deferAll: Boolean = false,
) : WorkloadLoader(submissionTime) {
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
    private fun getWorkload(fragmentReader: TableReader, taskId: Int, numFragments: Int): Workload {
        val idCol = fragmentReader.resolve(TASK_ID)
        val durationCol = fragmentReader.resolve(FRAGMENT_DURATION)
        val usageCol = fragmentReader.resolve(FRAGMENT_CPU_USAGE)
        val resourceGpuCapacityCol = fragmentReader.resolve(FRAGMENT_GPU_USAGE)

        val builder = Builder(checkpointInterval, checkpointDuration, checkpointIntervalScaling, scalingPolicy, taskId, numFragments)

        try {
            while (true) {
                val fragmentId = fragmentReader.getInt(idCol)
                if (fragmentId != taskId) {
                    break;
                }

                val durationMs = fragmentReader.getDuration(durationCol)!!
                val cpuUsage = fragmentReader.getDouble(usageCol)
                val gpuUsage =
                    if (fragmentReader.getDouble(
                            resourceGpuCapacityCol,
                        ).isNaN()
                    ) {
                        0.0
                    } else {
                        fragmentReader.getDouble(resourceGpuCapacityCol) // Default to 0 if not present
                    }
                val gpuMemory = 0 // Default to 0 if not present

                builder.add(durationMs, cpuUsage, gpuUsage, gpuMemory)
                fragmentReader.nextRow()
            }
            return builder.build()
        } catch (e: Exception) {
            return builder.build()
        }
    }

    /**
     * Read the metadata into a workload.
     */
    private fun loadTrace(
        trace: Trace,
    ): List<ServiceTask> {
        val taskReader = checkNotNull(trace.getTable(TABLE_TASKS)).newReader()
        val fragmentReader = checkNotNull(trace.getTable(TABLE_FRAGMENTS)).newReader()
        fragmentReader.nextRow();

        val taskIdCol = taskReader.resolve(TASK_ID)
        val submissionTimeCol = taskReader.resolve(TASK_SUBMISSION_TIME)
        val taskDurationCol = taskReader.resolve(TASK_DURATION)
        val cpuCountCol = taskReader.resolve(TASK_CPU_COUNT)
        val cpuCapacityCol = taskReader.resolve(TASK_CPU_CAPACITY)
        val memCol = taskReader.resolve(TASK_MEM_CAPACITY)
        val gpuCapacityCol = taskReader.resolve(TASK_GPU_CAPACITY) // Assuming GPU capacity is also present
        val gpuCoreCountCol = taskReader.resolve(TASK_GPU_COUNT) // Assuming GPU cores are also present
        val parentsCol = taskReader.resolve(TASK_PARENTS)
        val childrenCol = taskReader.resolve(TASK_CHILDREN)
        val deferrableCol = taskReader.resolve(TASK_DEFERRABLE)
        val deadlineCol = taskReader.resolve(TASK_DEADLINE)
        val numFragmentsCol = taskReader.resolve(TASK_NUM_FRAGMENTS)

        val trace = mutableListOf<ServiceTask>()

        return try {
            while (taskReader.nextRow()) {
                val id = taskReader.getInt(taskIdCol)

                val submissionTime = taskReader.getInstant(submissionTimeCol)!!.toEpochMilli()
                val duration = taskReader.getLong(taskDurationCol)
                val cpuCoreCount = taskReader.getInt(cpuCountCol)
                val cpuCapacity = taskReader.getDouble(cpuCapacityCol)
                val memUsage = taskReader.getDouble(memCol) / 1000.0 // Convert from KB to MB
                val gpuCapacity =
                    if (taskReader.getDouble(
                            gpuCapacityCol,
                        ).isNaN()
                    ) {
                        0.0
                    } else {
                        taskReader.getDouble(gpuCapacityCol) // Default to 0 if not present
                    }
                val gpuCoreCount = taskReader.getInt(gpuCoreCountCol) // Default to 0 if not present
                val gpuMemory = 0L // currently not implemented

                val parents = taskReader.getSet(parentsCol, Int::class.java) // No dependencies in the trace
                val children = taskReader.getSet(childrenCol, Int::class.java) // No dependencies in the trace

                val parentsOutput: IntArray? = if (parents.isNullOrEmpty()) null else parents.toIntArray()
                val childrenOutput: IntArray? = if (children.isNullOrEmpty()) null else children.toIntArray()

                var deferrable = taskReader.getBoolean(deferrableCol)
                var deadline = taskReader.getLong(deadlineCol)
                if (deferAll) {
                    deferrable = true
                    deadline = submissionTime + (3 * duration)
                }

                val numFragments = taskReader.getInt(numFragmentsCol)

                if (numFragments == -1) {
                    throw Exception("Trace should contain the numFragment column to use the EfficientWorkloadLoader")
                }

                // Get Workload
                val workload = getWorkload(fragmentReader, id, numFragments)

                if (workload.length == 0) {
                    logger.warn("Task $id does not have any fragments. Make sure your trace is properly ordered.")
                    continue
                }

                trace.add(
                    ServiceTask(
                        id,
                        submissionTime,
                        duration,
                        cpuCoreCount,
                        cpuCapacity,
                        memUsage.roundToLong(),
                        gpuCoreCount,
                        gpuCapacity,
                        gpuMemory,
                        workload,
                        deferrable,
                        deadline,
                        parentsOutput,
                        childrenOutput,
                    ),
                )
            }

            trace
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            taskReader.close()
            fragmentReader.close()
        }
    }

    /**
     * Load the trace at the specified [pathToFile].
     */
    override fun load(): List<ServiceTask> {
        val trace = Trace.open(pathToFile, "workload")

        println("LOADED Fragments")

        return loadTrace(trace)
    }

    /**
     * Clear the workload cache.
     */
    public fun reset() {
        cache.clear()
    }

    /**
     * A builder for a VM trace.
     *
     */
    private class Builder(
        checkpointInterval: Long,
        checkpointDuration: Long,
        checkpointIntervalScaling: Double,
        scalingPolicy: ScalingPolicy,
        taskId: Int,
        numFragments: Int
    ) {
        /**
         * The internal builder for the trace.
         */
        private val builder =
            TraceWorkload.efficientBuilder(
                checkpointInterval,
                checkpointDuration,
                checkpointIntervalScaling,
                scalingPolicy,
                taskId,
                numFragments
            )

        /**
         * Add a fragment to the trace.
         *
         * @param duration The duration of the fragment (in epoch millis).
         * @param cpuUsage CPU usage of this fragment.
         * @param gpuUsage GPU usage of this fragment.
         * @param gpuMemoryUsage GPU memory usage of this fragment.
         *
         * TODO:
         */
        fun add(
            duration: Duration,
            cpuUsage: Double,
            gpuUsage: Double = 0.0,
            gpuMemoryUsage: Int = 0,
        ) {
            if (duration == Duration.ofMillis(0)) {
                return
            }
            builder.add(duration.toMillis(), cpuUsage, gpuUsage, gpuMemoryUsage)
        }

        /**
         * Build the trace.
         */
        fun build(): TraceWorkload = builder.build()
    }
}
