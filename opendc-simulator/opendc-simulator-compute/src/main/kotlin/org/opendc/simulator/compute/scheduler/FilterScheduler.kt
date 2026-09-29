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

package org.opendc.simulator.compute.scheduler

import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.models.HostModel
import org.opendc.simulator.compute.models.HostState
import org.opendc.simulator.compute.scheduler.filters.HostFilter
import org.opendc.simulator.compute.scheduler.weights.HostWeigher
import org.opendc.simulator.compute.task.SimTask
import java.util.SplittableRandom
import java.util.random.RandomGenerator
import kotlin.math.min

/**
 * A [ComputeScheduler] implementation that uses filtering and weighing passes to select
 * the host to schedule a [SimTask] on.
 *
 * This implementation is based on the filter scheduler from OpenStack Nova.
 * See: https://docs.openstack.org/nova/latest/user/filter-scheduler.html
 *
 * Hosts running tasks are kept in a [SortedHostList], so the ones that pass the filters are found by binary search.
 * Empty hosts are grouped by [SimHost.modelId]: empty hosts with the same model look the same to every filter and
 * weigher, so only one host per group is tested and weighed.
 *
 * @param filters The list of filters to apply when searching for an appropriate host.
 * @param weighers The list of weighers to apply when searching for an appropriate host.
 * @param subsetSize The number of best-weighed hosts from which a target is randomly chosen.
 * @param random A [RandomGenerator] instance for choosing from the best-weighed hosts.
 * @param numHosts The expected number of hosts.
 */
