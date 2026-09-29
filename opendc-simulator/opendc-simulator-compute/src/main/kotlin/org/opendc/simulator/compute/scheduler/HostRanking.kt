/*
 * Copyright (c) 2026 AtLarge Research
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
import org.opendc.simulator.compute.scheduler.filters.ThresholdFilter
import org.opendc.simulator.compute.task.SimTask

/**
 * The hosts a [FilterScheduler] chooses from, ranked from the highest score to the lowest, ties going to the lowest
 * [SimHost.id]. The ranking is split into blocks of consecutive hosts.
 *
 * Every host is stored with its score and, for each [ThresholdFilter], the room it has; every block stores the most room
 * any of its hosts has. [firstFit] uses these to rule out whole blocks, and then single hosts, before running the
 * filters. Because hosts are found and skipped by these stored values, a host must be [put] again whenever the
 * resources in use on it change.
 *
 * @param filters The filters a host must pass.
 * @param blockSize The number of hosts a block aims for. A block is split when it reaches twice this size, and merged
 *   with a neighbor when it drops below a quarter of it.
 * @param numHosts The expected number of hosts.
 */
internal class HostRanking(
    private val filters: List<HostFilter>,
    private val blockSize: Int,
    numHosts: Int,
) {
    init {
        require(blockSize >= 2) { "Block size must be at least two" }
    }

    /** A ranked host, with the score and room it is ranked on. */
    private class Entry(val host: SimHost, thresholds: Int) {
        val id = host.id
        var score = 0.0
        val available = DoubleArray(thresholds)

        /** The block holding this entry, or null if the host is not ranked. */
        var block: Block? = null
    }

    /** Consecutive entries of the ranking, with the most room any of them has for each threshold filter. */
    private class Block(capacity: Int, thresholds: Int) {
        val entries = arrayOfNulls<Entry>(capacity)
        var size = 0
        val maxAvailable = DoubleArray(thresholds) { Double.NEGATIVE_INFINITY }

        fun last(): Entry = entries[size - 1]!!
    }

    private val thresholds = filters.filterIsInstance<ThresholdFilter>()

    private val maxBlockSize = 2 * blockSize
    private val minBlockSize = maxOf(1, blockSize / 4)

    private val blocks = ArrayList<Block>()

    /** The entry of every host ever ranked, at the index of its [SimHost.id]. */
    private var entryOf = arrayOfNulls<Entry>(numHosts)

    /** The room the task being placed needs, for each threshold filter. */
    private val required = DoubleArray(thresholds.size)

    private val comparator =
        Comparator<Entry> { a, b ->
            val result = b.score.compareTo(a.score)
            if (result != 0) result else a.id.compareTo(b.id)
        }

    /** The number of ranked hosts. */
    var size: Int = 0
        private set

    /** Rank [host] at [score] and its current room, or move it there if it is already ranked. */
    fun put(
        host: SimHost,
        score: Double,
    ) {
        val entry = find(host) ?: newEntry(host)
        if (entry.block != null) {
            removeEntry(entry)
        }

        entry.score = score
        for (i in thresholds.indices) {
            entry.available[i] = thresholds[i].available(host)
        }
        insertEntry(entry)
    }

    fun remove(host: SimHost) {
        val entry = find(host) ?: return
        if (entry.block != null) {
            removeEntry(entry)
        }
    }

    /** Give every ranked host the score [score] computes for it, and rank them again. */
    fun rescore(score: (SimHost) -> Double) {
        val entries = ArrayList<Entry>(size)
        for (block in blocks) {
            for (i in 0 until block.size) {
                entries.add(block.entries[i]!!)
            }
        }

        for (entry in entries) {
            entry.score = score(entry.host)
        }
        entries.sortWith(comparator)
        rebuild(entries)
    }

    /** The highest-ranked host that passes every filter for [task], or null if none does. */
    fun firstFit(task: SimTask): SimHost? {
        for (i in thresholds.indices) {
            required[i] = thresholds[i].required(task)
        }

        for (b in blocks.indices) {
            val block = blocks[b]
            if (!hasRoom(block.maxAvailable)) {
                continue
            }

            for (i in 0 until block.size) {
                val entry = block.entries[i]!!
                if (hasRoom(entry.available) && filters.all { it.test(entry.host, task) }) {
                    return entry.host
                }
            }
        }
        return null
    }

    /** The ranked hosts, from the highest score to the lowest. */
    fun hosts(): List<SimHost> = blocks.flatMap { block -> (0 until block.size).map { block.entries[it]!!.host } }

    /** Check that the ranking is sorted and that every block is consistent with its entries. */
    fun checkInvariants() {
        var count = 0
        var previous: Entry? = null
        for (block in blocks) {
            check(block.size in 1..maxBlockSize) { "A block holds ${block.size} hosts" }
            check(blocks.size == 1 || block.size >= minBlockSize) { "A block holds ${block.size} hosts" }

            val max = DoubleArray(thresholds.size) { Double.NEGATIVE_INFINITY }
            for (i in 0 until block.size) {
                val entry = block.entries[i]!!
                check(entry.block === block) { "Host ${entry.id} points to the wrong block" }
                check(
                    previous == null || comparator.compare(previous, entry) < 0,
                ) { "Hosts ${previous?.id} and ${entry.id} are out of order" }
                for (f in max.indices) {
                    if (entry.available[f] > max[f]) {
                        max[f] = entry.available[f]
                    }
                }
                previous = entry
                count++
            }
            check((block.size until block.entries.size).all { block.entries[it] == null }) { "A block keeps removed hosts" }
            check(max.contentEquals(block.maxAvailable)) { "The room of a block is out of date" }
        }
        check(count == size) { "The blocks hold $count hosts, but the size is $size" }
    }

    /**
     * Whether [available] meets the [required] room for every threshold filter. A NaN is never ruled out here; by the
     * contract of [ThresholdFilter], the filter itself rejects it.
     */
    private fun hasRoom(available: DoubleArray): Boolean {
        for (i in required.indices) {
            if (available[i] < required[i]) {
                return false
            }
        }
        return true
    }

    private fun find(host: SimHost): Entry? {
        val entry = entryOf.getOrNull(host.id) ?: return null
        check(entry.host === host) { "Hosts ${entry.host.name} and ${host.name} have the same id ${host.id}" }
        return entry
    }

    private fun newEntry(host: SimHost): Entry {
        val id = host.id
        require(id >= 0) { "Host ${host.name} has a negative id $id" }
        if (id >= entryOf.size) {
            entryOf = entryOf.copyOf(maxOf(id + 1, entryOf.size * 2))
        }

        val entry = Entry(host, thresholds.size)
        entryOf[id] = entry
        return entry
    }

    private fun insertEntry(entry: Entry) {
        if (blocks.isEmpty()) {
            blocks.add(Block(maxBlockSize, thresholds.size))
        }

        var index = blockIndexFor(entry)
        var block = blocks[index]
        if (block.size == maxBlockSize) {
            split(index)
            if (comparator.compare(entry, block.last()) > 0) {
                index++
                block = blocks[index]
            }
        }

        val position = -search(block, entry) - 1
        System.arraycopy(block.entries, position, block.entries, position + 1, block.size - position)
        block.entries[position] = entry
        block.size++
        entry.block = block
        size++

        for (i in thresholds.indices) {
            if (entry.available[i] > block.maxAvailable[i]) {
                block.maxAvailable[i] = entry.available[i]
            }
        }
    }

    private fun removeEntry(entry: Entry) {
        val block = entry.block!!
        val position = search(block, entry)
        check(position >= 0 && block.entries[position] === entry) { "Host ${entry.host.name} is not at its ranked position" }

        System.arraycopy(block.entries, position + 1, block.entries, position, block.size - position - 1)
        block.size--
        block.entries[block.size] = null
        entry.block = null
        size--

        if (block.size == 0) {
            blocks.remove(block)
            return
        }

        if (thresholds.indices.any { entry.available[it] >= block.maxAvailable[it] }) {
            updateMax(block)
        }
        if (block.size < minBlockSize && blocks.size > 1) {
            merge(blocks.indexOf(block))
        }
    }

    /** The index of the block [entry] belongs in: the first block whose last entry does not come before it. */
    private fun blockIndexFor(entry: Entry): Int {
        var low = 0
        var high = blocks.size - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            if (comparator.compare(blocks[mid].last(), entry) < 0) low = mid + 1 else high = mid
        }
        return low
    }

    /** The position of [entry] in [block], or `-(insertion point) - 1` if it is not in it. */
    private fun search(
        block: Block,
        entry: Entry,
    ): Int {
        var low = 0
        var high = block.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val result = comparator.compare(block.entries[mid]!!, entry)
            when {
                result < 0 -> low = mid + 1
                result > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return -(low + 1)
    }

    /** Move the second half of the block at [index] into a new block after it. */
    private fun split(index: Int) {
        val block = blocks[index]
        val half = block.size / 2
        val next = Block(maxBlockSize, thresholds.size)

        System.arraycopy(block.entries, half, next.entries, 0, block.size - half)
        next.size = block.size - half
        block.entries.fill(null, half, block.size)
        block.size = half

        for (i in 0 until next.size) {
            next.entries[i]!!.block = next
        }
        updateMax(block)
        updateMax(next)
        blocks.add(index + 1, next)
    }

    /** Merge the small block at [index] with its smaller neighbor, or share their hosts evenly if they do not fit one block. */
    private fun merge(index: Int) {
        val left =
            when {
                index == 0 -> 0
                index == blocks.size - 1 -> index - 1
                blocks[index - 1].size <= blocks[index + 1].size -> index - 1
                else -> index
            }
        val first = blocks[left]
        val second = blocks[left + 1]
        val total = first.size + second.size

        val entries = arrayOfNulls<Entry>(total)
        System.arraycopy(first.entries, 0, entries, 0, first.size)
        System.arraycopy(second.entries, 0, entries, first.size, second.size)

        if (total <= maxBlockSize) {
            fill(first, entries, 0, total)
            blocks.removeAt(left + 1)
        } else {
            fill(first, entries, 0, total / 2)
            fill(second, entries, total / 2, total)
        }
    }

    /** Make [block] hold [entries] from [from] until [to]. */
    private fun fill(
        block: Block,
        entries: Array<Entry?>,
        from: Int,
        to: Int,
    ) {
        System.arraycopy(entries, from, block.entries, 0, to - from)
        block.entries.fill(null, to - from, block.entries.size)
        block.size = to - from
        for (i in 0 until block.size) {
            block.entries[i]!!.block = block
        }
        updateMax(block)
    }

    /** Split the sorted [entries] over new blocks of about [blockSize] hosts each. */
    private fun rebuild(entries: List<Entry>) {
        blocks.clear()
        if (entries.isEmpty()) {
            return
        }

        val count = (entries.size + blockSize - 1) / blockSize
        var start = 0
        for (b in 0 until count) {
            val end = (entries.size.toLong() * (b + 1) / count).toInt()
            val block = Block(maxBlockSize, thresholds.size)
            for (i in start until end) {
                block.entries[block.size++] = entries[i]
                entries[i].block = block
            }
            updateMax(block)
            blocks.add(block)
            start = end
        }
    }

    private fun updateMax(block: Block) {
        block.maxAvailable.fill(Double.NEGATIVE_INFINITY)
        for (i in 0 until block.size) {
            val available = block.entries[i]!!.available
            for (f in available.indices) {
                if (available[f] > block.maxAvailable[f]) {
                    block.maxAvailable[f] = available[f]
                }
            }
        }
    }
}
