package org.opendc.compute.simulator.scheduler

import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import java.time.InstantSource
import java.util.SplittableRandom
import java.util.random.RandomGenerator
import org.opendc.common.logger.logger
import kotlin.math.abs

// Emily's version

public class WorkflowAwareScheduler(
    filters: List<HostFilter>,
    weighers: List<HostWeigher>,
    private val enableDeadlineScore: Boolean,
    private val weightUrgency: Double = 0.2,
    private val weightCriticalDependencyChain: Double = 0.2,
    private val enableParallelismScore: Boolean = false,
    private val weightParallelism: Double = 0.0,
    private val parallelismDecayRate: Double = 0.15,
    private val clock: InstantSource,
    subsetSize: Int = 1,
    random: RandomGenerator = SplittableRandom(0),
    numHosts: Int = 1000,
    private val taskLookaheadThreshold: Int = 1000,
) : FilterScheduler(filters, weighers, subsetSize, random, numHosts) {

    private val initTime: Long = clock.millis()

    @Volatile
    private var simToEpochOffset: Long? = null

    // Debug controls
    private var pickedCount: Int = 0

    private companion object {
        val LOG by logger()
    }

    override fun selectTask(iter: MutableIterator<SchedulingRequest>): SchedulingRequest? {
        val listIter = iter as ListIterator<SchedulingRequest>

        // Count ready tasks
        var readyCount = 0
        while (listIter.hasNext()) {
            val req = listIter.next()
            if (!req.isCancelled) readyCount++
        }
        while (listIter.hasPrevious()) listIter.previous()

        var highestPriorityIndex = -1
        var highestPriorityScore = Double.NEGATIVE_INFINITY
        var count = 0

        // Lookahead
        while (listIter.hasNext() && count < taskLookaheadThreshold) {
            val currentIndex = listIter.nextIndex()
            val req = listIter.next()
            if (req.isCancelled) continue

            // Initialize offset once
            if (simToEpochOffset == null) {
                val firstSubmitEpoch = req.task.getService().firstTaskSubmittedAt.toLong()
                simToEpochOffset = firstSubmitEpoch - initTime
            }

            val nowEpoch = clock.millis() + simToEpochOffset!!
            val score = calculatePriorityScore(req, nowEpoch, readyCount)

            if (score > highestPriorityScore) {
                highestPriorityScore = score
                highestPriorityIndex = currentIndex
            }

            count++
        }

        // Reset iterator
        while (listIter.hasPrevious()) listIter.previous()

        if (highestPriorityIndex == -1) return null

        // Move to selected task
        while (listIter.nextIndex() < highestPriorityIndex) {
            listIter.next()
        }

        val selected = listIter.next()

        return selected
    }

    private fun calculatePriorityScore(
        req: SchedulingRequest,
        currentTimeEpoch: Long,
        readyCount: Int
    ): Double {
        val task = req.task
        val parallelismActive = enableParallelismScore && readyCount < taskLookaheadThreshold
        val deadlineEpoch = task.getSubmittedAt() + abs(task.deadline)

        val slack =
            if (enableDeadlineScore) {
                deadlineEpoch - currentTimeEpoch
            } else {
                0L
            }

        val urgencyScore =
            if (enableDeadlineScore) {
                when {
                    // TODO: how to prioritize among overdue tasks
                    slack < 0L -> Double.MAX_VALUE
                    slack > 0L -> 1.0 / slack.toDouble()
                    else -> Double.MAX_VALUE
                }
            } else {
                0.0
            }

        // Compute chain score based on critical dependency chain length
        val chainLength = task.calcMaxDependencyChainLength()
        val chainScore = chainLength.toDouble()

        // Enable parallelism scoring only when the ready pool is small
        val parallelismScore =
            if (parallelismActive) {
                task.calculateParallelismScore(parallelismDecayRate)
            } else {
                0.0
            }

        // Weighted sum of urgency and chain scores
        val weightedUrgency = weightUrgency * urgencyScore
        val weightedChain = weightCriticalDependencyChain * chainScore
        val weightedParallelism = weightParallelism * parallelismScore

        val finalScore = weightedUrgency + weightedChain + weightedParallelism

        return finalScore
    }
}


