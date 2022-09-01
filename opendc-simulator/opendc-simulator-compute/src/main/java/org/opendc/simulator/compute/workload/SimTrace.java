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
     * @param offset The offset for the timestamps.
     */
    public SimWorkload createWorkload(long offset) {
        return new Workload(offset, usageCol, deadlineCol, coresCol, size);
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
    private static class Workload implements SimWorkload, FlowStageLogic {
        private SimMachineContext ctx;
        private FlowStage stage;
        private OutPort[] outputs;
        private int index;
        private int coreCount;

        private final long offset;
        private final double[] usageCol;
        private final long[] deadlineCol;
        private final int[] coresCol;
        private final int size;

        private Workload(long offset, double[] usageCol, long[] deadlineCol, int[] coresCol, int size) {
            this.offset = offset;
            this.usageCol = usageCol;
            this.deadlineCol = deadlineCol;
            this.coresCol = coresCol;
            this.size = size;
        }

        @Override
        public void onStart(SimMachineContext ctx) {
            this.ctx = ctx;

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
        public void onStop(SimMachineContext ctx) {
            this.ctx = null;

            final FlowStage stage = this.stage;

            if (stage != null) {
                this.stage = null;
                stage.close();
            }
        }

        @Override
        public long onUpdate(FlowStage ctx, long now) {
            int size = this.size;
            long offset = this.offset;
            long nowOffset = now - offset;

            int index = this.index;

            long[] deadlines = deadlineCol;
            long deadline = deadlines[index];

            while (deadline <= nowOffset && ++index < size) {
                deadline = deadlines[index];
            }

            if (index >= size) {
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
    }
}
