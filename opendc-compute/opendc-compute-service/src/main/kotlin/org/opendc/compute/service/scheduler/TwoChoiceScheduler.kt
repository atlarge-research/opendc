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

package org.opendc.compute.service.scheduler

import org.opendc.compute.api.Task
import org.opendc.compute.service.HostView
import org.opendc.compute.service.scheduler.filters.HostFilter
import java.util.SplittableRandom
import java.util.random.RandomGenerator

public class TwoChoiceScheduler(
    private val filters: List<HostFilter>,
    private val random: RandomGenerator = SplittableRandom(0),
): ComputeScheduler {

    private val hostsList = mutableListOf<HostView>()
    private var numHosts = 0

    override fun addHost(host: HostView) {
        hostsList.add(host)
        host.listIndex = hostsList.size
        numHosts++
    }

    override fun removeHost(host: HostView) {
        val listIdx = host.listIndex

        if (listIdx == hostsList.size - 1) {
            hostsList.removeLast()
        } else {
            val lastItem = hostsList.removeLast()
            hostsList[listIdx] = lastItem
            lastItem.listIndex = listIdx
        }
        numHosts--
    }

    override fun select(iter: MutableIterator<Task>): HostView? {
        if (numHosts == 0) {
            return null
        }

        val chosen1 = hostsList.elementAt(random.nextInt(hostsList.size))
        val chosen2 = hostsList.elementAt(random.nextInt(hostsList.size))
        val chosenHost = minOf(chosen1, chosen2, { a, b -> a.instanceCount - b.instanceCount })

        var taskFound = false
        for (task in iter) {
            val satisfied = filters.all { filter -> filter.test(chosenHost, task) }
            if (satisfied) {
                iter.remove()
                taskFound = true
                break
            }
        }
        if (!taskFound) return null // No task found that fits in the host

        return chosenHost
    }

    override fun removeTask(task: Task, host: HostView) {
        // All necessary bookkeeping is already handled by ComputeService
        // ComputeService increments/decrements the number of instances on a host
    }
}
