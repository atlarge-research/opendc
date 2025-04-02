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

import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.CarbonReceiver
import java.time.InstantSource
import java.util.LinkedList
import kotlin.math.roundToInt

public interface Timeshifter : CarbonReceiver {
    public val windowSize: Int
    public val clock: InstantSource
    public val forecast: Boolean
    public val shortForecastThreshold: Double
    public val longForecastThreshold: Double
    public val forecastSize: Int

    public val pastCarbonIntensities: LinkedList<Double>
    public var carbonRunningSum: Double
    public var shortLowCarbon: Boolean // Low carbon regime for short tasks (< 2 hours)
    public var longLowCarbon: Boolean // Low carbon regime for long tasks (>= hours)
    public var carbonMod: CarbonModel?

    /**
     Compare current carbon intensity to the chosen quantile from the [forecastSize]
     number of intensity forecasts
     */
    override fun updateCarbonIntensity(newCarbonIntensity: Double) {
        if (!forecast) {
            noForecastUpdateCarbonIntensity(newCarbonIntensity)
            return
        }

        val forecast = carbonMod!!.getForecast(forecastSize)
        val localForecastSize = forecast.size

        val shortQuantileIndex = (localForecastSize * shortForecastThreshold).roundToInt()
        val shortCarbonIntensity = forecast.sorted()[shortQuantileIndex]
        val longQuantileIndex = (localForecastSize * longForecastThreshold).roundToInt()
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
        this.carbonMod = carbonModel
    }

    override fun removeCarbonModel(carbonModel: CarbonModel?) {}
}
