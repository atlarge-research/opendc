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
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.power.CarbonModel
import java.time.InstantSource
import java.util.LinkedList

public class MemorizingTimeshift(
    private val filters: List<HostFilter>,
    override val windowSize: Int,
    override val clock: InstantSource,
    override val forecast: Boolean = true,
    override val shortForecastThreshold: Double = 0.2,
    override val longForecastThreshold: Double = 0.35,
    override val forecastSize: Int = 24,
    public val maxTimesSkipped: Int = 7,
) : ComputeScheduler, Timeshifter {
    // We assume that there will be max 200 tasks per host.
    // The index of a host list is the number of tasks on that host.
    private val hostsQueue = List(100) { mutableListOf<HostView>() }
    private var minAvailableHost = 0
    private var numHosts = 0

    override val pastCarbonIntensities: LinkedList<Double> = LinkedList<Double>()
    override var carbonRunningSum: Double = 0.0
    override var shortLowCarbon: Boolean = false // Low carbon regime for short tasks (< 2 hours)
    override var longLowCarbon: Boolean = false // Low carbon regime for long tasks (>= hours)
    override var carbonMod: CarbonModel? = null

    override fun addHost(host: HostView) {
        val zeroQueue = hostsQueue[0]
        zeroQueue.add(host)
        host.priorityIndex = 0
        host.listIndex = zeroQueue.size - 1
        numHosts++
        minAvailableHost = 0
    }

    override fun removeHost(host: HostView) {
        val priorityIdx = host.priorityIndex
        val listIdx = host.listIndex
        val chosenList = hostsQueue[priorityIdx]

        if (chosenList.size == 1) {
            chosenList.removeLast()
            if (listIdx == minAvailableHost) {
                for (i in minAvailableHost + 1..hostsQueue.lastIndex) {
                    if (hostsQueue[i].size > 0) {
                        minAvailableHost = i
                        break
                    }
                }
            }
        } else {
            val lastItem = chosenList.removeLast()
            chosenList[listIdx] = lastItem
            lastItem.listIndex = listIdx
        }
        numHosts--
    }

    override fun updateHost(hostView: HostView) {
        // No-op
    }

    override fun setHostEmpty(hostView: HostView) {
        // No-op
    }

    override fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult {
        if (numHosts == 0) {
            return SchedulingResult(SchedulingResultType.FAILURE)
        }

        val maxIters = 10000
        var numIters = 0

        var chosenList: MutableList<HostView>? = null
        var chosenHost: HostView? = null

        var result: SchedulingResult? = null
        taskloop@ for (req in iter) {
            if (req.isCancelled) {
                iter.remove()
                continue
            }

            numIters++
            if (numIters > maxIters) {
                return SchedulingResult(SchedulingResultType.EMPTY)
            }

            val task = req.task

            /**
             If we are not in a low carbon regime, delay tasks.
             Only delay tasks if they are deferrable and it doesn't violate the deadline.
             Separate delay thresholds for short and long tasks.
             */
            if (task.deferrable) {
                val durInHours = task.duration / (1000.0 * 60.0 * 60.0)
                if ((durInHours < 2 && !shortLowCarbon) ||
                    (durInHours >= 2 && !longLowCarbon)
                ) {
                    val currentTime = clock.millis()
                    val estimatedCompletion = currentTime + task.duration
                    val deadline = task.deadline
                    if (estimatedCompletion <= deadline) {
                        // No need to schedule this task in a high carbon intensity period
                        continue
                    }
//                    val currentTime = clock.instant()
//                    val estimatedCompletion = currentTime.plus(task.duration)
//                    val deadline = Instant.ofEpochMilli(task.deadline)
//                    if (estimatedCompletion.isBefore(deadline)) {
//                        // No need to schedule this task in a high carbon intensity period
//                        continue
//                    }
                }
            }

            for (chosenListIndex in minAvailableHost until hostsQueue.size) {
                chosenList = hostsQueue[chosenListIndex]

                for (host in chosenList) {
                    val satisfied = filters.all { filter -> filter.test(host, req.task) }
                    if (satisfied) {
                        iter.remove()
                        chosenHost = host
                        result = SchedulingResult(SchedulingResultType.SUCCESS, host, req)
                        break@taskloop
                    } else if (req.timesSkipped >= maxTimesSkipped) {
                        return SchedulingResult(SchedulingResultType.FAILURE, null, req)
                    }
                }
            }
            req.timesSkipped++
        }

        if (result == null) return SchedulingResult(SchedulingResultType.EMPTY) // No tasks to schedule that fit

        // Bookkeeping to maintain the calendar priority queue
        if (chosenList!!.size == 1) {
            chosenList.removeLast()
            minAvailableHost++
        } else {
            val listIdx = chosenHost!!.listIndex
            // Not using removeLast here as it would cause problems during swapping
            // if chosenHost is lastItem
            val lastItem = chosenList.last()
            chosenList[listIdx] = lastItem
            lastItem.listIndex = listIdx
            chosenList.removeLast()
        }

        val nextList = hostsQueue[chosenHost!!.priorityIndex + 1]
        nextList.add(chosenHost)
        chosenHost.priorityIndex++
        chosenHost.listIndex = nextList.size - 1

        return result
    }

    override fun removeTask(
        task: ServiceTask,
        host: HostView?,
    ) {
        if (host == null) return

        val priorityIdx = host.priorityIndex
        val listIdx = host.listIndex
        val chosenList = hostsQueue[priorityIdx]
        val nextList = hostsQueue[priorityIdx - 1]

        if (chosenList.size == 1) {
            chosenList.removeLast()
        } else {
            val lastItem = chosenList.last()
            chosenList[listIdx] = lastItem
            lastItem.listIndex = listIdx
            chosenList.removeLast()
        }

        nextList.add(host)
        host.priorityIndex--
        host.listIndex = nextList.size - 1
        if (priorityIdx == minAvailableHost) {
            minAvailableHost--
        }
    }
}