//    package org.opendc.compute.simulator.scheduler
//
//    import org.opendc.compute.simulator.service.HostView
//    import org.opendc.compute.simulator.service.ServiceTask
//    import org.opendc.compute.simulator.scheduler.filters.HostFilter
//    import org.opendc.compute.simulator.scheduler.weights.HostWeigher
//    import java.time.InstantSource
//    import java.util.SplittableRandom
//    import java.util.random.RandomGenerator
//    import org.opendc.common.logger.errAndNull
//    import org.opendc.common.logger.logger
//
//    public class WorkflowAwareScheduler(
//        filters: List<HostFilter>,
//        weighers: List<HostWeigher>,
//        private val enableDeadlineScore: Boolean,
//        private val weightUrgency: Double = 0.2,
//        private val weightCriticalDependencyChain: Double = 0.2,
//        private val enableParallelismScore: Boolean = false,
//        private val weightParallelism: Double = 0.0,
//        private val parallelismDecayRate: Double = 0.15,
//        private val clock: InstantSource,
//        subsetSize: Int = 1,
//        random: RandomGenerator = SplittableRandom(0),
//        numHosts: Int = 1000,
//        private val taskLookaheadThreshold: Int = 1000,
//    ) : FilterScheduler(filters, weighers, subsetSize, random, numHosts) {
//
//        private val initTime: Long
//
//        init {
//            // Record scheduler initialization time
//            initTime = clock.millis()
//        }
//
//        override fun selectTask(iter: MutableIterator<SchedulingRequest>): SchedulingRequest? {
//            val currentTime = clock.millis()
//
//            var simulationOffsetCurrentTime: Long? = null
//            val listIter = iter as ListIterator<SchedulingRequest>
//
//            // Count ready tasks
//            var readyCount = 0
//            while (listIter.hasNext()) {
//                val req = listIter.next()
//                if (!req.isCancelled) readyCount++
//            }
//            while (listIter.hasPrevious()) listIter.previous()
//
//            var highestPriorityIndex = -1
//            var highestPriorityScore = Double.NEGATIVE_INFINITY
//            var count = 0
//
//            // --- Lookahead phase: evaluate up to 'taskLookaheadThreshold' tasks ---
//            while (listIter.hasNext() && count < taskLookaheadThreshold) {
//                val currentIndex = listIter.nextIndex()
//                val req = listIter.next()
//
//                // Initialize simulation offset based on first submitted task
//                if (simulationOffsetCurrentTime == null) {
//                    val firstSubmit = req.task.getService().firstTaskSubmittedAt.toLong()
//                    val simulationOffset = firstSubmit - initTime
//                    simulationOffsetCurrentTime = currentTime + simulationOffset
//                }
//
//                if (req.isCancelled) continue  // Skip cancelled tasks
//
//                // Calculate task priority score
//                val score = calculatePriorityScore(req, simulationOffsetCurrentTime!!, readyCount)
//
//                // Track the task with the highest score
//                if (score > highestPriorityScore) {
//                    highestPriorityScore = score
//                    highestPriorityIndex = currentIndex
//                }
//
//                count++
//            }
//
//            // Reset iterator back to the beginning
//            while (listIter.hasPrevious()) {
//                listIter.previous()
//            }
//
//            if (highestPriorityIndex == -1) return null  // No valid task found
//
//            // --- Navigation phase: move iterator to highest priority task ---
//            while (listIter.nextIndex() < highestPriorityIndex) {
//                listIter.next()
//            }
//
//            val selected = listIter.next()  // Return selected task
//            return selected
//        }
//
//        private fun calculatePriorityScore(req: SchedulingRequest, currentTime: Long, readyCount: Int): Double {
//            val task = req.task
//
//            // Compute urgency score based on task deadline
//            val urgencyScore =
//                if (this.enableDeadlineScore) {
//                    val slack = task.deadline + task.getSubmittedAt() - currentTime
//
//                    if (slack < 0L) {
//                        // TODO: how to prioritze among overdue tasks
//                        Double.MAX_VALUE  // 1_000_000.0 + (1.0 / abs(slack.toDouble()))
//                    } else if (slack > 0L) {
//                        1.0 / slack.toDouble()
//                    } else {
//                        Double.MAX_VALUE
//                    }
//                } else {
//                    0.0
//                }
//
//            // Compute chain score based on critical dependency chain length
//            val chainLength = task.calcMaxDependencyChainLength()
//            val chainScore = chainLength.toDouble()
//
//            // Enable parallelism scoring only when the ready pool is small
//            val parallelismScore =
//                if (this.enableParallelismScore && readyCount < this.taskLookaheadThreshold) {
//                    val calculatedParallelism = task.calculateParallelismScore(this.parallelismDecayRate)
//                    calculatedParallelism
//                } else {
//                    0.0
//                }
//
//            // Weighted sum of urgency and chain scores
//
//            val weightedUrgency = this.weightUrgency * urgencyScore
//            val weightedChain = this.weightCriticalDependencyChain * chainScore
//            val weightedParallelism = this.weightParallelism * parallelismScore
//
//            val finalScore = weightedUrgency + weightedChain + weightedParallelism
//
//            return finalScore
//        }
//
//        private companion object {
//            val LOG by logger()
//        }
//    }



        // package org.opendc.compute.simulator.scheduler

        // import org.opendc.compute.simulator.service.HostView
        // import org.opendc.compute.simulator.service.ServiceTask
        // import org.opendc.compute.simulator.scheduler.filters.HostFilter
        // import org.opendc.compute.simulator.scheduler.weights.HostWeigher
        // import java.time.InstantSource
        // import java.util.SplittableRandom
        // import java.util.random.RandomGenerator



        // public class WorkflowAwareScheduler(
        //     filters: List<HostFilter>,
        //     weighers: List<HostWeigher>,
        //     private val taskDeadlineScore: Boolean,
        //     private val weightUrgency: Double = 0.2,
        //     private val weightCriticalDependencyChain: Double = 0.2,
        //     private val clock: InstantSource,
        //     subsetSize: Int = 1,
        //     random: RandomGenerator = SplittableRandom(0),
        //     numHosts: Int = 1000,
        //     private val taskLookaheadThreshold: Int = 1000,
        // ) : FilterScheduler(filters, weighers, subsetSize, random, numHosts) {

        //     private val initTime: Long

        //     init {
        //         initTime = clock.millis()
        //     }

        //     override fun selectTask(iter: MutableIterator<SchedulingRequest>): SchedulingRequest? {
        //         val currentTime = clock.millis()
        //         LOG.errAndNull("[selectTask] currentTime: $currentTime")



        //         var simulationOffsetCurrentTime: Long? = null


        //         val listIter = iter as ListIterator<SchedulingRequest>

        //         // --- Debug: log full list state before scoring ---
        //         LOG.errAndNull("[selectTask] BEGIN ITERATION DUMP")
        //         val snapshot = mutableListOf<SchedulingRequest>()
        //         while (listIter.hasNext()) {
        //             snapshot.add(listIter.next())
        //         }
        //         LOG.errAndNull("[selectTask] total-requests=${snapshot.size}")
        //         snapshot.forEachIndexed { idx, req ->
        //             LOG.errAndNull("[selectTask] idx=$idx cancelled=${req.isCancelled} task=${req.task}")
        //         }
        //         // Reset iterator to beginning
        //         while (listIter.hasPrevious()) listIter.previous()
        //         LOG.errAndNull("[selectTask] END ITERATION DUMP")

        //         var highestPriorityIndex = -1
        //         var highestPriorityScore = Double.MIN_VALUE
        //         val scoreConstant = Double.MIN_VALUE + Double.MIN_VALUE
        //         var count = 0

        //         // --- Lookahead phase ---
        //         LOG.errAndNull("[selectTask] Starting lookahead up to $taskLookaheadThreshold tasks")

        //         while (listIter.hasNext() && count < taskLookaheadThreshold) {
        //             val currentIndex = listIter.nextIndex()
        //             val req = listIter.next()

        //             if (simulationOffsetCurrentTime == null) {
        //                 val firstSubmit = req.task.getService().firstTaskSubmittedAt.toLong()
        //                 val simulationOffset = firstSubmit - initTime
        //                 simulationOffsetCurrentTime = currentTime + simulationOffset

        //                 LOG.errAndNull(
        //                 """
        //                 [selectTask] INITIALIZING SIMULATION OFFSET
        //                 | initTime                = $initTime
        //                 | currentTime             = $currentTime
        //                 | firstTaskSubmittedAt    = $firstSubmit
        //                 | simulationOffset        = $simulationOffset
        //                 | simulationOffsetCurrent = $simulationOffsetCurrentTime
        //                 """.trimMargin()
        //             )

        //             }

        //             LOG.errAndNull("[selectTask] Scoring idx=$currentIndex cancelled=${req.isCancelled} task=${req.task}")

        //             if (req.isCancelled) {
        //                 LOG.errAndNull("[selectTask]   -> Skipped cancelled task at $currentIndex")
        //                 continue
        //             }

        //             val score = calculatePriorityScore(req, simulationOffsetCurrentTime!!) + scoreConstant
        //             .errAndNull("[selectTask]   -> Score[$currentIndex] = $score")

        //             if (score > highestPriorityScore) {
        //                 LOG.errAndNull("[selectTask]   -> New best score! idx=$currentIndex score=$score")
        //                 highestPriorityScore = score
        //                 highestPriorityIndex = currentIndex
        //             }

        //             count++
        //         }

        //         // Reset listIter back to start of list before selecting
        //         while (listIter.hasPrevious()) {
        //             listIter.previous()
        //         }

        //         LOG.errAndNull("[selectTask] Lookahead finished. BestIndex=$highestPriorityIndex BestScore=$highestPriorityScore")

        //         if (highestPriorityIndex == -1) {
        //             LOG.errAndNull("[selectTask] No non-cancelled tasks found. Returning null.")
        //             return null
        //         }

        //         // --- Navigation back phase ---
        //         LOG.errAndNull("[selectTask] Navigating to highestPriorityIndex=$highestPriorityIndex")
        //         while (listIter.nextIndex() < highestPriorityIndex) {
        //             val moved = listIter.nextIndex()
        //             listIter.next()
        //             LOG.errAndNull("[selectTask]   -> moved forward to idx $moved")
        //         }

        //         val selected = listIter.next()
        //         LOG.errAndNull("[selectTask] SELECTED TASK = ${selected.task}")

        //         return selected
        //     }

        //     private fun calculatePriorityScore(req: SchedulingRequest, currentTime: Long): Double {
        //         val task = req.task
        //         LOG.errAndNull("[prio] scoring task=${task.getId()} deadline=${task.getDeadline()} currTime=$currentTime")

        //         var urgencyScore = 0.0

        //         if (this.taskDeadlineScore) {
        //             val slack = task.deadline - currentTime
        //             LOG.errAndNull("[prio]   slack = deadline - currentTime = $slack")

        //             urgencyScore = if (slack != 0L) {
        //                 1.0 / slack.toDouble()
        //             } else {
        //                 LOG.errAndNull("[prio] slack = 0 → using MAX_VALUE")
        //                 Double.MAX_VALUE
        //             }

        //             LOG.errAndNull("[prio]   urgencyScore = $urgencyScore")
        //         } else {
        //             LOG.errAndNull("[prio]   urgency disabled")
        //         }

        //         val chainLength = task.calcMaxDependencyChainLength()
        //         LOG.errAndNull("[prio]   chainLength = $chainLength")

        //         val chainScore = chainLength.toDouble()
        //         val result = (this.weightUrgency * urgencyScore) + (this.weightCriticalDependencyChain * chainScore)

        //         LOG.errAndNull("[prio]   finalScore = ($weightUrgency * $urgencyScore) + ($weightCriticalDependencyChain * $chainScore) = $result")

        //         return result
        //     }

        //     private companion object {
        //         val LOG by logger()
        //     }
        // }



                // Calculate urgency: how close we are to the deadline
                // val waitTime = currentTime - req.submitTime
                // val timeUntilDeadline = task.deadline - currentTime
                // val urgencyScore = if (timeUntilDeadline > 0) {
                //     waitTime.toDouble() / (waitTime + timeUntilDeadline)
                // } else {
                //     1.0  // Overdue = maximum urgency
                // }
