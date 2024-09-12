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

    override fun removeTask(task: Task, hv: HostView) {
        // All necessary bookkeeping is already handled by ComputeService
        // ComputeService increments/decrements the number of instances on a host
    }
}
