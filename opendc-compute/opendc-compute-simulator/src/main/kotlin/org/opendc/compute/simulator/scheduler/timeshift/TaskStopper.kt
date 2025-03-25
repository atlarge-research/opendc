package org.opendc.compute.simulator.scheduler.timeshift

import org.opendc.compute.simulator.service.ComputeService
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.CarbonReceiver
import java.time.InstantSource
import java.util.LinkedList
import kotlin.math.roundToInt

public class TaskStopper(
    private val clock: InstantSource,
    private val forecast: Boolean = true,
    private val forecastThreshold: Double = 0.6,
    private val forecastSize: Int = 24,
    private val windowSize: Int = 168,
) : CarbonReceiver {

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

            val snapshots = guests.map {
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
            pauseTasks()
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
