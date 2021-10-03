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

package org.opendc.simulator.compute.kernel.interference

import org.opendc.simulator.flow.interference.InterferenceKey
import java.util.*

/**
 * An interference model that models the resource interference between virtual machines on a host.
 *
 * @param groups The groups of virtual machines that interfere with each other.
 * @param random The [Random] instance to select the affected virtual machines.
 */
public class VmInterferenceModel(
    private val groups: List<VmInterferenceGroup>,
    private val random: Random = Random(0)
) {
    /**
     * Construct a new [VmInterferenceDomain].
     */
    public fun newDomain(): VmInterferenceDomain = object : VmInterferenceDomain {
        /**
         * The stateful groups of this domain.
         */
        private val groups = this@VmInterferenceModel.groups.map { GroupContext(it) }

        /**
         * The set of keys active in this domain.
         */
        private val keys = mutableSetOf<InterferenceKeyImpl>()

        override fun join(id: String): InterferenceKey {
            val key = InterferenceKeyImpl(id, groups.filter { id in it }.sortedBy { it.group.targetLoad })
            keys += key
            return key
        }

        override fun leave(key: InterferenceKey) {
            if (key is InterferenceKeyImpl) {
                keys -= key
                key.leave()
            }
        }

        override fun apply(key: InterferenceKey?, load: Double): Double {
            if (key == null || key !is InterferenceKeyImpl) {
                return 1.0
            }

            val ctx = key.findGroup(load)
            val group = ctx?.group

            // Apply performance penalty to (on average) only one of the VMs
            return if (group != null && random.nextInt(group.members.size) == 0) {
                group.score
            } else {
                1.0
            }
        }

        override fun toString(): String = "VmInterferenceDomain"
    }

    /**
     * An interference key.
     *
     * @param id The identifier of the member.
     * @param groups The groups to which the key belongs.
     */
    private inner class InterferenceKeyImpl(val id: String, private val groups: List<GroupContext>) : InterferenceKey {
        init {
            for (group in groups) {
                group.join(this)
            }
        }

        /**
         * Find the active group that applies for the interference member.
         */
        fun findGroup(load: Double): GroupContext? {
            // Find the first active group whose target load is lower than the current load
            val index = groups.binarySearchBy(load) { it.group.targetLoad }
            val target = if (index >= 0) index else -(index + 1)

            // Check whether there are active groups ahead of the index
            for (i in target until groups.size) {
                val group = groups[i]
                if (group.group.targetLoad > load) {
                    break
                } else if (group.isActive) {
                    return group
                }
            }

            // Check whether there are active groups before the index
            for (i in (target - 1) downTo 0) {
                val group = groups[i]
                if (group.isActive) {
                    return group
                }
            }

            return null
        }

        /**
         * Leave all the groups.
         */
        fun leave() {
            for (group in groups) {
                group.leave(this)
            }
        }
    }

    /**
     * A group context is used to track the active keys per interference group.
     */
    private inner class GroupContext(val group: VmInterferenceGroup) {
        /**
         * The active keys that are part of this group.
         */
        private val keys = mutableSetOf<InterferenceKeyImpl>()

        /**
         * A flag to indicate that the group is active.
         */
        val isActive
            get() = keys.size > 1

        /**
         * Determine whether the specified [id] is part of this group.
         */
        operator fun contains(id: String): Boolean = id in group.members

        /**
         * Join this group with the specified [key].
         */
        fun join(key: InterferenceKeyImpl) {
            keys += key
        }

        /**
         * Leave this group with the specified [key].
         */
        fun leave(key: InterferenceKeyImpl) {
            keys -= key
        }
    }
}
