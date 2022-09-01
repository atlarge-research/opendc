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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.WeakHashMap;

/**
 * A domain where virtual machines may incur performance variability due to operating on the same resource and
 * therefore causing interference.
 */
public final class VmInterferenceDomain {
    /**
     * A cache to maintain a mapping between the active profiles in this domain.
     */
    private final WeakHashMap<VmInterferenceProfile, VmInterferenceMember> cache = new WeakHashMap<>();

    /**
     * The set of members active in this domain.
     */
    private final ArrayList<VmInterferenceMember> activeKeys = new ArrayList<>();

    /**
     * Queue of participants that will be removed or added to the active groups.
     */
    private final ArrayDeque<VmInterferenceMember> participants = new ArrayDeque<>();

    /**
     * Join this interference domain with the specified <code>profile</code> and return the {@link VmInterferenceMember}
     * associated with the profile. If the member does not exist, it will be created.
     */
    public VmInterferenceMember join(VmInterferenceProfile profile) {
        return cache.computeIfAbsent(profile, (key) -> key.newMember(this));
    }

    /**
     * Mark the specified <code>member</code> as active in this interference domain.
     */
    void activate(VmInterferenceMember member) {
        final ArrayList<VmInterferenceMember> activeKeys = this.activeKeys;
        int pos = Collections.binarySearch(activeKeys, member);
        if (pos < 0) {
            activeKeys.add(-pos - 1, member);
        }

        computeActiveGroups(activeKeys, member);
    }

    /**
     * Mark the specified <code>member</code> as inactive in this interference domain.
     */
    void deactivate(VmInterferenceMember member) {
        final ArrayList<VmInterferenceMember> activeKeys = this.activeKeys;
        activeKeys.remove(member);
        computeActiveGroups(activeKeys, member);
    }

    /**
     * (Re-)compute the active groups.
     */
    private void computeActiveGroups(ArrayList<VmInterferenceMember> activeKeys, VmInterferenceMember member) {
        if (activeKeys.isEmpty()) {
            return;
        }

        final int[] groups = member.membership;
        final int[][] members = member.members;
        final ArrayDeque<VmInterferenceMember> participants = this.participants;

        for (int group : groups) {
            int[] groupMembers = members[group];

            int i = 0;
            int j = 0;
            int intersection = 0;

            // Compute the intersection of the group members and the current active members
            while (i < groupMembers.length && j < activeKeys.size()) {
                int l = groupMembers[i];
                final VmInterferenceMember rightEntry = activeKeys.get(j);
                int r = rightEntry.id;

                if (l < r) {
                    i++;
                } else if (l > r) {
                    j++;
                } else {
                    if (++intersection > 1) {
                        rightEntry.addGroup(group);
                    } else {
                        participants.add(rightEntry);
                    }

                    i++;
                    j++;
                }
            }

            while (true) {
                VmInterferenceMember participant = participants.poll();

                if (participant == null) {
                    break;
                }

                if (intersection <= 1) {
                    participant.removeGroup(group);
                } else {
                    participant.addGroup(group);
                }
            }
        }
    }
}
