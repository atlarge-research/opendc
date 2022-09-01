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

package org.opendc.simulator.compute.kernel.interference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jetbrains.annotations.Nullable;

/**
 * An interference model that models the resource interference between virtual machines on a host.
 */
public final class VmInterferenceModel {
    private final Map<String, Integer> idMapping;
    private final int[][] members;
    private final int[][] membership;
    private final double[] targets;
    private final double[] scores;

    private VmInterferenceModel(
            Map<String, Integer> idMapping, int[][] members, int[][] membership, double[] targets, double[] scores) {
        this.idMapping = idMapping;
        this.members = members;
        this.membership = membership;
        this.targets = targets;
        this.scores = scores;
    }

    /**
     * Create a {@link Builder} for constructing a {@link VmInterferenceModel}.
     */
    public static Builder builder() {
        return new Builder(256);
    }

    /**
     * Return the {@link VmInterferenceProfile} associated with the specified <code>id</code>.
     *
     * @param id The identifier of the virtual machine.
     * @return A {@link VmInterferenceProfile} representing the virtual machine as part of interference model or
     * <code>null</code> if there is no profile for the virtual machine.
     */
    @Nullable
    public VmInterferenceProfile getProfile(String id) {
        Integer intId = idMapping.get(id);
        if (intId == null) {
            return null;
        }
        return new VmInterferenceProfile(this, intId, membership[intId], members, targets, scores);
    }

    /**
     * Builder class for a {@link VmInterferenceModel}.
     */
    public static final class Builder {
        private double[] targets;
        private double[] scores;
        private final ArrayList<Set<String>> members;
        private final TreeSet<String> ids;
        private int size;

        private Builder(int initialCapacity) {
            this.targets = new double[initialCapacity];
            this.scores = new double[initialCapacity];
            this.members = new ArrayList<>(initialCapacity);
            this.ids = new TreeSet<>();
        }

        /**
         * Add the specified group to the model.
         */
        public Builder addGroup(Set<String> members, double targetLoad, double score) {
            int size = this.size;

            if (size == targets.length) {
                grow();
            }

            targets[size] = targetLoad;
            scores[size] = score;
            this.members.add(members);
            ids.addAll(members);

            this.size++;

            return this;
        }

        /**
         * Build the {@link VmInterferenceModel}.
         */
        public VmInterferenceModel build() {
            int size = this.size;
            double[] targets = this.targets;
            double[] scores = this.scores;
            ArrayList<Set<String>> members = this.members;

            Integer[] indices = new Integer[size];
            Arrays.setAll(indices, (i) -> i);
            Arrays.sort(
                    indices,
                    Comparator.comparingDouble((Integer l) -> targets[l])
                            .thenComparingDouble(l -> scores[l])
                            .thenComparingInt(l -> l));

            double[] newTargets = new double[size];
            double[] newScores = new double[size];
            int[][] newMembers = new int[size][];

            int nextId = 0;

            Map<String, Integer> idMapping = new HashMap<>();
            TreeMap<String, ArrayList<Integer>> membership = new TreeMap<>();
            for (String id : ids) {
                idMapping.put(id, nextId++);
                membership.put(id, new ArrayList<>());
            }

            for (int group = 0; group < indices.length; group++) {
                int j = indices[group];
                newTargets[group] = targets[j];
                newScores[group] = scores[j];

                Set<String> groupMembers = members.get(j);
                int[] newGroupMembers = new int[groupMembers.size()];
                int k = 0;

                for (String groupMember : groupMembers) {
                    newGroupMembers[k++] = idMapping.get(groupMember);
                }

                Arrays.sort(newGroupMembers);
                newMembers[group] = newGroupMembers;

                for (String member : groupMembers) {
                    membership.get(member).add(group);
                }
            }

            int[][] newMembership = new int[membership.size()][];
            int k = 0;
            for (ArrayList<Integer> value : membership.values()) {
                newMembership[k++] = value.stream().mapToInt(i -> i).toArray();
            }

            return new VmInterferenceModel(idMapping, newMembers, newMembership, newTargets, newScores);
        }

        /**
         * Helper function to grow the capacity of the internal arrays.
         */
        private void grow() {
            int oldSize = targets.length;
            int newSize = oldSize + (oldSize >> 1);

            targets = Arrays.copyOf(targets, newSize);
            scores = Arrays.copyOf(scores, newSize);
        }
    }
}
