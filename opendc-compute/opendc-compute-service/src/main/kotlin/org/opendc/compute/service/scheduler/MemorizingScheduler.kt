package org.opendc.compute.service.scheduler

import org.opendc.compute.api.Task
import org.opendc.compute.service.HostView
import org.opendc.compute.service.scheduler.filters.HostFilter
import java.util.SplittableRandom
import java.util.random.RandomGenerator

public class MemorizingScheduler(
    private val filters: List<HostFilter>,
    private val random: RandomGenerator = SplittableRandom(0),
    private val maxTimesSkipped: Int = 7
): ComputeScheduler {

    // We assume that there will be max 200 tasks per host.
    // The index of a host list is the number of tasks on that host.
    private val hostsQueue = List(200, { mutableListOf<HostView>() })
    private var minAvailableHost = 0
    private var numHosts = 0

    override fun addHost(host: HostView) {
        val zeroQueue = hostsQueue[0]
        zeroQueue.add(host)
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
                for (i in minAvailableHost+1 .. hostsQueue.lastIndex) {
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

    override fun select(iter: MutableIterator<Task>): HostView? {
        if (numHosts == 0) {
            return null
        }

        val chosenList = hostsQueue[minAvailableHost]
        val nextList = hostsQueue[minAvailableHost + 1]
        var chosenHost = chosenList.elementAt(random.nextInt(chosenList.size))

        var taskFound = false
        for (task in iter) {
            val satisfied = filters.all { filter -> filter.test(chosenHost, task) }
            if (satisfied) {
                iter.remove()
                taskFound = true
                break
            } else if (task.timesSkipped >= maxTimesSkipped) {
                // Don't skip over it. Wait till a suitable host becomes available
                val q = hostsQueue[minAvailableHost]
                for (h in q) {
                    val satisfied = filters.all { filter -> filter.test(h, task) }
                    if (satisfied) {
                        chosenHost = h
                        taskFound = true
                        break
                    }
                }
                if (!taskFound) {
                    return null
                }
            } else {
                task.timesSkipped++
            }
        }
        if (!taskFound) return null // No task found that fits in the host

        val listIdx = chosenHost.listIndex

        if (listIdx == chosenList.size - 1) {
            chosenList.removeLast()
            if (chosenList.isEmpty()) minAvailableHost++
        } else {
            val lastItem = chosenList.removeLast()
            chosenList[listIdx] = lastItem
            lastItem.listIndex = listIdx
        }
        nextList.add(chosenHost)
        chosenHost.priorityIndex = minAvailableHost + 1
        chosenHost.listIndex = nextList.size - 1

        return chosenHost
    }

    override fun removeTask(task: Task, hv: HostView) {
        val priorityIdx = hv.priorityIndex
        val listIdx = hv.listIndex
        val chosenList = hostsQueue[priorityIdx]
        val nextList = hostsQueue[priorityIdx - 1]

        if (listIdx == chosenList.size - 1) {
            chosenList.removeLast()
            if (listIdx == minAvailableHost) {
                minAvailableHost--
            }
        } else {
            val lastItem = chosenList.removeLast()
            chosenList[listIdx] = lastItem
            lastItem.listIndex = listIdx
        }
        nextList.add(hv)
        hv.priorityIndex = priorityIdx - 1
        hv.listIndex = nextList.size - 1
    }
}
