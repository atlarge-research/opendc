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

import java.util.TreeMap
import java.util.TreeSet

/**
 * An interference model that models the resource interference between virtual machines on a host.
 *
 * @param members The target load of each group.
 * @param scores The performance score of each group.
 * @param members The members belonging to each group.
 * @param membership The identifier of each key.
 * @param size The number of groups.
 */
public class VmInterferenceModel private constructor(
    private val idMapping: Map<String, Int>,
    private val members: Array<IntArray>,
    private val membership: Array<IntArray>,
    private val targets: DoubleArray,
    private val scores: DoubleArray,
    private val size: Int
) {
    /**
     * Return the [VmInterferenceProfile] associated with the specified [id].
     *
     * @param id The identifier of the virtual machine.
     * @return A [VmInterferenceProfile] representing the virtual machine as part of interference model or `null` if
     *         there is no profile for the virtual machine.
     */
    public fun getProfile(id: String): VmInterferenceProfile? {
        val intId = idMapping[id] ?: return null
        return VmInterferenceProfile(this, intId, membership[intId], members, targets, scores)
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
        public fun build(): VmInterferenceModel {
            val size = size
            val targets = _targets
            val scores = _scores
            val members = _members

            val indices = IntArray(size) { it }
            indices.sortedWith(
                Comparator { l, r ->
                    var cmp = targets[l].compareTo(targets[r]) // Order by target load
                    if (cmp != 0) {
                        return@Comparator cmp
                    }

                    cmp = scores[l].compareTo(scores[r]) // Higher penalty first (this means lower performance score first)
                    if (cmp != 0) {
                        cmp
                    } else {
                        l.compareTo(r)
                    }
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
                size
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

        private companion object {
            /**
             * The initial capacity of the builder.
             */
            const val INITIAL_CAPACITY = 256
        }
    }
}
