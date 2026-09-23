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

package org.opendc.compute.simulator.scheduler

import org.opendc.compute.simulator.infrastructure.SimHost
import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.service.ServiceTask

public class SortedHostList(
    public val capacity: Int,
    public val filters: List<HostFilter>,
) {
    private var firstFilter: HostFilter
    private var otherFilters: List<HostFilter> = listOf()

    private var noFilters = false

    public val hosts: ArrayList<SimHost> = ArrayList(capacity)

    public var cmp: Comparator<SimHost>

    init {
        cmp = compareBy { filters[0].score(it) as Comparable<*>? }
        for (i in 1 until filters.size) {
            cmp = cmp.thenBy { filters[i].score(it) as Comparable<*>? }
        }

        if (filters.isNotEmpty()) {
            firstFilter = filters[0]
        } else {
            noFilters = true
            firstFilter =
                object : HostFilter {
                    override fun test(
                        host: SimHost,
                        task: ServiceTask,
                    ): Boolean {
                        return true
                    }

                    override fun score(host: SimHost): Double {
                        return 0.0
                    }
                }
        }

        if (filters.size > 1) {
            otherFilters = filters.subList(1, filters.size)
        }
    }

    public fun addSorted(host: SimHost) {
        if (noFilters) {
            hosts.add(host)
            return
        }

        val index = hosts.binarySearch(host, cmp)
        val insertIndex = if (index < 0) -index - 1 else index
        hosts.add(insertIndex, host)
    }

    public fun updateHost(host: SimHost) {
        // TODO: See if we can improve this by using binary search to find the index
        hosts.remove(host)

        // TODO: See if we can move this instead of removing and adding
        addSorted(host)
    }

    public fun findIndex(task: ServiceTask): Int {
        // lower_bound on firstFilter.score
        var lowIndex = 0
        var highIndex = this.hosts.size
        while (lowIndex < highIndex) {
            val mid = (lowIndex + highIndex) ushr 1
            if (this.firstFilter.test(this.hosts[mid], task)) highIndex = mid else lowIndex = mid + 1
        }

        if (lowIndex == 0) {
            return if (this.hosts.isNotEmpty() && this.firstFilter.test(this.hosts[0], task)) 0 else -1
        }

        return lowIndex
    }

    public fun remove(host: SimHost) {
        hosts.remove(host)
    }

    public fun getFittingHosts(task: ServiceTask): MutableList<SimHost> {
        if (filters.isEmpty()) {
            return hosts
        }

        val index = findIndex(task)

        if (index < 0) return mutableListOf()

        var subset = hosts.subList(index, hosts.size).toMutableList()

        if (otherFilters.isEmpty()) {
            return subset
        }

        subset = subset.filter { host -> otherFilters.all { it.test(host, task) } }.toMutableList()

        return subset
    }

    public fun isSorted(): Boolean {
        return hosts.isSorted(cmp)
    }

    private fun <T> List<T>.isSorted(comparator: Comparator<in T>): Boolean {
        for (i in 0 until size - 1) {
            if (comparator.compare(this[i], this[i + 1]) > 0) {
                return false
            }
        }
        return true
    }
}
