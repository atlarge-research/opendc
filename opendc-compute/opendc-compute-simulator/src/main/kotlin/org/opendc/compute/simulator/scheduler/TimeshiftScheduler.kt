package org.opendc.compute.simulator.scheduler

import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.CarbonReceiver
import java.time.Instant
import java.time.InstantSource
import java.util.Collections
import java.util.LinkedList
import java.util.SplittableRandom
import java.util.random.RandomGenerator
import kotlin.math.min


public class TimeshiftScheduler(
    private val filters: List<HostFilter>,
    private val weighers: List<HostWeigher>,
    private val windowSize: Int,
    private val clock: InstantSource,
    private val subsetSize: Int = 1,
    private val random: RandomGenerator = SplittableRandom(0)
) : ComputeScheduler, CarbonReceiver  {
    /**
     * The pool of hosts available to the scheduler.
     */
    private val hosts = mutableListOf<HostView>()

    init {
        require(subsetSize >= 1) { "Subset size must be one or greater" }
    }

    private val pastCarbonIntensities = LinkedList<Double>()
    private var carbonRunningSum = 0.0

    private var carbonIntensity = 0.0
//    private var lowerCarbonIntensity = 0.0
    private var thresholdCarbonIntensity = 0.0

    override fun addHost(host: HostView) {
        hosts.add(host)
    }

    override fun removeHost(host: HostView) {
        hosts.remove(host)
    }

    override fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult {
        var result: SchedulingResult? = null
        for (req in iter) {
            if (req.isCancelled) {
                iter.remove()
                continue
            }

            val task = req.task

            if (carbonIntensity > thresholdCarbonIntensity) {
                if (task.nature.deferrable) {
                    val currentTime = clock.instant()
                    val estimatedCompletion = currentTime.plus(task.duration)
                    val deadline = Instant.ofEpochMilli(task.deadline)
                    if (estimatedCompletion.isBefore(deadline)) {
                        // No need to schedule this task in a high carbon intensity period
                        continue;
                    }
                }
            }

            val filteredHosts = hosts.filter { host -> filters.all { filter -> filter.test(host, task) } }

            val subset =
                if (weighers.isNotEmpty()) {
                    val results = weighers.map { it.getWeights(filteredHosts, task) }
                    val weights = DoubleArray(filteredHosts.size)

                    for (result in results) {
                        val min = result.min
                        val range = (result.max - min)

                        // Skip result if all weights are the same
                        if (range == 0.0) {
                            continue
                        }

                        val multiplier = result.multiplier
                        val factor = multiplier / range

                        for ((i, weight) in result.weights.withIndex()) {
                            weights[i] += factor * (weight - min)
                        }
                    }

                    weights.indices
                        .asSequence()
                        .sortedByDescending { weights[it] }
                        .map { filteredHosts[it] }
                        .take(subsetSize)
                        .toList()
                } else {
                    filteredHosts
                }

            val maxSize = min(subsetSize, subset.size)
            if (maxSize == 0) {
                result = SchedulingResult(SchedulingResultType.FAILURE, null, req)
                break
            } else {
                iter.remove()
                result = SchedulingResult(SchedulingResultType.SUCCESS, subset[random.nextInt(maxSize)], req)
                break
            }
        }

        if (result == null) return SchedulingResult(SchedulingResultType.EMPTY)

        return result
    }

    override fun removeTask(
        task: ServiceTask,
        host: HostView?,
    ) {}

    override fun updateCarbonIntensity(newCarbonIntensity: Double) {
        this.carbonIntensity = newCarbonIntensity
        this.pastCarbonIntensities.addLast(newCarbonIntensity)
        this.carbonRunningSum += newCarbonIntensity
        if (this.pastCarbonIntensities.size > this.windowSize) {
            this.carbonRunningSum -= this.pastCarbonIntensities.removeFirst()
        }
        this.thresholdCarbonIntensity = this.carbonRunningSum / this.pastCarbonIntensities.size
    }

    private fun getPositionalValue(list: ArrayList<Double>, position: Double): Double {
        if (list.size == 1) {
            return list[0]
        }

        Collections.sort(list)
        val size = list.size

        val middle_index = size * position
        return if (middle_index % 1 == 0.0) {
            list[middle_index.toInt()]
        } else {
            (list[middle_index.toInt()] + list[middle_index.toInt() + 1]) / 2.0
        }
    }

    override fun setCarbonModel(carbonModel: CarbonModel?) {}

    override fun removeCarbonModel(carbonModel: CarbonModel?) {}
}