public class FilterScheduler(
    private val filters: List<HostFilter>,
    private val weighers: List<HostWeigher>,
    private val subsetSize: Int = 1,
    private val random: RandomGenerator = SplittableRandom(0),
    numHosts: Int = 1000,
) : ComputeScheduler {
    /** Hosts running at least one task. */
    private val usedHosts = SortedHostList(numHosts, filters)

    /** Hosts without tasks, grouped by model: the group of a model is at the index of its [SimHost.modelId]. */
    private val emptyHosts = ArrayList<LinkedHashSet<SimHost>>()

    /** The model of each group, to check that hosts sharing a model id have the same model. */
    private val groupModels = ArrayList<HostModel?>()

    /** Hosts that are currently not available. */
    private val failedHosts = HashSet<SimHost>()

    /** The hosts that pass the filters for the task being scheduled: fitting used hosts first, then one host per empty group. */
    private val candidates = HostBuffer(numHosts)

    /** The number of [candidates] that are used hosts; each candidate after them stands for its empty-host group. */
    private var usedCandidates = 0

    init {
        require(subsetSize >= 1) { "Subset size must be one or greater" }
    }

    override fun addHost(host: SimHost) {
        checkModelId(host)

        if (host.isEmpty()) {
            emptyHosts[host.modelId].add(host)
        } else {
            usedHosts.put(host)
        }
    }

    /** Make sure the group of [host] exists and holds only hosts of the same model. */
    private fun checkModelId(host: SimHost) {
        val id = host.modelId
        require(id >= 0) { "Host ${host.name} has no model id" }

        while (emptyHosts.size <= id) {
            emptyHosts.add(LinkedHashSet())
            groupModels.add(null)
        }

        val model = groupModels[id]
        if (model == null) {
            groupModels[id] = host.model
        } else {
            require(model == host.model) { "Host ${host.name} has model id $id, which belongs to a different model" }
        }
    }

    // Remove host from the Available hosts list
    override fun removeHost(host: SimHost) {
        emptyHosts[host.modelId].remove(host)
        usedHosts.remove(host)
        failedHosts.remove(host)
    }

    // Remove a failed host from available hosts, and add it to the failed hosts.
    override fun failHost(host: SimHost) {
        removeHost(host)
        failedHosts.add(host)
    }

    override fun restartHost(host: SimHost) {
        if (failedHosts.remove(host)) {
            addHost(host)
        }
    }

    override fun updateHost(host: SimHost) {
        if (host.state != HostState.UP) {
            return
        }

        if (host.isEmpty()) {
            setHostEmpty(host)
        } else {
            emptyHosts[host.modelId].remove(host)
            usedHosts.put(host)
        }
    }

    override fun setHostEmpty(host: SimHost) {
        usedHosts.remove(host)
        emptyHosts[host.modelId].add(host)
    }

    override fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult {
        val req = nextRequest(iter) ?: return SchedulingResult(SchedulingResultType.EMPTY)
        val task = req.task

        collectCandidates(task)
        if (candidates.isEmpty()) {
            return SchedulingResult(SchedulingResultType.FAILURE, null, req)
        }

        val host = if (subsetSize == 1) candidates[bestCandidate(task)] else chooseFromSubset(task)

        iter.remove()

        if (host.isEmpty()) {
            emptyHosts[host.modelId].remove(host)
            usedHosts.put(host)
        }

        return SchedulingResult(SchedulingResultType.SUCCESS, host, req)
    }

    /** Skip and remove cancelled requests, returning the first request still to schedule. */
    private fun nextRequest(iter: MutableIterator<SchedulingRequest>): SchedulingRequest? {
        while (iter.hasNext()) {
            val req = iter.next()
            if (!req.isCancelled) {
                return req
            }
            iter.remove()
        }
        return null
    }

    private fun collectCandidates(task: SimTask) {
        candidates.clear()

        usedHosts.addFittingHosts(task, candidates)
        usedCandidates = candidates.size

        for (group in emptyHosts) {
            if (group.isEmpty()) {
                continue
            }

            val host = group.first()
            if (filters.all { it.test(host, task) }) {
                candidates.add(host)
            }
        }
    }

    /** The number of hosts candidate [i] stands for: one for a used host, the group size for an empty host. */
    private fun multiplicity(i: Int): Int = if (i < usedCandidates) 1 else emptyHosts[candidates[i].modelId].size

    /**
     * The combined weight of every candidate. Each weigher's weights are scaled to between 0 and its multiplier, so
     * weighers with different units can be summed.
     */
    private fun weigh(task: SimTask): DoubleArray {
        val scores = DoubleArray(candidates.size)
        for (weigher in weighers) {
            val result = weigher.getWeights(candidates, task)
            val range = result.max - result.min

            // Skip result if all weights are the same
            if (range == 0.0) {
                continue
            }

            val factor = result.multiplier / range
            for (i in scores.indices) {
                scores[i] += factor * (result.weights[i] - result.min)
            }
        }
        return scores
    }

    /** The index of the highest-weighed candidate; the first one on a tie. */
    private fun bestCandidate(task: SimTask): Int {
        if (weighers.isEmpty()) {
            return 0
        }

        val scores = weigh(task)
        var best = 0
        for (i in 1 until scores.size) {
            if (scores[i] > scores[best]) {
                best = i
            }
        }
        return best
    }

    /** Choose uniformly among the [subsetSize] highest-weighed hosts, counting every host of an empty group. */
    private fun chooseFromSubset(task: SimTask): SimHost {
        val total = candidates.indices.sumOf { multiplicity(it) }
        val subset = min(subsetSize, total)

        // When every host is in the subset, their order does not matter
        val order =
            if (weighers.isEmpty() || subset == total) {
                candidates.indices
            } else {
                val scores = weigh(task)
                candidates.indices.sortedByDescending { scores[it] }
            }

        var slot = random.nextInt(subset)
        for (i in order) {
            slot -= multiplicity(i)
            if (slot < 0) {
                return candidates[i]
            }
        }
        error("Chose slot outside the $subset hosts of the subset")
    }

    override fun removeTask(
        task: SimTask,
        host: SimHost?,
    ) {
    }
}

/**
 * An append-only list of hosts that clears in constant time. [ArrayList.clear] writes null to every slot, which costs a
 * pass over all candidates on every selection. Leaving the old references in place is harmless, because hosts live for
 * the whole simulation.
 */
private class HostBuffer(capacity: Int) : AbstractMutableList<SimHost>(), RandomAccess {
    private var hosts = arrayOfNulls<SimHost>(maxOf(capacity, 16))

    override var size: Int = 0
        private set

    override fun get(index: Int): SimHost {
        if (index >= size) throw IndexOutOfBoundsException("Index $index out of bounds for size $size")
        return hosts[index]!!
    }

    override fun add(
        index: Int,
        element: SimHost,
    ) {
        if (index != size) throw UnsupportedOperationException("Hosts can only be appended")
        if (size == hosts.size) {
            hosts = hosts.copyOf(size * 2)
        }
        hosts[size++] = element
    }

    override fun clear() {
        size = 0
    }

    override fun set(
        index: Int,
        element: SimHost,
    ): SimHost = throw UnsupportedOperationException("Hosts can only be appended")

    override fun removeAt(index: Int): SimHost = throw UnsupportedOperationException("Hosts can only be appended")
}
