/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.compute.kernel.interference

import java.util.SplittableRandom

/**
 * A participant of an interference domain.
 */
public class VmInterferenceMember(
    private val domain: VmInterferenceDomain,
    private val model: VmInterferenceModel,
    @JvmField internal val id: Int,
    @JvmField internal val membership: IntArray,
    @JvmField internal val members: Array<IntArray>,
    private val targets: DoubleArray,
    private val scores: DoubleArray
) : Comparable<VmInterferenceMember> {
    /**
     * The active groups to which the key belongs.
     */
    private var groups: IntArray = IntArray(2)
    private var groupsSize: Int = 0

    /**
     * The number of users of the interference key.
     */
    private var refCount: Int = 0

    /**
     * Mark this member as active in this interference domain.
     */
    public fun activate() {
        if (refCount++ <= 0) {
            domain.activate(this)
        }
    }

    /**
     * Mark this member as inactive in this interference domain.
     */
    public fun deactivate() {
        if (--refCount <= 0) {
            domain.deactivate(this)
        }
    }

    /**
     * Compute the performance score of the member in this interference domain.
     *
     * @param random The source of randomness to apply when computing the performance score.
     * @param load The overall load on the interference domain.
     * @return A score representing the performance score to be applied to the member, with 1
     * meaning no influence, <1 means that performance degrades, and >1 means that performance improves.
     */
    public fun apply(random: SplittableRandom, load: Double): Double {
        val groupsSize = groupsSize

        if (groupsSize == 0) {
            return 1.0
        }

        val groups = groups
        val targets = targets

        var low = 0
        var high = groupsSize - 1
        var group = -1

        // Perform binary search over the groups based on target load
        while (low <= high) {
            val mid = low + high ushr 1
            val midGroup = groups[mid]
            val target = targets[midGroup]

            if (target < load) {
                low = mid + 1
                group = midGroup
            } else if (target > load) {
                high = mid - 1
            } else {
                group = midGroup
                break
            }
        }

        return if (group >= 0 && random.nextInt(members[group].size) == 0) {
            scores[group]
        } else {
            1.0
        }
    }

    /**
     * Add an active group to this member.
     */
    internal fun addGroup(group: Int) {
        var groups = groups
        val groupsSize = groupsSize
        val pos = groups.binarySearch(group, toIndex = groupsSize)

        if (pos >= 0) {
            return
        }

        val idx = -pos - 1

        if (groups.size == groupsSize) {
            val newSize = groupsSize + (groupsSize shr 1)
            groups = groups.copyOf(newSize)
            this.groups = groups
        }

        groups.copyInto(groups, idx + 1, idx, groupsSize)
        groups[idx] = group
        this.groupsSize += 1
    }

    /**
     * Remove an active group from this member.
     */
    internal fun removeGroup(group: Int) {
        val groups = groups
        val groupsSize = groupsSize
        val pos = groups.binarySearch(group, toIndex = groupsSize)

        if (pos < 0) {
            return
        }

        groups.copyInto(groups, pos, pos + 1, groupsSize)
        this.groupsSize -= 1
    }

    override fun compareTo(other: VmInterferenceMember): Int {
        val cmp = model.hashCode().compareTo(other.model.hashCode())
        if (cmp != 0) {
            return cmp
        }

        return id.compareTo(other.id)
    }
}
