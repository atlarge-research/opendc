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
import org.opendc.simulator.compute.scheduler.timeshift.Timeshifter
import org.opendc.simulator.compute.scheduler.weights.HostWeigher
import org.opendc.simulator.compute.task.SimTask
import java.util.TreeSet

/**
 * A [ComputeScheduler] implementation that uses filtering and weighing passes to select
 * the host to schedule a [SimTask] on.
 *
 * This implementation is based on the filter scheduler from OpenStack Nova.
 * See: https://docs.openstack.org/nova/latest/user/filter-scheduler.html
 *
 * The hosts are kept ranked by their score, the sum of their scaled weights. To place a task, the scheduler walks the
 * ranking from the highest score down and picks the first host that passes every filter; ties go to the lowest
 * [SimHost.id].
 *
 * Each weigher's weights are scaled to between 0 and its multiplier, using the lowest and highest weight the weigher
 * has given any host so far. That range only grows: when a weight falls outside it, every score is recomputed and the
 * hosts are ranked again. Otherwise a change to a host only moves that host.
 *
 * The ranking is split into blocks that record the most room (free cores, free memory, ...) any of their hosts has
 * for each [org.opendc.simulator.compute.scheduler.filters.ThresholdFilter], so the walk skips blocks in which no host
 * can fit the task (see [HostRanking]).
 *
 * Empty hosts are grouped by [SimHost.modelId]: empty hosts with the same model look the same to every filter and
 * weigher, so only the lowest-id host of each group is ranked, standing in for the whole group.
 *
 * With a [timeshifter], the scheduler skips the tasks it delays and places the first task in the queue that may start
 * now. Delayed tasks stay queued and are considered again in later scheduling cycles.
 *
 * @param filters The list of filters to apply when searching for an appropriate host.
 * @param weighers The list of weighers to apply when searching for an appropriate host.
 * @param numHosts The expected number of hosts.
 * @param blockSize The number of hosts a block of the ranking aims for.
 * @param timeshifter Decides which tasks wait instead of being placed now, or null to place every task right away.
 */
