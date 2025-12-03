package org.opendc.compute.simulator.scheduler.carbonaware

/**
 * Internal state for the carbon-aware scheduling algorithm.
 *
 * This class maintains all the state needed for the depth-first search
 * with branch-and-bound optimization algorithm.
 *
 * @param taskCount Number of tasks to schedule
 * @param horizonSlots Number of time slots in the planning horizon
 * @param slotLengthMs Duration of each time slot in milliseconds
 */
public class CarbonScheduleState(
    public val taskCount: Int,
    public val horizonSlots: Int,
    public val slotLengthMs: Long,
) {
    /**
     * Topological order of tasks (parents before children)
     */
    public val topoOrder: IntArray = IntArray(taskCount)

    /**
     * Parent task indices for each task.
     * Element at index [i] contains the indices of all parent tasks of task [i].
     */
    public val parents: Array<IntArray> = Array(taskCount) { IntArray(0) }

    /**
     * Duration in slots for each task (rounded up)
     */
    public val durationSlots: IntArray = IntArray(taskCount)

    /**
     * Exact duration in milliseconds for each task
     */
    public val durationMs: LongArray = LongArray(taskCount)

    /**
     * Earliest slot a task can start (based on submission time)
     */
    public val releaseSlot: IntArray = IntArray(taskCount)

    /**
     * Current assignment of tasks to start slots during DFS
     * -1 means not yet assigned
     */
    public val assignment: IntArray = IntArray(taskCount) { -1 }

    /**
     * Number of tasks running in each slot (for capacity constraints)
     */
    public val slotLoad: IntArray = IntArray(horizonSlots)

    /**
     * Best assignment found so far
     */
    public val bestAssignment: IntArray = IntArray(taskCount) { -1 }

    /**
     * Best (minimum) cost found so far
     */
    public var bestCost: Double = Double.POSITIVE_INFINITY

    /**
     * Reset the state for a new optimization run
     */
    public fun reset() {
        assignment.fill(-1)
        slotLoad.fill(0)
        bestAssignment.fill(-1)
        bestCost = Double.POSITIVE_INFINITY
    }
}

