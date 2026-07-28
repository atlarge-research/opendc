/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.workload

import kotlinx.serialization.Serializable
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

private val EMPTY_INT_ARRAY = IntArray(0)

/**
 * Sentinel [TaskSpec.deadline] meaning the task has no deadline. Used instead of a nullable
 * [TimeDelta] so the field stays an unboxed primitive.
 */
public val NO_DEADLINE: TimeDelta = TimeDelta.ofMillis(-1)

/**
 * A schedulable unit of work with its resource requirements and execution profile.
 *
 * @property id Unique identifier of the task within its workload.
 * @property submissionTime Offset from the workload start at which the task is submitted.
 * @property duration Total wall-clock duration of the task.
 * @property cpuCoreCount Number of CPU cores the task requires.
 * @property cpuCapacity Per-core CPU capacity the task requires.
 * @property memory Amount of main memory the task requires.
 * @property fragments Ordered execution slices describing the task's resource demand over time.
 * @property gpuCoreCount Number of GPU cores the task requires.
 * @property gpuCapacity Per-core GPU capacity the task requires.
 * @property gpuMemory Amount of GPU memory the task requires.
 * @property deferrable Whether the task may be postponed by a time-shifting scheduler.
 * @property deadline Latest completion time, as an offset from the workload start, or [NO_DEADLINE] if none.
 * @property parents Identifiers of tasks that must complete before this task may start.
 * @property children Identifiers of tasks that depend on this task.
 */
@Serializable
public data class TaskSpec(
    public val id: Int,
    public val submissionTime: TimeDelta,
    public val duration: TimeDelta,
    public val cpuCoreCount: Short,
    public val cpuCapacity: Frequency,
    public val memory: DataSize,
    public val fragments: List<TaskFragmentSpec>,
    public val gpuCoreCount: Short = 0,
    public val gpuCapacity: Frequency = Frequency.ofMHz(0),
    public val gpuMemory: DataSize = DataSize.ofBytes(0),
    public val deferrable: Boolean = false,
    public val deadline: TimeDelta = NO_DEADLINE,
    public val parents: IntArray = EMPTY_INT_ARRAY,
    public val children: IntArray = EMPTY_INT_ARRAY,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (cpuCoreCount <= 0) add(ValidationIssue("cpuCoreCount", "must be greater than zero"))
            if (cpuCapacity <= Frequency.zero) add(ValidationIssue("cpuCapacity", "must be greater than zero"))
            if (fragments.isEmpty()) add(ValidationIssue("fragments", "must not be empty"))
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaskSpec) return false
        return id == other.id &&
            submissionTime == other.submissionTime &&
            duration == other.duration &&
            cpuCoreCount == other.cpuCoreCount &&
            cpuCapacity == other.cpuCapacity &&
            memory == other.memory &&
            fragments == other.fragments &&
            gpuCoreCount == other.gpuCoreCount &&
            gpuCapacity == other.gpuCapacity &&
            gpuMemory == other.gpuMemory &&
            deferrable == other.deferrable &&
            deadline == other.deadline &&
            parents.contentEquals(other.parents) &&
            children.contentEquals(other.children)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + submissionTime.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + cpuCoreCount
        result = 31 * result + cpuCapacity.hashCode()
        result = 31 * result + memory.hashCode()
        result = 31 * result + fragments.hashCode()
        result = 31 * result + gpuCoreCount
        result = 31 * result + gpuCapacity.hashCode()
        result = 31 * result + gpuMemory.hashCode()
        result = 31 * result + deferrable.hashCode()
        result = 31 * result + deadline.hashCode()
        result = 31 * result + parents.contentHashCode()
        result = 31 * result + children.contentHashCode()
        return result
    }

    override fun toString(): String =
        "TaskSpec(id=$id, submissionTime=$submissionTime, duration=$duration, cpuCoreCount=$cpuCoreCount, " +
            "cpuCapacity=$cpuCapacity, memory=$memory, fragments=$fragments, gpuCoreCount=$gpuCoreCount, " +
            "gpuCapacity=$gpuCapacity, gpuMemory=$gpuMemory, deferrable=$deferrable, deadline=$deadline, " +
            "parents=${parents.contentToString()}, children=${children.contentToString()})"
}
