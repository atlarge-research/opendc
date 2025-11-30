/*
 * Copyright (c) 2021 AtLarge Research
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

import org.opendc.compute.simulator.host.HostState
import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import java.util.SplittableRandom
import org.opendc.common.logger.errAndNull
import org.opendc.common.logger.logger
import java.util.random.RandomGenerator

/**
 * A [ComputeScheduler] implementation that uses filtering and weighing passes to select
 * the host to schedule a [ServiceTask] on.
 *
 * This implementation is based on the filter scheduler from OpenStack Nova.
 * See: https://docs.openstack.org/nova/latest/user/filter-scheduler.html
 *
 * @param filters The list of filters to apply when searching for an appropriate host.
 * @param weighers The list of weighers to apply when searching for an appropriate host.
 * @param subsetSize The size of the subset of best hosts from which a target is randomly chosen.
 * @param random A [RandomGenerator] instance for selecting
 */
public open class FilterScheduler(
    private val filters: List<HostFilter>,
    private val weighers: List<HostWeigher>,
    private val subsetSize: Int = 1,
    private val random: RandomGenerator = SplittableRandom(0),
    numHosts: Int = 1000,
) : ComputeScheduler {
    /**
     * The pool of hosts available to the scheduler.
     */

    private val failedHosts = mutableListOf<HostView>() // List of Hosts that are currently not available
    private val emptyHostMap = mutableMapOf<String, MutableList<HostView>>()

    private val weights = DoubleArray(numHosts)

    private val usedHosts = SortedHostViewList(numHosts, filters)

    init {
        require(subsetSize >= 1) { "Subset size must be one or greater" }
    }

    override fun addHost(hostView: HostView) {
        val hostType = hostView.host.getType()

        if (emptyHostMap.containsKey(hostType)) {
            emptyHostMap[hostType]?.add(hostView)
        } else {
            emptyHostMap[hostType] = mutableListOf(hostView)
        }
    }

    // Remove host from the Available hosts list
    override fun removeHost(hostView: HostView) {
        val hostType = hostView.host.getType()

        // remove from emptyHosts if present
        val removed = emptyHostMap[hostType]?.remove(hostView)
        if (removed != null && removed) {
            return
        }

        // If the hosts was being used, remove it from there
        usedHosts.remove(hostView)
    }

    // Remove a failed host from available hosts, and add it to the failed hosts.
    override fun failHost(hostView: HostView) {
        removeHost(hostView)
        failedHosts.add(hostView)
    }

    override fun restartHost(hostView: HostView) {
        val removed = failedHosts.remove(hostView)
        if (removed) {
            addHost(hostView)
        }
    }

    override fun updateHost(hostView: HostView) {
        if (hostView.host.getState() == HostState.ERROR) {
            return
        }

        if (hostView.host.isEmpty()) {
            setHostEmpty(hostView)
        } else {
            usedHosts.updateHost(hostView)
        }
    }

    override fun setHostEmpty(hostView: HostView) {
        val hostType = hostView.host.getType()

        usedHosts.remove(hostView)
        if (emptyHostMap.containsKey(hostType)) {
            emptyHostMap[hostType]?.add(hostView)
        } else {
            emptyHostMap[hostType] = mutableListOf(hostView)
        }
    }

    override fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult {
        val req = selectTask(iter) ?: return SchedulingResult(SchedulingResultType.EMPTY)
        
        val task = req.task

        val fittingHosts = usedHosts.getFittingHosts(task)

        for (emptyHosts in emptyHostMap.values) {
            if (!emptyHosts.isEmpty()) {
                val host = emptyHosts.first()
                if (filters.all { filter -> filter.test(host, req.task) }) {
                    fittingHosts.add(host)
                }
            }
        }

        if (fittingHosts.isEmpty()) {
            return SchedulingResult(SchedulingResultType.FAILURE, null, req)
        }

        var maxWeight = Double.MIN_VALUE
        var maxIndex = 0

        val hostView =
            if (weighers.isNotEmpty()) {
                val results = weighers.map { it.getWeights(fittingHosts, task) }

                for (result in results) {
                    val min = result.min
                    val range = (result.max - min)

                    // Skip result if all weights are the same
                    if (range == 0.0) {
                        continue
                    }

                    val multiplier = result.multiplier
                    val factor = multiplier / range

                    for ((i, weight) in result.weights.withIndex()) {
                        this.weights[i] += factor * (weight - min)
                        if (this.weights[i] > maxWeight) {
                            maxIndex = i
                            maxWeight = weight
                        }
                    }
                }

                fittingHosts[maxIndex]
            } else {
                fittingHosts.first()
            }

        iter.remove()

        val hostType = hostView.host.getType()

        if (hostView.host.isEmpty()) {
            emptyHostMap[hostType]?.remove(hostView)
            usedHosts.addSorted(hostView)
        }

        return SchedulingResult(SchedulingResultType.SUCCESS, hostView, req)
    }


    protected open fun selectTask(iter: MutableIterator<SchedulingRequest>): SchedulingRequest? {
        if (!iter.hasNext()) {
            return null
        }
        
        var req = iter.next()

        while (req.isCancelled) {
            iter.remove()
            if (iter.hasNext()) {
                req = iter.next()
            } else {
                // No tasks in queue
                return null
            }
        }
        
        return req
    }

    override fun removeTask(
        task: ServiceTask,
        host: HostView?,
    ) {
    }

    private companion object {
        val LOG by logger()
    }
}
