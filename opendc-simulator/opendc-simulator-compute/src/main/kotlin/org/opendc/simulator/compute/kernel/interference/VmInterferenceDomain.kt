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

import java.util.ArrayDeque
import java.util.ArrayList
import java.util.WeakHashMap

/**
 * A domain where virtual machines may incur performance variability due to operating on the same resource and
 * therefore causing interference.
 */
public class VmInterferenceDomain {
    /**
     * A cache to maintain a mapping between the active profiles in this domain.
     */
    private val cache = WeakHashMap<VmInterferenceProfile, VmInterferenceMember>()

    /**
     * The set of members active in this domain.
     */
    private val activeKeys = ArrayList<VmInterferenceMember>()

    /**
     * Queue of participants that will be removed or added to the active groups.
     */
    private val participants = ArrayDeque<VmInterferenceMember>()

    /**
     * Join this interference domain with the specified [profile] and return the [VmInterferenceMember] associated with
     * the profile. If the member does not exist, it will be created.
     */
    public fun join(profile: VmInterferenceProfile): VmInterferenceMember {
        return cache.computeIfAbsent(profile) { key -> key.newMember(this) }
    }

    /**
     * Mark the specified [member] as active in this interference domain.
     */
    internal fun activate(member: VmInterferenceMember) {
        val activeKeys = activeKeys
        val pos = activeKeys.binarySearch(member)
        if (pos < 0) {
            activeKeys.add(-pos - 1, member)
        }

        computeActiveGroups(activeKeys, member)
    }

    /**
     * Mark the specified [member] as inactive in this interference domain.
     */
    internal fun deactivate(member: VmInterferenceMember) {
        val activeKeys = activeKeys
        activeKeys.remove(member)
        computeActiveGroups(activeKeys, member)
    }

    /**
     * (Re-)compute the active groups.
     */
    private fun computeActiveGroups(activeKeys: ArrayList<VmInterferenceMember>, member: VmInterferenceMember) {
        if (activeKeys.isEmpty()) {
            return
        }

        val groups = member.membership
        val members = member.members
        val participants = participants

        for (group in groups) {
            val groupMembers = members[group]

            var i = 0
            var j = 0
            var intersection = 0

            // Compute the intersection of the group members and the current active members
            while (i < groupMembers.size && j < activeKeys.size) {
                val l = groupMembers[i]
                val rightEntry = activeKeys[j]
                val r = rightEntry.id

                if (l < r) {
                    i++
                } else if (l > r) {
                    j++
                } else {
                    if (++intersection > 1) {
                        rightEntry.addGroup(group)
                    } else {
                        participants.add(rightEntry)
                    }

                    i++
                    j++
                }
            }

            while (true) {
                val participant = participants.poll() ?: break

                if (intersection <= 1) {
                    participant.removeGroup(group)
                } else {
                    participant.addGroup(group)
                }
            }
        }
    }
}
