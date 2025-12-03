package org.opendc.compute.simulator.scheduler.carbonaware

import kotlin.math.min

/**
 * Core optimization algorithm using depth-first search with branch-and-bound
 * to find the carbon-minimal schedule for a workflow.
 *
 * @param carbonIntensity Array of carbon intensity values for each time slot
 * @param maxParallelTasks Maximum number of tasks that can run in parallel (number of hosts)
 */
public class CarbonOptimizer(
    private val carbonIntensity: DoubleArray,
    private val maxParallelTasks: Int,
) {
    /**
     * Optimize the task schedule to minimize carbon emissions.
     *
     * This method performs a depth-first search with branch-and-bound pruning
     * to explore all valid task placements and find the one with minimum carbon cost.
     *
     * @param state The scheduling state containing tasks and constraints
     */
    public fun optimize(state: CarbonScheduleState) {
        state.reset()
        dfs(0, state, 0.0)
    }

    /**
     * Recursive depth-first search to explore all valid task placements.
     *
     * @param index Current position in the topological order
     * @param state The scheduling state
     * @param currentCost Accumulated carbon cost so far
     */
    private fun dfs(
        index: Int,
        state: CarbonScheduleState,
        currentCost: Double,
    ) {
        // Base case: all tasks have been assigned
        if (index == state.taskCount) {
            if (currentCost < state.bestCost) {
                state.bestCost = currentCost
                state.assignment.copyInto(state.bestAssignment)
            }
            return
        }

        val taskIdx = state.topoOrder[index]
        val durSlots = state.durationSlots[taskIdx]
        val durMs = state.durationMs[taskIdx]

        // Calculate earliest start slot based on release time and parent completion
        var earliest = state.releaseSlot[taskIdx]
        for (parentIdx in state.parents[taskIdx]) {
            val parentStart = state.assignment[parentIdx]
            val parentFinish = parentStart + state.durationSlots[parentIdx]
            if (parentFinish > earliest) {
                earliest = parentFinish
            }
        }

        // Try all possible start slots for this task
        for (startSlot in earliest until state.horizonSlots) {
            val endSlot = startSlot + durSlots
            if (endSlot > state.horizonSlots) break

            // Check capacity constraints
            if (!hasCapacity(state, startSlot, endSlot)) continue

            // Calculate carbon cost for this task placement
            val taskCost =
                calculateCarbonCost(
                    startSlot,
                    durSlots,
                    durMs,
                    state.slotLengthMs,
                    carbonIntensity,
                )

            val newCost = currentCost + taskCost

            // Branch-and-bound pruning: skip if already worse than best
            if (newCost >= state.bestCost) continue

            // Place task in this slot
            state.assignment[taskIdx] = startSlot
            updateSlotLoad(state, startSlot, endSlot, 1)

            // Recurse to next task
            dfs(index + 1, state, newCost)

            // Backtrack: remove task from this slot
            updateSlotLoad(state, startSlot, endSlot, -1)
            state.assignment[taskIdx] = -1
        }
    }

    /**
     * Check if there is capacity to run a task in the given slot range.
     *
     * @param state The scheduling state
     * @param startSlot First slot the task would occupy
     * @param endSlot Slot after the last slot the task would occupy
     * @return true if there is capacity in all slots, false otherwise
     */
    private fun hasCapacity(
        state: CarbonScheduleState,
        startSlot: Int,
        endSlot: Int,
    ): Boolean {
        for (slot in startSlot until endSlot) {
            if (state.slotLoad[slot] >= maxParallelTasks) {
                return false
            }
        }
        return true
    }

    /**
     * Update the load counter for slots when adding or removing a task.
     *
     * @param state The scheduling state
     * @param startSlot First slot the task occupies
     * @param endSlot Slot after the last slot the task occupies
     * @param delta +1 to add task, -1 to remove task
     */
    private fun updateSlotLoad(
        state: CarbonScheduleState,
        startSlot: Int,
        endSlot: Int,
        delta: Int,
    ) {
        for (slot in startSlot until endSlot) {
            state.slotLoad[slot] += delta
        }
    }

    /**
     * Calculate the carbon cost of running a task starting at a given slot.
     *
     * The cost is calculated proportionally based on how much of each slot
     * the task actually uses (handles fractional slot usage).
     *
     * @param startSlot First slot the task would occupy
     * @param durSlots Number of slots the task spans
     * @param durMs Exact duration of the task in milliseconds
     * @param slotLengthMs Duration of each time slot in milliseconds
     * @param carbonIntensity Array of carbon intensity values
     * @return Total carbon cost for running this task
     */
    private fun calculateCarbonCost(
        startSlot: Int,
        durSlots: Int,
        durMs: Long,
        slotLengthMs: Long,
        carbonIntensity: DoubleArray,
    ): Double {
        var cost = 0.0
        var remaining = durMs

        for (k in 0 until durSlots) {
            if (remaining <= 0) break

            val slot = startSlot + k

            // Bounds check: ensure we don't access beyond the carbon intensity array
            if (slot >= carbonIntensity.size) {
                break // Task extends beyond horizon, stop calculating
            }

            val usedInSlot = min(remaining, slotLengthMs)
            val fraction = usedInSlot.toDouble() / slotLengthMs.toDouble()

            cost += carbonIntensity[slot] * fraction
            remaining -= usedInSlot
        }

        return cost
    }
}

