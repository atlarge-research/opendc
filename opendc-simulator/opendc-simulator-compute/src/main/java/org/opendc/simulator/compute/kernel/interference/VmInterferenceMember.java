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

import java.util.Arrays;
import java.util.SplittableRandom;
import org.jetbrains.annotations.NotNull;

/**
 * A participant of an interference domain.
 */
public final class VmInterferenceMember implements Comparable<VmInterferenceMember> {
    private final VmInterferenceDomain domain;
    private final VmInterferenceModel model;
    final int id;
    final int[] membership;
    final int[][] members;
    private final double[] targets;
    private final double[] scores;

    private int[] groups = new int[2];
    private int groupsSize = 0;

    private int refCount = 0;

    VmInterferenceMember(
            VmInterferenceDomain domain,
            VmInterferenceModel model,
            int id,
            int[] membership,
            int[][] members,
            double[] targets,
            double[] scores) {
        this.domain = domain;
        this.model = model;
        this.id = id;
        this.membership = membership;
        this.members = members;
        this.targets = targets;
        this.scores = scores;
    }

    /**
     * Mark this member as active in this interference domain.
     */
    public void activate() {
        if (refCount++ <= 0) {
            domain.activate(this);
        }
    }

    /**
     * Mark this member as inactive in this interference domain.
     */
    public void deactivate() {
        if (--refCount <= 0) {
            domain.deactivate(this);
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
    public double apply(SplittableRandom random, double load) {
        int groupsSize = this.groupsSize;

        if (groupsSize == 0) {
            return 1.0;
        }

        int[] groups = this.groups;
        double[] targets = this.targets;

        int low = 0;
        int high = groupsSize - 1;
        int group = -1;

        // Perform binary search over the groups based on target load
        while (low <= high) {
            int mid = low + high >>> 1;
            int midGroup = groups[mid];
            double target = targets[midGroup];

            if (target < load) {
                low = mid + 1;
                group = midGroup;
            } else if (target > load) {
                high = mid - 1;
            } else {
                group = midGroup;
                break;
            }
        }

        if (group >= 0 && random.nextInt(members[group].length) == 0) {
            return scores[group];
        }

        return 1.0;
    }

    /**
     * Add an active group to this member.
     */
    void addGroup(int group) {
        int[] groups = this.groups;
        int groupsSize = this.groupsSize;
        int pos = Arrays.binarySearch(groups, 0, groupsSize, group);

        if (pos >= 0) {
            return;
        }

        int idx = -pos - 1;

        if (groups.length == groupsSize) {
            int newSize = groupsSize + (groupsSize >> 1);
            groups = Arrays.copyOf(groups, newSize);
            this.groups = groups;
        }

        System.arraycopy(groups, idx, groups, idx + 1, groupsSize - idx);
        groups[idx] = group;
        this.groupsSize += 1;
    }

    /**
     * Remove an active group from this member.
     */
    void removeGroup(int group) {
        int[] groups = this.groups;
        int groupsSize = this.groupsSize;
        int pos = Arrays.binarySearch(groups, 0, groupsSize, group);

        if (pos < 0) {
            return;
        }

        System.arraycopy(groups, pos + 1, groups, pos, groupsSize - pos - 1);
        this.groupsSize -= 1;
    }

    @Override
    public int compareTo(@NotNull VmInterferenceMember member) {
        int cmp = Integer.compare(model.hashCode(), member.model.hashCode());
        if (cmp != 0) {
            return cmp;
        }

        return Integer.compare(id, member.id);
    }
}
