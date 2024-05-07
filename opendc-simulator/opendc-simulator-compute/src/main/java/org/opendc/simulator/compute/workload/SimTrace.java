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

import java.util.Arrays;
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
    private final double[] usageCol;
    private final long[] deadlineCol;
    private final int[] coresCol;
    private final int size;

    /**
     * Construct a {@link SimTrace} instance.
     *
     * @param usageCol The column containing the CPU usage of each fragment (in MHz).
     * @param deadlineCol The column containing the ending timestamp for each fragment (in epoch millis).
     * @param coresCol The column containing the utilized cores.
     * @param size The number of fragments in the trace.
     */
    private SimTrace(double[] usageCol, long[] deadlineCol, int[] coresCol, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Invalid trace size");
        } else if (usageCol.length < size) {
            throw new IllegalArgumentException("Invalid number of usage entries");
        } else if (deadlineCol.length < size) {
            throw new IllegalArgumentException("Invalid number of deadline entries");
        } else if (coresCol.length < size) {
            throw new IllegalArgumentException("Invalid number of core entries");
        }

        this.usageCol = usageCol;
        this.deadlineCol = deadlineCol;
        this.coresCol = coresCol;
        this.size = size;
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
        return new Workload(start, usageCol, deadlineCol, coresCol, size, 0, checkpointTime, checkpointWait);
    }

    /**
     * Create a new {@link Builder} instance with the specified initial capacity.
     */
    public static Builder builder(int initialCapacity) {
        return new Builder(initialCapacity);
    }

    /**
     * Create a new {@link Builder} instance with a default initial capacity.
     */
    public static Builder builder() {
        return builder(256);
    }

    /**
     * Construct a {@link SimTrace} from the specified fragments.
     *
     * @param fragments The array of fragments to construct the trace from.
     */
    public static SimTrace ofFragments(SimTraceFragment... fragments) {
        final Builder builder = builder(fragments.length);

        for (SimTraceFragment fragment : fragments) {
            builder.add(fragment.timestamp + fragment.duration, fragment.usage, fragment.cores);
        }

        return builder.build();
    }

    /**
     * Construct a {@link SimTrace} from the specified fragments.
     *
     * @param fragments The fragments to construct the trace from.
     */
    public static SimTrace ofFragments(List<SimTraceFragment> fragments) {
        final Builder builder = builder(fragments.size());

        for (SimTraceFragment fragment : fragments) {
            builder.add(fragment.timestamp + fragment.duration, fragment.usage, fragment.cores);
        }

        return builder.build();
    }

    /**
     * Builder class for a {@link SimTrace}.
     */
    public static final class Builder {
        private double[] usageCol;
        private long[] deadlineCol;
        private int[] coresCol;

        private int size;
        private boolean isBuilt;

        /**
         * Construct a new {@link Builder} instance.
         */
        private Builder(int initialCapacity) {
            this.usageCol = new double[initialCapacity];
            this.deadlineCol = new long[initialCapacity];
            this.coresCol = new int[initialCapacity];
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

            int size = this.size;
            double[] usageCol = this.usageCol;

            if (size == usageCol.length) {
                grow();
                usageCol = this.usageCol;
            }

            deadlineCol[size] = deadline;
            usageCol[size] = usage;
            coresCol[size] = cores;

            this.size++;
        }

        /**
         * Build the {@link SimTrace} instance.
         */
        public SimTrace build() {
            isBuilt = true;
            return new SimTrace(usageCol, deadlineCol, coresCol, size);
        }

        /**
         * Helper method to grow the capacity of the trace.
         */
        private void grow() {
            int arraySize = usageCol.length;
            int newSize = arraySize + (arraySize >> 1);

            usageCol = Arrays.copyOf(usageCol, newSize);
            deadlineCol = Arrays.copyOf(deadlineCol, newSize);
            coresCol = Arrays.copyOf(coresCol, newSize);
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
            usageCol = usageCol.clone();
            deadlineCol = deadlineCol.clone();
            coresCol = coresCol.clone();
        }
    }

    /**
     * Implementation of {@link SimWorkload} that executes a trace.
     */
    private static class Workload implements SimWorkload {
        private WorkloadStageLogic logic;

        private long offset;

        private final long start;
        private final double[] usageCol;
        private final long[] deadlineCol;
        private final int[] coresCol;
        private final int size;
        private final int index;

        private long checkpointTime; // How long does it take to make a checkpoint?
        private long checkpointWait; // How long to wait until a new checkpoint is made?
        private long total_checks;

        private Workload(
                long start,
                double[] usageCol,
                long[] deadlineCol,
                int[] coresCol,
                int size,
                int index,
                long checkpointTime,
                long checkpointWait) {
            this.start = start;
            this.usageCol = usageCol;
            this.deadlineCol = deadlineCol;
            this.coresCol = coresCol;
            this.size = size;
            this.index = index;
            this.checkpointTime = checkpointTime;
            this.checkpointWait = checkpointWait;
        }

        @Override
        public void setOffset(long now) {
            this.offset = now - this.start;
        }

        @Override
        public void onStart(SimMachineContext ctx) {
            final WorkloadStageLogic logic;
            if (ctx.getCpus().size() == 1) {
                logic = new SingleWorkloadLogic(ctx, offset, usageCol, deadlineCol, size, index);
            } else {
                logic = new MultiWorkloadLogic(ctx, offset, usageCol, deadlineCol, coresCol, size, index);
            }
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
        public SimWorkload snapshot() {
            final WorkloadStageLogic logic = this.logic;
            int index = this.index;

            if (logic != null) {
                index = logic.getIndex();
            }

            return new Workload(start, usageCol, deadlineCol, coresCol, size, index, checkpointTime, checkpointWait);
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
        private int index;

        private final long workloadOffset;
        private final double[] cpuUsages;
        private final long[] deadlines;
        private final int traceSize;

        private final SimMachineContext ctx;

        private SingleWorkloadLogic(
                SimMachineContext ctx, long offset, double[] usageCol, long[] deadlineCol, int size, int index) {
            this.ctx = ctx;
            this.workloadOffset = offset;
            this.cpuUsages = usageCol;
            this.deadlines = deadlineCol;
            this.traceSize = size;
            this.index = index;

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
            long deadline = this.deadlines[this.index];

            // Loop through the deadlines until the next deadline is reached.
            while (deadline <= nowOffset) {
                if (++this.index >= this.traceSize) {
                    return doStop(ctx);
                }
                deadline = this.deadlines[this.index];
            }

            this.output.push((float) this.cpuUsages[this.index]);
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
        private int index;
        private final int coreCount;

        private final long offset;
        private final double[] usageCol;
        private final long[] deadlineCol;
        private final int[] coresCol;
        private final int traceSize;

        private final SimMachineContext ctx;

        private MultiWorkloadLogic(
                SimMachineContext ctx,
                long offset,
                double[] usageCol,
                long[] deadlineCol,
                int[] coresCol,
                int traceSize,
                int index) {
            this.ctx = ctx;
            this.offset = offset;
            this.usageCol = usageCol;
            this.deadlineCol = deadlineCol;
            this.coresCol = coresCol;
            this.traceSize = traceSize;
            this.index = index;

            final FlowGraph graph = ctx.getGraph();
            final List<? extends SimProcessingUnit> cpus = ctx.getCpus();

            stage = graph.newStage(this);
            coreCount = cpus.size();

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

            int index = this.index;

            long[] deadlines = deadlineCol;
            long deadline = deadlines[index];

            while (deadline <= nowOffset && ++index < this.traceSize) {
                deadline = deadlines[index];
            }

            if (index >= this.traceSize) {
                final SimMachineContext machineContext = this.ctx;
                if (machineContext != null) {
                    machineContext.shutdown();
                }
                ctx.close();
                return Long.MAX_VALUE;
            }

            this.index = index;

            int cores = Math.min(coreCount, coresCol[index]);
            float usage = (float) usageCol[index] / cores;

            final OutPort[] outputs = this.outputs;

            for (int i = 0; i < cores; i++) {
                outputs[i].push(usage);
            }

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
