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

package org.opendc.simulator.compute.workload;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import org.opendc.simulator.compute.SimMachineContext;
import org.opendc.simulator.compute.SimProcessingUnit;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.OutPort;

/**
 * A workload trace that describes the resource utilization over time in a collection of {@link SimTraceFragment}s.
 */
public final class SimTrace {
    private final ArrayDeque<SimTraceFragment> fragments;
    /**
     * Construct a {@link SimTrace} instance.
     *
     */
    private SimTrace(ArrayDeque<SimTraceFragment> fragments) {
        if (fragments.isEmpty()) {
            throw new IllegalArgumentException("No Fragments found for the Trace");
        }
        this.fragments = fragments;
    }

    /**
     * Construct a {@link SimWorkload} for this trace.
     *
     * //     * @param offset The offset for the timestamps.
     */
    public SimWorkload createWorkload(long start) {
        return createWorkload(start, 0, 0, 1);
    }

    /**
     * Construct a {@link SimWorkload} for this trace.
     *
     * //     * @param offset The offset for the timestamps.
     */
    public SimWorkload createWorkload(
            long start, long checkpointInterval, long checkpointDuration, double checkpointIntervalScaling) {
        return new Workload(start, fragments, checkpointInterval, checkpointDuration, checkpointIntervalScaling);
    }

    //    /**
    //     * Create a new {@link Builder} instance with a default initial capacity.
    //     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construct a {@link SimTrace} from the specified fragments.
     *
     * @param fragments The array of fragments to construct the trace from.
     */
    public static SimTrace ofFragments(SimTraceFragment... fragments) {
        final Builder builder = builder();

        for (SimTraceFragment fragment : fragments) {
            builder.add(fragment.duration(), fragment.cpuUsage(), fragment.coreCount());
        }

        return builder.build();
    }

    /**
     * Construct a {@link SimTrace} from the specified fragments.
     *
     * @param fragments The fragments to construct the trace from.
     */
    public static SimTrace ofFragments(List<SimTraceFragment> fragments) {
        final Builder builder = builder();

        for (SimTraceFragment fragment : fragments) {
            builder.add(fragment.duration(), fragment.cpuUsage(), fragment.coreCount());
        }

        return builder.build();
    }

    /**
     * Builder class for a {@link SimTrace}.
     */
    public static final class Builder {
        private final ArrayDeque<SimTraceFragment> fragments;

        private boolean isBuilt;

        /**
         * Construct a new {@link Builder} instance.
         */
        private Builder() {
            this.fragments = new ArrayDeque<>();
        }

        /**
         * Add a fragment to the trace.
         *
         * @param duration The timestamp at which the fragment ends (in epoch millis).
         * @param usage The CPU usage at this fragment.
         * @param cores The number of cores used during this fragment.
         */
        public void add(long duration, double usage, int cores) {
            if (isBuilt) {
                recreate();
            }

            fragments.add(new SimTraceFragment(duration, usage, cores));
        }

        /**
         * Build the {@link SimTrace} instance.
         */
        public SimTrace build() {
            isBuilt = true;
            return new SimTrace(fragments);
        }

        /**
         * Clone the columns of the trace.
         *
         * <p>
         * This is necessary when a {@link SimTrace} has been built already, but the user is again adding entries to
         * the builder.
         */
        private void recreate() {
            isBuilt = false;
            this.fragments.clear();
        }
    }

    /**
     * Implementation of {@link SimWorkload} that executes a trace.
     */
    private static class Workload implements SimWorkload {
        private WorkloadStageLogic logic;

        private long offset;

        private final long start;
        private ArrayDeque<SimTraceFragment> fragments;

        private long checkpointInterval; // How long to wait until a new checkpoint is made
        private long checkpointDuration; // How long does it take to make a checkpoint
        private double checkpointIntervalScaling;
        private SimWorkload snapshot;

