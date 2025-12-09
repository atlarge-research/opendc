package org.opendc.compute.simulator.scheduler

import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.scheduler.carbonaware.CarbonOptimizer
import org.opendc.compute.simulator.scheduler.carbonaware.CarbonScheduleState
import org.opendc.compute.simulator.scheduler.carbonaware.TopologicalSort
import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.scheduler.timeshift.Timeshifter
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.power.CarbonModel
import java.time.InstantSource
import java.util.LinkedList
import java.util.SplittableRandom
import java.util.random.RandomGenerator

/**
 * A scheduler that optimizes task placement to minimize carbon emissions
 * using depth-first search with branch-and-bound.
 *
 * This scheduler analyzes the entire workflow and uses a global optimization
 * approach to find the carbon-minimal schedule while respecting task dependencies
 * and resource constraints.
 *
 * @param filters List of host filters to apply
 * @param weighers List of host weighers for scoring hosts
 * @param clock Clock for tracking time
 * @param slotLengthMs Duration of each time slot in milliseconds (default: 1 hour)
 * @param horizonSlots Number of time slots in planning horizon (default: 1 week)
 * @param enableOptimization Enable/disable DFS optimization (default: true)
 * @param batchSize Number of tasks to optimize together in each batch (default: 100)
 * @param maxSlotsToTry Maximum number of time slots to try per task in optimizer (default: 5)
 * @param searchWindowSize How many slots ahead from earliest to search for low-carbon slots (default: 12)
 * @param subsetSize Number of hosts to consider after weighing
 * @param random Random number generator
 * @param numHosts Expected number of hosts
 */
