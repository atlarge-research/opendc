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

package org.opendc.compute.simulator.scheduler.timeshift

import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.SchedulingRequest
import org.opendc.compute.simulator.scheduler.SchedulingResult
import org.opendc.compute.simulator.scheduler.SchedulingResultType
import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.CarbonReceiver
import java.time.Instant
import java.time.InstantSource
import java.util.LinkedList
import java.util.SplittableRandom
import java.util.random.RandomGenerator
import kotlin.math.min
import kotlin.math.roundToInt

public class TimeshiftScheduler(
    private val filters: List<HostFilter>,
    private val weighers: List<HostWeigher>,
    private val windowSize: Int,
    private val clock: InstantSource,
    private val subsetSize: Int = 1,
    private val forecast: Boolean = true,
    private val shortForecastThreshold: Double = 0.2,
    private val longForecastThreshold: Double = 0.35,
    private val forecastSize: Int = 24,
    private val random: RandomGenerator = SplittableRandom(0),
) : ComputeScheduler, CarbonReceiver {
    /**
     * The pool of hosts available to the scheduler.
     */
    private val hosts = mutableListOf<HostView>()

    init {
        require(subsetSize >= 1) { "Subset size must be one or greater" }
    }

    private val pastCarbonIntensities = LinkedList<Double>()
    private var carbonRunningSum = 0.0
    private var shortLowCarbon = false // Low carbon regime for short tasks (< 2 hours)
    private var longLowCarbon = false // Low carbon regime for long tasks (>= hours)
    private var carbonModel: CarbonModel? = null

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

            /**
             If we are not in a low carbon regime, delay tasks.
             Only delay tasks if they are deferrable and it doesn't violate the deadline.
             Separate delay thresholds for short and long tasks.
             */
            if (task.nature.deferrable) {
                val durInHours = task.duration.toHours()
                if ((durInHours < 2 && !shortLowCarbon) ||
                    (durInHours >= 2 && !longLowCarbon)
                ) {
                    val currentTime = clock.instant()
                    val estimatedCompletion = currentTime.plus(task.duration)
                    val deadline = Instant.ofEpochMilli(task.deadline)
                    if (estimatedCompletion.isBefore(deadline)) {
                        // No need to schedule this task in a high carbon intensity period
                        continue
                    }
                }
            }

            val filteredHosts = hosts.filter { host -> filters.all { filter -> filter.test(host, task) } }

            val subset =
                if (weighers.isNotEmpty()) {
                    val filterResults = weighers.map { it.getWeights(filteredHosts, task) }
                    val weights = DoubleArray(filteredHosts.size)

                    for (fr in filterResults) {
                        val min = fr.min
                        val range = (fr.max - min)

                        // Skip result if all weights are the same
                        if (range == 0.0) {
                            continue
                        }

                        val multiplier = fr.multiplier
                        val factor = multiplier / range

                        for ((i, weight) in fr.weights.withIndex()) {
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

    /**
     Compare current carbon intensity to the chosen quantile from the [forecastSize]
     number of intensity forecasts
     */
    override fun updateCarbonIntensity(newCarbonIntensity: Double) {
        if (!forecast) {
            noForecastUpdateCarbonIntensity(newCarbonIntensity)
            return
        }

        val forecast = carbonModel!!.getForecast(forecastSize)
        val forecastSize = forecast.size
        val shortQuantileIndex = (forecastSize * shortForecastThreshold).roundToInt()
        val shortCarbonIntensity = forecast.sorted()[shortQuantileIndex]
        val longQuantileIndex = (forecastSize * longForecastThreshold).roundToInt()
        val longCarbonIntensity = forecast.sorted()[longQuantileIndex]

        shortLowCarbon = newCarbonIntensity < shortCarbonIntensity
        longLowCarbon = newCarbonIntensity < longCarbonIntensity
    }

    /**
     Compare current carbon intensity to the moving average of the past [windowSize]
     number of intensity updates
     */
    private fun noForecastUpdateCarbonIntensity(newCarbonIntensity: Double) {
        val previousCarbonIntensity =
            if (this.pastCarbonIntensities.isEmpty()) {
                0.0
            } else {
                this.pastCarbonIntensities.last()
            }
        this.pastCarbonIntensities.addLast(newCarbonIntensity)
        this.carbonRunningSum += newCarbonIntensity
        if (this.pastCarbonIntensities.size > this.windowSize) {
            this.carbonRunningSum -= this.pastCarbonIntensities.removeFirst()
        }

        val thresholdCarbonIntensity = this.carbonRunningSum / this.pastCarbonIntensities.size

        shortLowCarbon = (newCarbonIntensity < thresholdCarbonIntensity) &&
            (newCarbonIntensity > previousCarbonIntensity)
        longLowCarbon = (newCarbonIntensity < thresholdCarbonIntensity)
    }

    override fun setCarbonModel(carbonModel: CarbonModel?) {
        this.carbonModel = carbonModel
    }

    override fun removeCarbonModel(carbonModel: CarbonModel?) {}
}