public class FilterScheduler internal constructor(
    private val filters: List<HostFilter>,
    private val weighers: List<HostWeigher>,
    numHosts: Int,
    blockSize: Int,
    public val timeshifter: Timeshifter? = null,
) : ComputeScheduler {
    public constructor(
        filters: List<HostFilter>,
        weighers: List<HostWeigher>,
        numHosts: Int = 1000,
        timeshifter: Timeshifter? = null,
    ) : this(filters, weighers, numHosts, DEFAULT_BLOCK_SIZE, timeshifter)

    /**
     * The hosts [select] chooses from, from the highest score to the lowest: every used host, and the first host of every
     * group of empty hosts.
     */
    private val ranking = HostRanking(filters, blockSize, numHosts)

    /** The weights of every host ever ranked, at the index of its [SimHost.id]. */
    private var weightsOf = arrayOfNulls<DoubleArray>(numHosts)

    /** The lowest and highest weight each weigher has given a host, and the factor that scales its weights. */
    private val minWeights = DoubleArray(weighers.size) { Double.POSITIVE_INFINITY }
    private val maxWeights = DoubleArray(weighers.size) { Double.NEGATIVE_INFINITY }
    private val factors = DoubleArray(weighers.size)

    /** Hosts without tasks, grouped by model and ordered by id: the group of a model is at the index of its [SimHost.modelId]. */
    private val emptyHosts = ArrayList<TreeSet<SimHost>>()

    /** The model of each group, to check that hosts sharing a model id have the same model. */
    private val groupModels = ArrayList<HostModel?>()

    /** Hosts that are currently not available. */
    private val failedHosts = HashSet<SimHost>()

    override fun addHost(host: SimHost) {
        checkModelId(host)

        if (host.isEmpty()) {
            addEmpty(host)
        } else {
            rank(host)
        }
    }

    /** Make sure the group of [host] exists and holds only hosts of the same model. */
    private fun checkModelId(host: SimHost) {
        val id = host.modelId
        require(id >= 0) { "Host ${host.name} has no model id" }

        while (emptyHosts.size <= id) {
            emptyHosts.add(TreeSet(compareBy(SimHost::id)))
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
        removeEmpty(host, keepRanked = false)
        unrank(host)
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
            addEmpty(host)
        } else {
            // A host that just got its first task leaves its group, but stays ranked on its own weights
            removeEmpty(host, keepRanked = true)
            rank(host)
        }
    }

    override fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult {
        while (iter.hasNext()) {
            val req = iter.next()
            if (req.isCancelled) {
                iter.remove()
                continue
            }

            // A delayed task stays in the queue; the next task gets its chance
            if (timeshifter?.shouldDelay(req.task) == true) {
                continue
            }

            val host = ranking.firstFit(req.task) ?: return SchedulingResult(SchedulingResultType.FAILURE, null, req)

            iter.remove()
            return SchedulingResult(SchedulingResultType.SUCCESS, host, req)
        }

        return SchedulingResult(SchedulingResultType.EMPTY)
    }

    /** Add [host] to its group of empty hosts. Only the first host of a group is ranked. */
    private fun addEmpty(host: SimHost) {
        val group = emptyHosts[host.modelId]
        val previous = group.firstOrNull()
        group.add(host)

        if (group.first() === host) {
            if (previous != null && previous !== host) {
                unrank(previous)
            }
            rank(host)
        } else {
            unrank(host)
        }
    }

    /**
     * Remove [host] from its group of empty hosts. If it was the first host of the group, the next one is ranked in its
     * place, and [host] only stays ranked if [keepRanked].
     */
    private fun removeEmpty(
        host: SimHost,
        keepRanked: Boolean,
    ) {
        val group = emptyHosts[host.modelId]
        val wasFirst = group.isNotEmpty() && group.first() === host
        if (!group.remove(host)) {
            return
        }

        if (wasFirst) {
            if (!keepRanked) {
                unrank(host)
            }
            if (group.isNotEmpty()) {
                rank(group.first())
            }
        }
    }

    /** Rank [host] on its current weights and resources, or move it there if it is already ranked. */
    private fun rank(host: SimHost) {
        val weights = weightsOf(host)

        var widened = false
        for (i in weighers.indices) {
            val weight = weighers[i].getWeight(host)
            weights[i] = weight
            if (weight < minWeights[i]) {
                minWeights[i] = weight
                widened = true
            }
            if (weight > maxWeights[i]) {
                maxWeights[i] = weight
                widened = true
            }
        }

        if (widened) {
            rescale()
        }

        ranking.put(host, score(weights))
    }

    private fun unrank(host: SimHost) {
        ranking.remove(host)
    }

    /** Recompute the factors from the weight ranges, and with them the score and position of every ranked host. */
    private fun rescale() {
        for (i in weighers.indices) {
            val range = maxWeights[i] - minWeights[i]
            factors[i] = if (range > 0.0) weighers[i].multiplier / range else 0.0
        }

        ranking.rescore { score(weightsOf[it.id]!!) }
    }

    /** The sum of [weights], each scaled to between 0 and the multiplier of its weigher. */
    private fun score(weights: DoubleArray): Double {
        var score = 0.0
        for (i in factors.indices) {
            score += factors[i] * (weights[i] - minWeights[i])
        }
        return score
    }

    /** The array holding the weights of [host]. */
    private fun weightsOf(host: SimHost): DoubleArray {
        val id = host.id
        require(id >= 0) { "Host ${host.name} has a negative id $id" }
        if (id >= weightsOf.size) {
            weightsOf = weightsOf.copyOf(maxOf(id + 1, weightsOf.size * 2))
        }
        return weightsOf[id] ?: DoubleArray(weighers.size).also { weightsOf[id] = it }
    }

    private companion object {
        const val DEFAULT_BLOCK_SIZE = 64
    }
}
