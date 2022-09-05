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

import java.util.*

/**
 * An interference model that models the resource interference between virtual machines on a host.
 *
 * @param targets The target load of each group.
 * @param scores The performance score of each group.
 * @param members The members belonging to each group.
 * @param membership The identifier of each key.
 * @param size The number of groups.
 * @param seed The seed to use for randomly selecting the virtual machines that are affected.
 */
public class VmInterferenceModel private constructor(
    private val idMapping: Map<String, Int>,
    private val members: Array<IntArray>,
    private val membership: Array<IntArray>,
    private val targets: DoubleArray,
    private val scores: DoubleArray,
    private val size: Int,
    seed: Long,
) {
    /**
     * A [SplittableRandom] used for selecting the virtual machines that are affected.
     */
    private val random = SplittableRandom(seed)

    /**
     * Construct a new [VmInterferenceDomain].
     */
    public fun newDomain(): VmInterferenceDomain = InterferenceDomainImpl(idMapping, members, membership, targets, scores, random)

    /**
     * Create a copy of this model with a different seed.
     */
    public fun withSeed(seed: Long): VmInterferenceModel {
        return VmInterferenceModel(idMapping, members, membership, targets, scores, size, seed)
    }

    public companion object {
        /**
         * Construct a [Builder] instance.
         */
        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Builder class for a [VmInterferenceModel]
     */
    public class Builder internal constructor() {
        /**
         * The initial capacity of the builder.
         */
        private val INITIAL_CAPACITY = 256

        /**
         * The target load of each group.
         */
        private var _targets = DoubleArray(INITIAL_CAPACITY) { Double.POSITIVE_INFINITY }

        /**
         * The performance score of each group.
         */
        private var _scores = DoubleArray(INITIAL_CAPACITY) { Double.POSITIVE_INFINITY }

        /**
         * The members of each group.
         */
        private var _members = ArrayList<Set<String>>(INITIAL_CAPACITY)

        /**
         * The mapping from member to group id.
         */
        private val ids = TreeSet<String>()

        /**
         * The number of groups in the model.
         */
        private var size = 0

        /**
         * Add the specified group to the model.
         */
        public fun addGroup(members: Set<String>, targetLoad: Double, score: Double): Builder {
            val size = size

            if (size == _targets.size) {
                grow()
            }

            _targets[size] = targetLoad
            _scores[size] = score
            _members.add(members)
            ids.addAll(members)

            this.size++

            return this
        }

        /**
         * Build the [VmInterferenceModel].
         */
        public fun build(seed: Long = 0): VmInterferenceModel {
            val size = size
            val targets = _targets
            val scores = _scores
            val members = _members

            val indices = Array(size) { it }
            indices.sortWith(
                Comparator { l, r ->
                    var cmp = targets[l].compareTo(targets[r]) // Order by target load
                    if (cmp != 0) {
                        return@Comparator cmp
                    }

                    cmp = scores[l].compareTo(scores[r]) // Higher penalty first (this means lower performance score first)
                    if (cmp != 0)
                        cmp
                    else
                        l.compareTo(r)
                }
            )

            val newTargets = DoubleArray(size)
            val newScores = DoubleArray(size)
            val newMembers = arrayOfNulls<IntArray>(size)

            var nextId = 0
            val idMapping = ids.associateWith { nextId++ }
            val membership = ids.associateWithTo(TreeMap()) { ArrayList<Int>() }

            for ((group, j) in indices.withIndex()) {
                newTargets[group] = targets[j]
                newScores[group] = scores[j]
                val groupMembers = members[j]
                val newGroupMembers = groupMembers.map { idMapping.getValue(it) }.toIntArray()

                newGroupMembers.sort()
                newMembers[group] = newGroupMembers

                for (member in groupMembers) {
                    membership.getValue(member).add(group)
                }
            }

            @Suppress("UNCHECKED_CAST")
            return VmInterferenceModel(
                idMapping,
                newMembers as Array<IntArray>,
                membership.map { it.value.toIntArray() }.toTypedArray(),
                newTargets,
                newScores,
                size,
                seed
            )
        }

        /**
         * Helper function to grow the capacity of the internal arrays.
         */
        private fun grow() {
            val oldSize = _targets.size
            val newSize = oldSize + (oldSize shr 1)

            _targets = _targets.copyOf(newSize)
            _scores = _scores.copyOf(newSize)
        }
    }

    /**
     * Internal implementation of [VmInterferenceDomain].
     */
    private class InterferenceDomainImpl(
        private val idMapping: Map<String, Int>,
        private val members: Array<IntArray>,
        private val membership: Array<IntArray>,
        private val targets: DoubleArray,
        private val scores: DoubleArray,
        private val random: SplittableRandom
    ) : VmInterferenceDomain {
        /**
         * Keys registered with this domain.
         */
        private val keys = HashMap<Int, InterferenceMemberImpl>()

        /**
         * The set of keys active in this domain.
         */
        private val activeKeys = ArrayList<InterferenceMemberImpl>()

        /**
         * Queue of participants that will be removed or added to the active groups.
         */
        private val participants = ArrayDeque<InterferenceMemberImpl>()

        override fun getMember(id: String): VmInterferenceMember? {
            val intId = idMapping[id] ?: return null
            return keys.computeIfAbsent(intId) { InterferenceMemberImpl(it, this, members, targets, scores, random) }
        }

        override fun toString(): String = "VmInterferenceDomain"

        fun join(key: InterferenceMemberImpl) {
            val activeKeys = activeKeys
            val pos = activeKeys.binarySearch(key)
            if (pos < 0) {
                activeKeys.add(-pos - 1, key)
            }

            computeActiveGroups(activeKeys, key.id)
        }

        fun leave(key: InterferenceMemberImpl) {
            val activeKeys = activeKeys
            activeKeys.remove(key)
            computeActiveGroups(activeKeys, key.id)
        }

        /**
         * (Re-)compute the active groups.
         */
        private fun computeActiveGroups(activeKeys: ArrayList<InterferenceMemberImpl>, id: Int) {
            if (activeKeys.isEmpty()) {
                return
            }

            val groups = membership[id]
            val members = members
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
                        participants.add(rightEntry)
                        intersection++

                        i++
                        j++
                    }
                }

                while (true) {
                    val participant = participants.poll() ?: break
                    val participantGroups = participant.groups
                    if (intersection <= 1) {
                        participantGroups.remove(group)
                    } else {
                        val pos = participantGroups.binarySearch(group)
                        if (pos < 0) {
                            participantGroups.add(-pos - 1, group)
                        }
                    }
                }
            }
        }
    }

    /**
     * An interference key.
     *
     * @param id The identifier of the member.
     */
    private class InterferenceMemberImpl(
        @JvmField val id: Int,
        private val domain: InterferenceDomainImpl,
        private val members: Array<IntArray>,
        private val targets: DoubleArray,
        private val scores: DoubleArray,
        private val random: SplittableRandom
    ) : VmInterferenceMember, Comparable<InterferenceMemberImpl> {
        /**
         * The active groups to which the key belongs.
         */
        @JvmField val groups: MutableList<Int> = ArrayList()

        /**
         * The number of users of the interference key.
         */
        private var refCount: Int = 0

        override fun activate() {
            if (refCount++ <= 0) {
                domain.join(this)
            }
        }

        override fun deactivate() {
            if (--refCount <= 0) {
                domain.leave(this)
            }
        }

        override fun apply(load: Double): Double {
            val groups = groups
            val groupSize = groups.size

            if (groupSize == 0) {
                return 1.0
            }

            val targets = targets
            val scores = scores
            var low = 0
            var high = groups.size - 1

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

        override fun compareTo(other: InterferenceMemberImpl): Int = id.compareTo(other.id)
    }
}
