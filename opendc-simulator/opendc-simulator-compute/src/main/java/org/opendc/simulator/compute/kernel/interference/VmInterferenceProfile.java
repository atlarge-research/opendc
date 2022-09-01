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

/**
 * A profile of a particular virtual machine describing its interference pattern with other virtual machines.
 */
public final class VmInterferenceProfile {
    private final VmInterferenceModel model;
    private final int id;
    private final int[] membership;
    private final int[][] members;
    private final double[] targets;
    private final double[] scores;

    /**
     * Construct a {@link VmInterferenceProfile}.
     */
    VmInterferenceProfile(
            VmInterferenceModel model, int id, int[] membership, int[][] members, double[] targets, double[] scores) {
        this.model = model;
        this.id = id;
        this.membership = membership;
        this.members = members;
        this.targets = targets;
        this.scores = scores;
    }

    /**
     * Create a new {@link VmInterferenceMember} based on this profile for the specified <code>domain</code>.
     */
    VmInterferenceMember newMember(VmInterferenceDomain domain) {
        return new VmInterferenceMember(domain, model, id, membership, members, targets, scores);
    }

    @Override
    public String toString() {
        return "VmInterferenceProfile[id=" + id + "]";
    }
}