        private Workload(
                long start,
                ArrayDeque<SimTraceFragment> fragments,
                long checkpointInterval,
                long checkpointDuration,
                double checkpointIntervalScaling) {
            this.start = start;
            this.checkpointInterval = checkpointInterval;
            this.checkpointDuration = checkpointDuration;
            this.checkpointIntervalScaling = checkpointIntervalScaling;

            this.fragments = fragments;

            this.snapshot = this;
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

        @Override
        public void setOffset(long now) {
            this.offset = now;
        }

        @Override
        public void onStart(SimMachineContext ctx) {
            final WorkloadStageLogic logic;
            logic = new SingleWorkloadLogic(ctx, offset, fragments.iterator());
            this.logic = logic;
        }

        @Override
        public void onStop(SimMachineContext ctx) {
            final WorkloadStageLogic logic = this.logic;

            if (logic != null) {
                this.logic = null;
                logic.getStage().close();
            }
        }

        @Override
        public void makeSnapshot(long now) {
            final WorkloadStageLogic logic = this.logic;
            final ArrayDeque<SimTraceFragment> newFragments = this.fragments;

            if (logic != null) {
                int index = logic.getIndex();

                if (index == 0 && (logic.getPassedTime(now) == 0)) {
                    this.snapshot = this;
                    return;
                }

                // Remove all finished fragments
                for (int i = 0; i < index; i++) {
                    newFragments.removeFirst();
                }
            } else {
                return;
            }

            // Reduce the current Fragment to a fragment with the remaining time.
            SimTraceFragment currentFragment = newFragments.pop();
            long passedTime = logic.getPassedTime(now);
            long remainingTime = currentFragment.duration() - passedTime;

            if (remainingTime > 0) {
                SimTraceFragment newFragment =
                        new SimTraceFragment(remainingTime, currentFragment.cpuUsage(), currentFragment.coreCount());

                newFragments.addFirst(newFragment);
            }

            // Add snapshot Fragment
            // TODO: improve CPUUsage and coreCount here
            SimTraceFragment snapshotFragment = new SimTraceFragment(checkpointDuration, 123456, 1);
            newFragments.addFirst(snapshotFragment);

            // Update the logic
            this.logic.updateFragments(newFragments.iterator(), now);

            // remove the snapshot Fragment and update fragments
            newFragments.removeFirst();
            this.fragments = newFragments;

            this.snapshot = new Workload(
                    start, this.fragments, checkpointInterval, checkpointDuration, checkpointIntervalScaling);
        }

        @Override
        public SimWorkload getSnapshot() {
            return this.snapshot;
        }

        @Override
        public void createCheckpointModel() {}
    }

    /**
     * Interface to represent the {@link FlowStage} that simulates the trace workload.
     */
    private interface WorkloadStageLogic extends FlowStageLogic {
        /**
         * Return the {@link FlowStage} belonging to this instance.
         */
        FlowStage getStage();

        long getPassedTime(long now);

        void updateFragments(Iterator<SimTraceFragment> newFragments, long offset);

        /**
         * Return the current index of the workload.
         */
        int getIndex();
    }

    /**
     * Implementation of {@link FlowStageLogic} for just a single CPU resource.
     */
    private static class SingleWorkloadLogic implements WorkloadStageLogic {
        private final FlowStage stage;
        private final OutPort output;
        private int index = 0;

        private final SimMachineContext ctx;

        private Iterator<SimTraceFragment> fragments;
        private SimTraceFragment currentFragment;
        private long startOffFragment;

        private SingleWorkloadLogic(SimMachineContext ctx, long offset, Iterator<SimTraceFragment> fragments) {
            this.ctx = ctx;

            this.fragments = fragments;

            final FlowGraph graph = ctx.getGraph();
            stage = graph.newStage(this);

            final SimProcessingUnit cpu = ctx.getCpu();
            final OutPort output = stage.getOutlet("cpu");
            this.output = output;

            graph.connect(output, cpu.getInput());

            // Start the first Fragment
            this.currentFragment = this.fragments.next();
            this.output.push((float) currentFragment.cpuUsage());
            this.startOffFragment = offset;
        }

        public long getPassedTime(long now) {
            return now - this.startOffFragment;
        }

        @Override
        public void updateFragments(Iterator<SimTraceFragment> newFragments, long offset) {
            this.fragments = newFragments;

            // Start the first Fragment
            this.currentFragment = this.fragments.next();
            this.output.push((float) currentFragment.cpuUsage());
            this.startOffFragment = offset;

            this.index = -1;

            this.stage.invalidate();
        }

        @Override
        public long onUpdate(FlowStage ctx, long now) {
            long passedTime = getPassedTime(now);
            long duration = this.currentFragment.duration();

            // The current Fragment has not yet been finished, continue
            if (passedTime < duration) {
                return now + (duration - passedTime);
            }

            // Loop through fragments until the passed time is filled.
            // We need a while loop to account for skipping of fragments.
            while (passedTime >= duration) {
                if (!this.fragments.hasNext()) {
                    return doStop(ctx);
                }

                passedTime = passedTime - duration;

                // get next Fragment
                this.index++;
                currentFragment = this.fragments.next();
                duration = currentFragment.duration();
            }

            // start new fragment
            this.startOffFragment = now - passedTime;

            // Change the cpu Usage to the new Fragment
            this.output.push((float) currentFragment.cpuUsage());

            // Return the time when the current fragment will complete
            return this.startOffFragment + duration;
        }

        @Override
        public FlowStage getStage() {
            return stage;
        }

        @Override
        public int getIndex() {
            return index;
        }

        /**
         * Helper method to stop the execution of the workload.
         */
        private long doStop(FlowStage ctx) {
            final SimMachineContext machineContext = this.ctx;
            if (machineContext != null) {
                machineContext.shutdown();
            }
            ctx.close();
            return Long.MAX_VALUE;
        }
    }
}
