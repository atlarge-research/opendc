/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.simulator.compute.scheduler.timeshift

import org.opendc.simulator.compute.carbon.CarbonNode
import org.opendc.simulator.compute.carbon.CarbonReceiver
import org.opendc.simulator.compute.task.SimTask
import java.time.InstantSource
import kotlin.math.roundToInt

/**
 * A [Timeshifter] that delays deferrable tasks while the carbon intensity is high, as long as they can still meet
 * their deadline.
 *
 * Tasks shorter than two hours follow the short regime and longer tasks the long regime. With [forecast], the current
 * intensity is low for a regime when it is below the quantile [shortForecastThreshold] or [longForecastThreshold] of
 * the next [forecastSize] forecasted intensities. Without it, the intensity is low when it is below the moving average
 * of the past [windowSize] intensities; for short tasks it must also have risen since the previous update.
 *
 * @param clock The clock used to check deadlines.
 * @param windowSize The number of past intensities in the moving average.
 * @param forecast Whether to compare against the forecast instead of the moving average.
 * @param shortForecastThreshold The forecast quantile, between 0 and 1, below which intensity is low for short tasks.
 * @param longForecastThreshold The forecast quantile, between 0 and 1, below which intensity is low for long tasks.
 * @param forecastSize The number of forecasted intensities to compare against.
 */
public class CarbonTimeshifter(
    private val clock: InstantSource,
    private val windowSize: Int = 168,
    private val forecast: Boolean = true,
    private val shortForecastThreshold: Double = 0.2,
    private val longForecastThreshold: Double = 0.35,
    private val forecastSize: Int = 24,
) : Timeshifter, CarbonReceiver {
    private val pastCarbonIntensities = ArrayDeque<Double>()
    private var carbonRunningSum = 0.0

    /** Whether the carbon intensity is low for tasks shorter than two hours. */
    private var shortLowCarbon = false

    /** Whether the carbon intensity is low for tasks of two hours or longer. */
    private var longLowCarbon = false

    private var carbonNode: CarbonNode? = null

    override fun shouldDelay(task: SimTask): Boolean {
        if (!task.deferrable) {
            return false
        }

        val lowCarbon = if (task.duration < SHORT_TASK_MILLIS) shortLowCarbon else longLowCarbon
        return !lowCarbon && clock.millis() + task.duration < task.deadline
    }

    override fun updateCarbonIntensity(newCarbonIntensity: Double) {
        if (!forecast) {
            updateMovingAverage(newCarbonIntensity)
            return
        }

        val forecast = carbonNode!!.getForecast(forecastSize).sorted()
        val shortCarbonIntensity = forecast[(forecast.size * shortForecastThreshold).roundToInt()]
        val longCarbonIntensity = forecast[(forecast.size * longForecastThreshold).roundToInt()]

        shortLowCarbon = newCarbonIntensity < shortCarbonIntensity
        longLowCarbon = newCarbonIntensity < longCarbonIntensity
    }

    /** Compare [newCarbonIntensity] to the moving average of the past [windowSize] intensities. */
    private fun updateMovingAverage(newCarbonIntensity: Double) {
        val previousCarbonIntensity = pastCarbonIntensities.lastOrNull() ?: 0.0
        pastCarbonIntensities.addLast(newCarbonIntensity)
        carbonRunningSum += newCarbonIntensity
        if (pastCarbonIntensities.size > windowSize) {
            carbonRunningSum -= pastCarbonIntensities.removeFirst()
        }

        val thresholdCarbonIntensity = carbonRunningSum / pastCarbonIntensities.size

        shortLowCarbon = newCarbonIntensity < thresholdCarbonIntensity && newCarbonIntensity > previousCarbonIntensity
        longLowCarbon = newCarbonIntensity < thresholdCarbonIntensity
    }

    override fun setCarbonNode(carbonNode: CarbonNode?) {
        this.carbonNode = carbonNode
    }

    override fun removeCarbonNode(carbonNode: CarbonNode?) {}

    private companion object {
        /** Tasks shorter than this follow the short regime. */
        const val SHORT_TASK_MILLIS = 2 * 60 * 60 * 1000L
    }
}
