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

package org.opendc.simulator.compute.workload;

import java.util.ArrayList;
import java.util.List;
import org.opendc.simulator.engine.FlowSupplier;

public class TraceWorkload implements Workload {
    private ArrayList<TraceFragment> fragments;
    private final long checkpointInterval;
    private final long checkpointDuration;
    private final double checkpointIntervalScaling;

    public TraceWorkload(
            ArrayList<TraceFragment> fragments,
            long checkpointInterval,
            long checkpointDuration,
            double checkpointIntervalScaling) {
        this.fragments = fragments;
        this.checkpointInterval = checkpointInterval;
        this.checkpointDuration = checkpointDuration;
        this.checkpointIntervalScaling = checkpointIntervalScaling;
    }

    public TraceWorkload(ArrayList<TraceFragment> fragments) {
        this(fragments, 0L, 0L, 1.0);
    }

    public ArrayList<TraceFragment> getFragments() {
        return fragments;
    }

    @Override
    public long getCheckpointInterval() {
        return checkpointInterval;
    }

    @Override
    public long getCheckpointDuration() {
        return checkpointDuration;
    }

    @Override
    public double getCheckpointIntervalScaling() {
        return checkpointIntervalScaling;
    }

    public void removeFragments(int numberOfFragments) {
        if (numberOfFragments <= 0) {
            return;
        }
        this.fragments.subList(0, numberOfFragments).clear();
    }

    public void addFirst(TraceFragment fragment) {
        this.fragments.add(0, fragment);
    }

    @Override
    public SimWorkload startWorkload(FlowSupplier supplier, long now) {
        return new SimTraceWorkload(supplier, this, now);
    }

    public static Builder builder() {
        return builder(0L, 0L, 0.0);
    }

    public static Builder builder(long checkpointInterval, long checkpointDuration, double checkpointIntervalScaling) {
        return new Builder(checkpointInterval, checkpointDuration, checkpointIntervalScaling);
    }

    /**
     * Construct a {@link TraceWorkload} from the specified fragments.
     *
     * @param fragments The array of fragments to construct the trace from.
     */
    public static TraceWorkload ofFragments(TraceFragment... fragments) {
        final Builder builder = builder();

        for (TraceFragment fragment : fragments) {
            builder.add(fragment.duration(), fragment.cpuUsage(), fragment.coreCount());
        }

        return builder.build();
    }

    /**
     * Construct a {@link TraceWorkload} from the specified fragments.
     *
     * @param fragments The fragments to construct the trace from.
     */
    public static TraceWorkload ofFragments(List<TraceFragment> fragments) {
        final Builder builder = builder();

        for (TraceFragment fragment : fragments) {
            builder.add(fragment.duration(), fragment.cpuUsage(), fragment.coreCount());
        }

        return builder.build();
    }

    public static final class Builder {
        private final ArrayList<TraceFragment> fragments;
        private final long checkpointInterval;
        private final long checkpointDuration;
        private final double checkpointIntervalScaling;

        /**
         * Construct a new {@link Builder} instance.
         */
        private Builder(long checkpointInterval, long checkpointDuration, double checkpointIntervalScaling) {
            this.fragments = new ArrayList<>();
            this.checkpointInterval = checkpointInterval;
            this.checkpointDuration = checkpointDuration;
            this.checkpointIntervalScaling = checkpointIntervalScaling;
        }

        /**
         * Add a fragment to the trace.
         *
         * @param duration The timestamp at which the fragment ends (in epoch millis).
         * @param usage The CPU usage at this fragment.
         * @param cores The number of cores used during this fragment.
         */
        public void add(long duration, double usage, int cores) {
            fragments.add(fragments.size(), new TraceFragment(duration, usage, cores));
        }

        /**
         * Build the {@link TraceWorkload} instance.
         */
        public TraceWorkload build() {
            return new TraceWorkload(
                    this.fragments, this.checkpointInterval, this.checkpointDuration, this.checkpointIntervalScaling);
        }
    }
}