public class CarbonAwareWorkflowScheduler(
    filters: List<HostFilter>,
    weighers: List<HostWeigher>,
    override val clock: InstantSource,
    private val slotLengthMs: Long = 3_600_000L, // 1 hour default
    private val horizonSlots: Int = 168, // 1 week default
    private val enableOptimization: Boolean = true,
    private val batchSize: Int = 100, // Optimize N tasks at a time
    private val maxSlotsToTry: Int = 5, // Try up to 5 different time slots per task
    private val searchWindowSize: Int = 12, // Search 12 slots ahead from earliest
    subsetSize: Int = 1,
    random: RandomGenerator = SplittableRandom(0),
    private val numHosts: Int = 1000,
) : FilterScheduler(filters, weighers, subsetSize, random, numHosts),
    Timeshifter {
    // Timeshifter properties for carbon tracking
    override val windowSize: Int = horizonSlots
    override val forecast: Boolean = true
    override val shortForecastThreshold: Double = 0.2
    override val longForecastThreshold: Double = 0.35
    override val forecastSize: Int = 24
    override val pastCarbonIntensities: LinkedList<Double> = LinkedList()
    override var carbonRunningSum: Double = 0.0
    override var shortLowCarbon: Boolean = false
    override var longLowCarbon: Boolean = false
    override var carbonMod: CarbonModel? = null

    // Scheduling state
    private val scheduledTasks = mutableMapOf<Int, Int>() // taskId -> startSlot
    private var lastOptimizationTime: Long = 0
    private val optimizationInterval = slotLengthMs // Re-optimize every slot

    override fun selectTask(iter: MutableIterator<SchedulingRequest>): SchedulingRequest? {
        val currentTime = clock.millis()

        // Check if we should re-optimize
        // Run optimization if: enabled AND (never optimized OR enough time has passed)
        if (enableOptimization &&
            (lastOptimizationTime == 0L || currentTime - lastOptimizationTime >= optimizationInterval)
        ) {
            performOptimization(iter)
            lastOptimizationTime = currentTime
        }

        // Select the highest priority task that's ready to run
        return selectReadyTask(iter, currentTime)
    }

    /**
     * Perform global optimization of the task schedule.
     *
     * Optimizes only a small batch of tasks at a time to keep DFS fast.
     */
    private fun performOptimization(iter: MutableIterator<SchedulingRequest>) {
        // Collect all pending tasks
        val tasks = mutableListOf<SchedulingRequest>()
        @Suppress("UNCHECKED_CAST")
        val listIter = iter as ListIterator<SchedulingRequest>

        while (listIter.hasNext()) {
            val req = listIter.next()
            if (!req.isCancelled) {
                tasks.add(req)
            }
        }

        // Reset iterator
        while (listIter.hasPrevious()) {
            listIter.previous()
        }

        if (tasks.isEmpty()) return

        // Only optimize tasks whose dependencies are actually met
        val readyTasks = tasks.filter { req ->
            areDependenciesMet(req.task)
        }

        if (readyTasks.isEmpty()) {
            println("[CarbonAware] No ready tasks to optimize (all blocked by dependencies)")
            return
        }

        // Take a batch of ready tasks (configurable batch size for performance)
        val tasksToOptimize = readyTasks.take(batchSize)

        println("[CarbonAware] Optimizing batch of ${tasksToOptimize.size} ready tasks (out of ${tasks.size} total, ${readyTasks.size} ready)")

        // Build optimization state
        val state = buildScheduleState(tasksToOptimize)

        // Get carbon forecast
        val carbonForecast = getCarbonForecast()

        // Run optimization
        val optimizer = CarbonOptimizer(
            carbonIntensity = carbonForecast,
            maxParallelTasks = numHosts,
            maxSlotsToTry = maxSlotsToTry,
            searchWindowSize = searchWindowSize
        )
        optimizer.optimize(state)

        // Update scheduled tasks map with results
        if (state.bestCost < Double.POSITIVE_INFINITY) {
            var scheduled = 0
            for (i in 0 until state.taskCount) {
                val taskIdx = state.topoOrder[i]
                val req = tasksToOptimize[taskIdx]
                val startSlot = state.bestAssignment[taskIdx]

                if (startSlot >= 0) {
                    scheduledTasks[req.task.id] = startSlot
                    scheduled++
                }
            }
            println("[CarbonAware] Successfully scheduled $scheduled tasks in batch")
        } else {
            // Optimization failed - use greedy fallback
            val currentSlot = (clock.millis() / slotLengthMs).toInt()
            for (req in tasksToOptimize) {
                if (req.task.id !in scheduledTasks) {
                    scheduledTasks[req.task.id] = currentSlot
                }
            }
            println("[CarbonAware] Batch optimization failed - used greedy fallback")
        }
    }

    /**
     * Build the schedule state from the list of pending tasks.
     */
    private fun buildScheduleState(tasks: List<SchedulingRequest>): CarbonScheduleState {
        val n = tasks.size
        val state = CarbonScheduleState(n, horizonSlots, slotLengthMs)

        // Build task index mapping
        val taskIdToIndex =
            tasks.mapIndexed { idx, req ->
                req.task.id to idx
            }.toMap()

        // Build parent relationships (only parents within this batch)
        for ((idx, req) in tasks.withIndex()) {
            val parents = req.task.parents
            if (parents != null && parents.isNotEmpty()) {
                state.parents[idx] =
                    parents.mapNotNull {
                        taskIdToIndex[it]
                    }.toIntArray()
            }
        }

        // Perform topological sort
        TopologicalSort.sort(n, state.parents).copyInto(state.topoOrder)

        // Calculate durations and release slots
        val minSubmission = tasks.minOf { it.task.submittedAt }

        for ((idx, req) in tasks.withIndex()) {
            val task = req.task
            state.durationMs[idx] = task.duration
            state.durationSlots[idx] =
                ((task.duration + slotLengthMs - 1) / slotLengthMs).toInt()

            // Calculate base release slot from submission time
            val delta = task.submittedAt - minSubmission
            var releaseSlot =
                if (delta <= 0) {
                    0
                } else {
                    ((delta + slotLengthMs - 1) / slotLengthMs).toInt()
                }

            // Check if task has parents outside this batch that aren't complete yet
            // If so, we can't schedule it yet (set release slot very far in future)
            val allParents = task.wfParents
            if (allParents.isNotEmpty()) {
                // Check if there are parents not in this batch
                val hasExternalParents = allParents.any { parent ->
                    parent.id !in taskIdToIndex
                }

                if (hasExternalParents) {
                    // Check if all external parents are complete
                    val allExternalParentsComplete = allParents
                        .filter { parent -> parent.id !in taskIdToIndex }
                        .all { parent ->
                            parent.state == TaskState.COMPLETED ||
                            parent.state == TaskState.DELETED ||
                            parent.state == TaskState.TERMINATED
                        }

                    if (!allExternalParentsComplete) {
                        // Can't schedule this task yet - push it far into the future
                        // This will prevent the optimizer from scheduling it
                        releaseSlot = horizonSlots + 1000
                    }
                }
            }

            state.releaseSlot[idx] = releaseSlot

            // Extract deadline
            val deadline = task.deadline
            if (deadline > 0) {
                val deadlineFromStart = deadline - minSubmission
                state.deadlineSlot[idx] =
                    if (deadlineFromStart <= 0) {
                        0
                    } else {
                        (deadlineFromStart / slotLengthMs).toInt()
                    }
            } else {
                state.deadlineSlot[idx] = -1
            }
        }

        return state
    }

    /**
     * Get carbon intensity forecast for the planning horizon.
     */
    private fun getCarbonForecast(): DoubleArray {
        val forecast = carbonMod?.getForecast(horizonSlots)

        return if (forecast != null) {
            DoubleArray(forecast.size) { i -> forecast[i] }
        } else {
            DoubleArray(horizonSlots) { 100.0 }
        }
    }

    /**
     * Select the highest priority task that is ready to run now.
     */
    private fun selectReadyTask(
        iter: MutableIterator<SchedulingRequest>,
        currentTime: Long,
    ): SchedulingRequest? {
        val currentSlot = (currentTime / slotLengthMs).toInt()
        @Suppress("UNCHECKED_CAST")
        val listIter = iter as ListIterator<SchedulingRequest>

        var bestReq: SchedulingRequest? = null
        var bestPriority = Double.NEGATIVE_INFINITY
        var bestIsUnscheduled = false
        var bestIndex = -1
        var currentIndex = 0

        while (listIter.hasNext()) {
            val req = listIter.next()
            if (req.isCancelled) {
                currentIndex++
                continue
            }

            val scheduledSlot = this.scheduledTasks[req.task.id]

            if (scheduledSlot != null) {
                if (scheduledSlot <= currentSlot) {
                    if (areDependenciesMet(req.task)) {
                        val priority = calculatePriority(req)
                        if (!bestIsUnscheduled || priority > bestPriority) {
                            bestPriority = priority
                            bestReq = req
                            bestIndex = currentIndex
                            bestIsUnscheduled = false
                        }
                    }
                }
            } else {
                // Task not scheduled - use greedy fallback if dependencies met
                if (areDependenciesMet(req.task)) {
                    val priority = calculatePriority(req)
                    if (bestReq == null || (bestIsUnscheduled && priority > bestPriority)) {
                        bestPriority = priority
                        bestReq = req
                        bestIndex = currentIndex
                        bestIsUnscheduled = true
                    }
                }
            }
            currentIndex++
        }

        // Reset iterator to the beginning
        while (listIter.hasPrevious()) {
            listIter.previous()
        }

        // If we found a best task, position the iterator at that task
        // so that the parent class can remove it correctly
        if (bestReq != null) {
            repeat(bestIndex + 1) {
                listIter.next()
            }
        }

        return bestReq
    }

    /**
     * Check if all dependencies of a task are met.
     */
    private fun areDependenciesMet(task: ServiceTask): Boolean {
        val parents = task.wfParents
        if (parents.isEmpty()) return true

        // A dependency is met if parent is COMPLETED. We also check for DELETED or TERMINATED states because i think
        // Opendc just deletes them after a while.
        return parents.all {
            it.state == TaskState.COMPLETED ||
            it.state == TaskState.DELETED ||
            it.state == TaskState.TERMINATED
        }
    }

    /**
     * Calculate priority score for a task.
     * Higher priority = more urgent
     *
     * Priority is based on:
     * 1. Deadline urgency (if task has a deadline)
     * 2. Dependency chain length (longer chains get higher priority)
     */
    private fun calculatePriority(req: SchedulingRequest): Double {
        val currentTime = clock.millis()
        val task = req.task

        // Base priority from dependency chain length
        var priority = task.calcMaxDependencyChainLength().toDouble()

        // Add deadline urgency boost
        val deadline = task.deadline
        if (deadline > 0) {
            val timeUntilDeadline = deadline - currentTime

            if (timeUntilDeadline <= 0) {
                // Deadline passed - maximum priority
                priority += 1000000.0
            } else {
                // Calculate slack: time until deadline - task duration
                val slack = timeUntilDeadline - task.duration

                if (slack <= 0) {
                    // No slack - very high priority
                    priority += 100000.0
                } else {
                    // Boost priority inversely proportional to slack
                    // Tasks with less slack get higher priority
                    val slackHours = slack.toDouble() / 3_600_000.0 // Convert to hours
                    priority += 10000.0 / (slackHours + 1.0)
                }
            }
        }

        return priority
    }
}
