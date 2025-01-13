/*
 * Copyright (c) 2024 AtLarge Research
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
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import java.util.SplittableRandom
import java.util.random.RandomGenerator


/*
This scheduler records the number of tasks scheduled on each host.
When scheduling a new task, it assign the next task to the host with the least number of tasks.
We filter hosts to check if the specific task can actually run on the host.
 */
public class MemorizingScheduler(
    private val filters: List<HostFilter>,
    private val random: RandomGenerator = SplittableRandom(0),
    private val maxTimesSkipped: Int = 7,
) : ComputeScheduler {
    // We assume that there will be max 200 tasks per host.
    // The index of a host list is the number of tasks on that host.
    private val hostsQueue = List(200, { mutableListOf<HostView>() })
    private var minAvailableHost = 0
    private var numHosts = 0

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

        if (listIdx == chosenList.size - 1) {
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

    override fun select(iter: MutableIterator<SchedulingRequest>): SchedResult {
        if (numHosts == 0) {
            return SchedResult(SchedResultType.FAILURE)
        }

        var chosenList: MutableList<HostView>? = null
        var chosenHost: HostView? = null

        var result: SchedResult? = null
        taskloop@ for (req in iter) {
            if (req.isCancelled) {
                iter.remove()
            }
            var satisfied = false
            for (chosenListIndex in minAvailableHost until hostsQueue.size) {
                chosenList = hostsQueue[chosenListIndex]

                for (host in chosenList) {
                    val satisfied = filters.all { filter -> filter.test(host, req.task) }
                    if (satisfied) {
                        iter.remove()
                        chosenHost = host
                        result = SchedResult(SchedResultType.SUCCESS, host, req)
                        break@taskloop
                    } else if (req.timesSkipped >= maxTimesSkipped) {
                        return SchedResult(SchedResultType.FAILURE, null, req)
                    }
                }
            }
            req.timesSkipped++
        }

        if (result == null) return SchedResult(SchedResultType.EMPTY) // No tasks to schedule that fit

        // Bookkeeping to maintain the calendar priority queue
        val listIdx = chosenHost!!.listIndex

        if (listIdx == chosenList!!.size - 1) {
            chosenList.removeLast()
            if (chosenList.isEmpty()) minAvailableHost++
        } else {
            val lastItem = chosenList.removeLast()
            chosenList[listIdx] = lastItem
            lastItem.listIndex = listIdx
        }

        val nextList = hostsQueue[chosenHost.priorityIndex + 1]
        nextList.add(chosenHost)
        chosenHost.priorityIndex++
        chosenHost.listIndex = nextList.size - 1

        return result
    }

    override fun removeTask(task: ServiceTask, host: HostView) {
        val priorityIdx = host.priorityIndex
        val listIdx = host.listIndex
        val chosenList = hostsQueue[priorityIdx]
        val nextList = hostsQueue[priorityIdx - 1]

        if (listIdx == chosenList.size - 1) {
            chosenList.removeLast()
            if (priorityIdx == minAvailableHost) {
                minAvailableHost--
            }
        } else {
            val lastItem = chosenList.removeLast()
            chosenList[listIdx] = lastItem
            lastItem.listIndex = listIdx
        }
        nextList.add(host)
        host.priorityIndex--
        host.listIndex = nextList.size - 1
    }
}
