/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.compute.power;

import java.util.List;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowNode;

public class CarbonModel extends FlowNode {

    private SimPowerSource powerSource;

    private long startTime = 0L; // The absolute timestamp on which the workload started

    private List<CarbonFragmentNew> fragments;
    private CarbonFragmentNew current_fragment;

    private int fragment_index;
    /**
     * Construct a new {@link FlowNode} instance.
     *
     * @param parentGraph The {@link FlowGraph} this stage belongs to.
     */
    public CarbonModel(
            FlowGraph parentGraph,
            SimPowerSource powerSource,
            List<CarbonFragmentNew> carbonFragments,
            long startTime) {
        super(parentGraph);

        this.powerSource = powerSource;
        this.startTime = startTime;
        this.fragments = carbonFragments;

        this.fragment_index = 0;
        this.current_fragment = this.fragments.get(this.fragment_index);
        this.pushCarbonIntensity(this.current_fragment.getCarbonIntensity());
    }

    public void close() {
        this.closeNode();
    }

    /**
     * Convert the given time to the absolute time by adding the start of workload
     *
     * @param time
     */
    private long getAbsoluteTime(long time) {
        return time + startTime;
    }

    private long getRelativeTime(long time) {
        return time - startTime;
    }

    private void findCorrectFragment(long absolute_time) {

        // Traverse to the previous fragment, until you reach the correct fragment
        while (absolute_time < this.current_fragment.getStartTime()) {
            this.current_fragment = fragments.get(--this.fragment_index);
        }

        // Traverse to the next fragment, until you reach the correct fragment
        while (absolute_time >= this.current_fragment.getEndTime()) {
            this.current_fragment = fragments.get(++this.fragment_index);
        }
    }

    @Override
    public long onUpdate(long now) {
        long absolute_time = getAbsoluteTime(now);

        // Check if the current fragment is still the correct fragment,
        // Otherwise, find the correct fragment.
        if ((absolute_time < current_fragment.getStartTime()) || (absolute_time >= current_fragment.getEndTime())) {
            this.findCorrectFragment(absolute_time);

            pushCarbonIntensity(current_fragment.getCarbonIntensity());
        }

        // Update again at the end of this fragment
        return getRelativeTime(current_fragment.getEndTime());
    }

    private void pushCarbonIntensity(double carbonIntensity) {
        this.powerSource.updateCarbonIntensity(carbonIntensity);
    }
}
