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

package org.opendc.compute.simulator.scheduler

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
    private val peakShift: Boolean = true,
    private val forecast: Boolean = true,
    private val forecastThreshold: Double = 0.35,
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
    private var isLowCarbon = false
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

            if (!isLowCarbon) {
                if (task.nature.deferrable) {
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

    override fun updateCarbonIntensity(newCarbonIntensity: Double) {
        if (!forecast) {
            noForecastUpdateCarbonIntensity(newCarbonIntensity)
            return
        }

        val forecast = carbonModel!!.getForecast(forecastSize)
        val forecastSize = forecast.size
        val quantileIndex = (forecastSize * forecastThreshold).roundToInt()
        val thresholdCarbonIntensity = forecast.sorted()[quantileIndex]

        isLowCarbon = newCarbonIntensity < thresholdCarbonIntensity
    }

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

        if (!peakShift) {
            isLowCarbon = newCarbonIntensity < thresholdCarbonIntensity
            return
        }

        isLowCarbon = (newCarbonIntensity < thresholdCarbonIntensity) &&
            (newCarbonIntensity > previousCarbonIntensity)
    }

    override fun setCarbonModel(carbonModel: CarbonModel?) {
        this.carbonModel = carbonModel
    }

    override fun removeCarbonModel(carbonModel: CarbonModel?) {}
}
