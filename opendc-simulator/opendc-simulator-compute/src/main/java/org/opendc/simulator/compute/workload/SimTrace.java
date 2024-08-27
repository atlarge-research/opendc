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
        return createWorkload(start, 0, 0);
    }

    /**
     * Construct a {@link SimWorkload} for this trace.
     *
     * //     * @param offset The offset for the timestamps.
     */
    public SimWorkload createWorkload(long start, long checkpointTime, long checkpointWait) {
        return new Workload(start, fragments, checkpointTime, checkpointWait);
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
            builder.add(fragment.deadline(), fragment.cpuUsage(), fragment.coreCount());
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
            builder.add(fragment.deadline(), fragment.cpuUsage(), fragment.coreCount());
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
         * @param deadline The timestamp at which the fragment ends (in epoch millis).
         * @param usage The CPU usage at this fragment.
         * @param cores The number of cores used during this fragment.
         */
        public void add(long deadline, double usage, int cores) {
            if (isBuilt) {
                recreate();
            }

            fragments.add(new SimTraceFragment(deadline, usage, cores));
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
        private final ArrayDeque<SimTraceFragment> fragments;

        private long checkpointTime; // How long does it take to make a checkpoint
        private long checkpointWait; // How long to wait until a new checkpoint is made

        private Workload(long start, ArrayDeque<SimTraceFragment> fragments, long checkpointTime, long checkpointWait) {
            this.start = start;
            this.checkpointTime = checkpointTime;
            this.checkpointWait = checkpointWait;

            this.fragments = fragments;
        }

        @Override
        public void setOffset(long now) {
            this.offset = now - this.start;
        }

        @Override
        public void onStart(SimMachineContext ctx) {
            final WorkloadStageLogic logic;
            if (ctx.getCpus().size() == 1) {
                logic = new SingleWorkloadLogic(ctx, offset, fragments.iterator());
            } else {
                logic = new MultiWorkloadLogic(ctx, offset, fragments.iterator());
            }
            this.logic = logic;
        }

        public void injectFragment(long duration, double usage, int coreCount) {}

        @Override
        public void onStop(SimMachineContext ctx) {
            final WorkloadStageLogic logic = this.logic;

            if (logic != null) {
                this.logic = null;
                logic.getStage().close();
            }
        }

        @Override
        public SimWorkload snapshot() {
            final WorkloadStageLogic logic = this.logic;

            if (logic != null) {
                int index = logic.getIndex();

                for (int i = 0; i < index; i++) {
                    this.fragments.removeFirst();
                }
            }

            return new Workload(start, this.fragments, checkpointTime, checkpointWait);
        }
    }

    /**
     * Interface to represent the {@link FlowStage} that simulates the trace workload.
     */
    private interface WorkloadStageLogic extends FlowStageLogic {
        /**
         * Return the {@link FlowStage} belonging to this instance.
         */
        FlowStage getStage();

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

        private final long workloadOffset;
        private final SimMachineContext ctx;

        private final Iterator<SimTraceFragment> fragments;
        private SimTraceFragment currentFragment;

        private SingleWorkloadLogic(SimMachineContext ctx, long offset, Iterator<SimTraceFragment> fragments) {
            this.ctx = ctx;
            this.workloadOffset = offset;
            this.fragments = fragments;
            this.currentFragment = this.fragments.next();

            final FlowGraph graph = ctx.getGraph();
            final List<? extends SimProcessingUnit> cpus = ctx.getCpus();

            stage = graph.newStage(this);

            final SimProcessingUnit cpu = cpus.get(0);
            final OutPort output = stage.getOutlet("cpu");
            this.output = output;

            graph.connect(output, cpu.getInput());
        }

        @Override
        public long onUpdate(FlowStage ctx, long now) {

            // Shift the current time to align with the starting time of the workload
            long nowOffset = now - this.workloadOffset;

            long deadline = currentFragment.deadline();

            // Loop through the deadlines until the next deadline is reached.
            while (deadline <= nowOffset) {
                if (!this.fragments.hasNext()) {
                    return doStop(ctx);
                }

                this.index++;
                currentFragment = this.fragments.next();
                deadline = currentFragment.deadline();
            }

            this.output.push((float) currentFragment.cpuUsage());
            return deadline + this.workloadOffset;
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

    /**
     * Implementation of {@link FlowStageLogic} for multiple CPUs.
     */
    private static class MultiWorkloadLogic implements WorkloadStageLogic {
        private final FlowStage stage;
        private final OutPort[] outputs;
        private int index = 0;

        private final int coreCount;
        private final long offset;

        private final Iterator<SimTraceFragment> fragments;
        private SimTraceFragment currentFragment;

        private final SimMachineContext ctx;

        private MultiWorkloadLogic(SimMachineContext ctx, long offset, Iterator<SimTraceFragment> fragments) {
            this.ctx = ctx;
            this.offset = offset;
            this.fragments = fragments;
            this.currentFragment = this.fragments.next();

            final FlowGraph graph = ctx.getGraph();
            final List<? extends SimProcessingUnit> cpus = ctx.getCpus();

            stage = graph.newStage(this);
            this.coreCount = cpus.size();

            final OutPort[] outputs = new OutPort[cpus.size()];
            this.outputs = outputs;

            for (int i = 0; i < cpus.size(); i++) {
                final SimProcessingUnit cpu = cpus.get(i);
                final OutPort output = stage.getOutlet("cpu" + i);

                graph.connect(output, cpu.getInput());
                outputs[i] = output;
            }
        }

        @Override
        public long onUpdate(FlowStage ctx, long now) {
            long offset = this.offset;
            long nowOffset = now - offset;

            long deadline = currentFragment.deadline();

            while (deadline <= nowOffset) {
                if (!this.fragments.hasNext()) {
                    final SimMachineContext machineContext = this.ctx;
                    if (machineContext != null) {
                        machineContext.shutdown();
                    }
                    ctx.close();
                    return Long.MAX_VALUE;
                }

                this.index++;
                currentFragment = this.fragments.next();
                deadline = currentFragment.deadline();
            }

            int cores = Math.min(this.coreCount, currentFragment.coreCount());
            float usage = (float) currentFragment.cpuUsage() / cores;

            final OutPort[] outputs = this.outputs;

            // Push the usage to all active cores
            for (int i = 0; i < cores; i++) {
                outputs[i].push(usage);
            }

            // Push a usage of 0 to all non-active cores
            for (int i = cores; i < outputs.length; i++) {
                outputs[i].push(0.f);
            }

            return deadline + offset;
        }

        @Override
        public FlowStage getStage() {
            return stage;
        }

        @Override
        public int getIndex() {
            return index;
        }
    }
}
