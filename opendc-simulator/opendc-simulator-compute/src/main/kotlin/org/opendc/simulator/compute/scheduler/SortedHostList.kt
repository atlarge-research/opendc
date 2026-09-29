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

package org.opendc.simulator.compute.scheduler

import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.scheduler.filters.HostFilter
import org.opendc.simulator.compute.task.SimTask

/**
 * A list of hosts kept sorted so the hosts that pass a set of [HostFilter]s can be found quickly.
 *
 * The hosts are sorted on the scores of the sortable filters (see [HostFilter.isSortable]). The first sortable filter
 * is applied by binary search; only the hosts it passes are tested against the other filters.
 *
 * Every host is stored with the scores it was sorted on, and ties are broken by [SimHost.id]. A host whose live scores
 * have changed can therefore still be found by binary search, so moving or removing it does not have to scan the list.
 *
 * @param capacity The expected number of hosts.
 * @param filters The filters a host must pass.
 */
public class SortedHostList(
    capacity: Int,
    filters: List<HostFilter>,
) {
    /**
     * A [host] with the [scores] it is sorted on. Its [id] breaks ties, so every entry has a unique position.
     */
    private class Entry(val host: SimHost, val scores: DoubleArray) {
        val id = host.id
        var isListed = false
    }

    private val sortFilters = filters.filter { it.isSortable }

    /** The filter applied by binary search, or null when no filter is sortable. */
    private val indexFilter = sortFilters.firstOrNull()

    /** The filters tested on each host that passes [indexFilter]. */
    private val otherFilters = filters.filter { it !== indexFilter }

    private val entries = ArrayList<Entry>(capacity)

    /** The entry of every host ever added, at the index of its [SimHost.id]. */
    private var entryOf = arrayOfNulls<Entry>(capacity)

    private val comparator =
        Comparator<Entry> { a, b ->
            for (i in a.scores.indices) {
                val result = a.scores[i].compareTo(b.scores[i])
                if (result != 0) {
                    return@Comparator result
                }
            }
            a.id.compareTo(b.id)
        }

    /** Add [host] at its sorted position, or move it there if it is already listed and its scores changed. */
    public fun put(host: SimHost) {
        val entry = entryOf.getOrNull(host.id) ?: newEntry(host)
        check(entry.host === host) { "Hosts ${entry.host.name} and ${host.name} have the same id ${host.id}" }

        if (entry.isListed) {
            // Without scores, the position of a host never changes
            if (sortFilters.isEmpty()) {
                return
            }
            entries.removeAt(indexOf(entry))
        }

        for (i in sortFilters.indices) {
            entry.scores[i] = sortFilters[i].score(host)
        }

        // The entry is not listed and its id is unique, so the search returns the insertion point
        entries.add(-entries.binarySearch(entry, comparator) - 1, entry)
        entry.isListed = true
    }

    private fun newEntry(host: SimHost): Entry {
        val id = host.id
        require(id >= 0) { "Host ${host.name} has a negative id $id" }
        if (id >= entryOf.size) {
            entryOf = entryOf.copyOf(maxOf(id + 1, entryOf.size * 2))
        }

        val entry = Entry(host, DoubleArray(sortFilters.size))
        entryOf[id] = entry
        return entry
    }

    public fun remove(host: SimHost) {
        val entry = entryOf.getOrNull(host.id)?.takeIf { it.host === host } ?: return
        if (entry.isListed) {
            entries.removeAt(indexOf(entry))
            entry.isListed = false
        }
    }

    private fun indexOf(entry: Entry): Int {
        val index = entries.binarySearch(entry, comparator)
        check(index >= 0) { "Host ${entry.host.name} is not at its sorted position" }
        return index
    }

    /** Add the hosts that pass every filter for [task] to [out], in sorted order. */
    public fun addFittingHosts(
        task: SimTask,
        out: MutableList<SimHost>,
    ) {
        val start = if (indexFilter == null) 0 else firstPassingIndex(indexFilter, task)
        for (i in start until entries.size) {
            val host = entries[i].host
            if (otherFilters.all { it.test(host, task) }) {
                out.add(host)
            }
        }
    }

    /** The index of the first host that passes [filter], or the size of the list if none does. */
    private fun firstPassingIndex(
        filter: HostFilter,
        task: SimTask,
    ): Int {
        var low = 0
        var high = entries.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (filter.test(entries[mid].host, task)) high = mid else low = mid + 1
        }
        return low
    }
}
