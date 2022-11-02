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

package org.opendc.compute.service.scheduler

import org.opendc.compute.api.Server
import org.opendc.compute.service.HostView
import org.opendc.compute.service.scheduler.filters.HostFilter
import org.opendc.compute.service.scheduler.weights.HostWeigher
import java.util.SplittableRandom
import java.util.random.RandomGenerator
import kotlin.math.min

/**
 * A [ComputeScheduler] implementation that uses filtering and weighing passes to select
 * the host to schedule a [Server] on.
 *
 * This implementation is based on the filter scheduler from OpenStack Nova.
 * See: https://docs.openstack.org/nova/latest/user/filter-scheduler.html
 *
 * @param filters The list of filters to apply when searching for an appropriate host.
 * @param weighers The list of weighers to apply when searching for an appropriate host.
 * @param subsetSize The size of the subset of best hosts from which a target is randomly chosen.
 * @param random A [RandomGenerator] instance for selecting
 */
public class FilterScheduler(
    private val filters: List<HostFilter>,
    private val weighers: List<HostWeigher>,
    private val subsetSize: Int = 1,
    private val random: RandomGenerator = SplittableRandom(0)
) : ComputeScheduler {
    /**
     * The pool of hosts available to the scheduler.
     */
    private val hosts = mutableListOf<HostView>()

    init {
        require(subsetSize >= 1) { "Subset size must be one or greater" }
    }

    override fun addHost(host: HostView) {
        hosts.add(host)
    }

    override fun removeHost(host: HostView) {
        hosts.remove(host)
    }

    override fun select(server: Server): HostView? {
        val hosts = hosts
        val filteredHosts = hosts.filter { host -> filters.all { filter -> filter.test(host, server) } }

        val subset = if (weighers.isNotEmpty()) {
            val results = weighers.map { it.getWeights(filteredHosts, server) }
            val weights = DoubleArray(filteredHosts.size)

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
                    weights[i] += factor * (weight - min)
                }
            }

            weights.indices
                .asSequence()
                .sortedByDescending { weights[it] }
                .map { filteredHosts[it] }
                .take(subsetSize)
                .toList()
        } else {
            filteredHosts
        }

        return when (val maxSize = min(subsetSize, subset.size)) {
            0 -> null
            1 -> subset[0]
            else -> subset[random.nextInt(maxSize)]
        }
    }
}
