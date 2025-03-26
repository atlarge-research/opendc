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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.CarbonReceiver
import java.time.InstantSource
import java.util.LinkedList
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

public class TaskStopper(
    private val clock: InstantSource,
    context: CoroutineContext,
    private val forecast: Boolean = true,
    private val forecastThreshold: Double = 0.6,
    private val forecastSize: Int = 24,
    private val windowSize: Int = 168,
) : CarbonReceiver {
    private val scope: CoroutineScope = CoroutineScope(context + Job())

    private val pastCarbonIntensities = LinkedList<Double>()
    private var carbonRunningSum = 0.0
    private var isHighCarbon = false
    private var carbonModel: CarbonModel? = null

    private var service: ComputeService? = null
    private var client: ComputeService.ComputeClient? = null

    public fun setService(service: ComputeService) {
        this.service = service
        this.client = service.newClient()
    }

    private fun pauseTasks() {
        for (host in service!!.hosts) {
            val guests = host.getGuests()

            val snapshots =
                guests
                    .filter { it.state != TaskState.COMPLETED }
                    .map {
                        it.virtualMachine!!.makeSnapshot(clock.millis())
                        it.virtualMachine!!.snapshot
                    }
            val tasks = guests.map { it.task }
            host.pauseAllTasks()

            for ((task, snapshot) in tasks.zip(snapshots)) {
                client!!.rescheduleTask(task, snapshot)
            }
        }
    }

    override fun updateCarbonIntensity(newCarbonIntensity: Double) {
        if (!forecast) {
            isHighCarbon = noForecastUpdateCarbonIntensity(newCarbonIntensity)
        } else {
            val forecast = carbonModel!!.getForecast(forecastSize)
            val forecastSize = forecast.size
            val quantileIndex = (forecastSize * forecastThreshold).roundToInt()
            val thresholdCarbonIntensity = forecast.sorted()[quantileIndex]

            isHighCarbon = newCarbonIntensity > thresholdCarbonIntensity
        }

        if (isHighCarbon) {
            scope.launch {
                pauseTasks()
            }
        }
    }

    private fun noForecastUpdateCarbonIntensity(newCarbonIntensity: Double): Boolean {
        this.pastCarbonIntensities.addLast(newCarbonIntensity)
        this.carbonRunningSum += newCarbonIntensity
        if (this.pastCarbonIntensities.size > this.windowSize) {
            this.carbonRunningSum -= this.pastCarbonIntensities.removeFirst()
        }

        val thresholdCarbonIntensity = this.carbonRunningSum / this.pastCarbonIntensities.size

        isHighCarbon = (newCarbonIntensity > thresholdCarbonIntensity)
        return isHighCarbon
    }

    override fun setCarbonModel(carbonModel: CarbonModel?) {
        this.carbonModel = carbonModel
    }

    override fun removeCarbonModel(carbonModel: CarbonModel?) {}
}
