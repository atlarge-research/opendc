package org.opendc.compute.simulator.scheduler

import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.scheduler.timeshift.Timeshifter
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import org.opendc.simulator.compute.power.CarbonModel
import java.time.InstantSource
import java.util.LinkedList
import java.util.SplittableRandom
import java.util.random.RandomGenerator

/**
 * A scheduler that combines workflow-aware task prioritization with timeshift (carbon-aware) scheduling.
 *
 * This scheduler:
 * 1. Uses workflow information (deadlines, dependency chains) to prioritize tasks
 * 2. Delays deferrable tasks during high carbon intensity periods (timeshifting)
 * 3. Selects hosts using the standard FilterScheduler approach
 *
 * @param filters List of host filters to apply
 * @param weighers List of host weighers for scoring hosts
 * @param taskDeadlineScore Whether to include deadline urgency in task prioritization
 * @param weightUrgency Weight for deadline urgency score
 * @param weightCriticalDependencyChain Weight for dependency chain length score
 * @param weightCarbonImpact Weight for carbon impact in task selection
 * @param clock Clock for tracking time
 * @param windowSize Size of the moving window for carbon intensity averaging
 * @param forecast Whether to use carbon intensity forecasting
 * @param shortForecastThreshold Quantile threshold for short task carbon decisions
 * @param longForecastThreshold Quantile threshold for long task carbon decisions
 * @param forecastSize Number of forecast points to consider
 * @param subsetSize Number of hosts to consider after weighing
 * @param random Random number generator
 * @param numHosts Expected number of hosts
 * @param taskLookaheadThreshold Maximum number of tasks to evaluate for prioritization
 */
public class WorkflowAwareTimeshiftScheduler(
    filters: List<HostFilter>,
    weighers: List<HostWeigher>,
    private val taskDeadlineScore: Boolean,
    private val weightUrgency: Double = 0.3,
    private val weightCriticalDependencyChain: Double = 0.4,
    private val weightCarbonImpact: Double = 0.3,
    override val clock: InstantSource,
    override val windowSize: Int = 168,
    override val forecast: Boolean = true,
    override val shortForecastThreshold: Double = 0.2,
    override val longForecastThreshold: Double = 0.35,
    override val forecastSize: Int = 24,
    subsetSize: Int = 1,
    random: RandomGenerator = SplittableRandom(0),
    numHosts: Int = 1000,
    private val taskLookaheadThreshold: Int = 1000,
) : FilterScheduler(filters, weighers, subsetSize, random, numHosts), Timeshifter {

    private val initTime: Long

    init {
        // Record scheduler initialization time
        initTime = clock.millis()
    }

    // Timeshifter interface implementation
    override val pastCarbonIntensities: LinkedList<Double> = LinkedList()
    override var carbonRunningSum: Double = 0.0
    override var shortLowCarbon: Boolean = false
    override var longLowCarbon: Boolean = false
    override var carbonMod: CarbonModel? = null

    override fun selectTask(iter: MutableIterator<SchedulingRequest>): SchedulingRequest? {
        val currentTime = clock.millis()
        var simulationOffsetCurrentTime: Long? = null

        @Suppress("UNCHECKED_CAST")
        val listIter = iter as ListIterator<SchedulingRequest>

        var highestPriorityIndex = -1
        var highestPriorityScore = Double.NEGATIVE_INFINITY
        var count = 0

        // --- Lookahead phase: evaluate up to 'taskLookaheadThreshold' tasks ---
        while (listIter.hasNext() && count < taskLookaheadThreshold) {
            val currentIndex = listIter.nextIndex()
            val req = listIter.next()

            // Initialize simulation offset based on first submitted task
            if (simulationOffsetCurrentTime == null) {
                val firstSubmit = req.task.getService().firstTaskSubmittedAt.toLong()
                val simulationOffset = firstSubmit - initTime
                simulationOffsetCurrentTime = currentTime + simulationOffset
            }

            if (req.isCancelled) continue  // Skip cancelled tasks

            val task = req.task

            // Carbon-aware delay logic: skip deferrable tasks during high carbon periods
            if (task.deferrable) {
                val durInHours = task.duration / (1000.0 * 60.0 * 60.0)
                val isShortTask = durInHours < 2
                val shouldDelay = if (isShortTask) !shortLowCarbon else !longLowCarbon

                if (shouldDelay) {
                    val estimatedCompletion = simulationOffsetCurrentTime + task.duration
                    val deadline = task.deadline
                    // Only delay if we won't violate the deadline
                    if (estimatedCompletion < deadline) {
                        continue
                    }
                }
            }

            // Calculate task priority score (workflow-aware + carbon-aware)
            val score = calculatePriorityScore(req, simulationOffsetCurrentTime)

            // Track the task with the highest score
            if (score > highestPriorityScore) {
                highestPriorityScore = score
                highestPriorityIndex = currentIndex
            }

            count++
        }

        // Reset iterator back to the beginning
        while (listIter.hasPrevious()) {
            listIter.previous()
        }

        if (highestPriorityIndex == -1) return null  // No valid task found

        // --- Navigation phase: move iterator to highest priority task ---
        while (listIter.nextIndex() < highestPriorityIndex) {
            listIter.next()
        }

        val selected = listIter.next()  // Return selected task
        println(selected.task.id)
        return selected
    }

    private fun calculatePriorityScore(req: SchedulingRequest, currentTime: Long): Double {
        val task = req.task

        // 1. Compute urgency score based on task deadline
        var urgencyScore = 0.0
        if (this.taskDeadlineScore) {
            val slack = task.deadline - currentTime
            urgencyScore = if (slack > 0) 1.0 / slack.toDouble() else Double.MAX_VALUE
        }

        // 2. Compute chain score based on critical dependency chain length
        val chainLength = task.calcMaxDependencyChainLength()
        val chainScore = chainLength.toDouble()

        // 3. Compute carbon impact score (favor scheduling during low carbon periods)
        val carbonScore = if (shortLowCarbon || longLowCarbon) 1.0 else 0.0

        // Weighted sum of all scores
        return (this.weightUrgency * urgencyScore) +
               (this.weightCriticalDependencyChain * chainScore) +
               (this.weightCarbonImpact * carbonScore)
    }
}


