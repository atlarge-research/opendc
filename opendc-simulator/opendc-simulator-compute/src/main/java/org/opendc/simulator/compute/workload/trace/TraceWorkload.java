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

package org.opendc.simulator.compute.workload.trace;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.machine.SimMachine;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.Workload;
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class TraceWorkload implements Workload {
    private final long checkpointInterval;
    private final long checkpointDuration;
    private final double checkpointIntervalScaling;
    private final double maxCpuDemand;
    private final double maxGpuDemand;
    private final int maxGpuMemoryDemand;
    private final int taskId;

    public boolean[] getUsedResourceTypes() {
        return usedResourceTypes;
    }

    private final boolean[] usedResourceTypes;

    public long checkpointDelay = 0;
    public long failureDelay = 0;

    private final long[] durations;
    private final double[] cpuUsages;
    private final double[] gpuUsages;
    private final int[] gpuMemoryUsages;

    public int getLength() {
        return length;
    }

    private final int length;

    private int startingIndex = 0;

    public ScalingPolicy getScalingPolicy() {
        return scalingPolicy;
    }

    private final ScalingPolicy scalingPolicy;

    public TraceWorkload(
            long[] durations,
            double[] cpuUsages,
            double[] gpuUsages,
            int[] gpuMemoryUsages,
            double maxCpuDemand,
            double maxGpuDemand,
            long checkpointInterval,
            long checkpointDuration,
            double checkpointIntervalScaling,
            ScalingPolicy scalingPolicy,
            int taskId,
            boolean[] usedResourceTypes) {
        this.durations = durations;
        this.cpuUsages = cpuUsages;
        this.gpuUsages = gpuUsages;
        this.gpuMemoryUsages = gpuMemoryUsages;

        this.length = durations.length;

        this.checkpointInterval = checkpointInterval;
        this.checkpointDuration = checkpointDuration;
        this.checkpointIntervalScaling = checkpointIntervalScaling;
        this.scalingPolicy = scalingPolicy;
        this.taskId = taskId;

        // TODO: remove if we decide not to use it.
        this.maxCpuDemand = maxCpuDemand;
        this.maxGpuDemand = maxGpuDemand;
        this.maxGpuMemoryDemand = 0; // TODO: add GPU memory demand to the trace fragments

        this.usedResourceTypes = usedResourceTypes;
    }

    /**
     * Build and return a [TraceFragment] at the given index
     *
     * @param index index of the fragment in the workload
     * @return
     */
    public TraceFragment getFragment(int index) {
        double gpuUsage = 0.0;
        int gpuMemoryUsage = 0;

        if (this.usedResourceTypes[ResourceType.GPU.ordinal()]) {
            gpuUsage = this.gpuUsages[index];
            gpuMemoryUsage = this.gpuMemoryUsages[index];
        }

        return new TraceFragment(this.durations[index], this.cpuUsages[index], gpuUsage, gpuMemoryUsage);
    }

    /**
     * Check if the workload has a fragment at the given index
     *
     * @param index
     * @return
     */
    public boolean hasFragmentAt(int index) {
        return index < this.durations.length;
    }

    /**
     * Update the values of the fragment at the given index
     *
     * @param index
     * @param duration
     * @param cpuUsage
     * @param gpuUsage
     * @param gpuMemoryUsage
     */
    public void updateFragment(int index, long duration, double cpuUsage, double gpuUsage, int gpuMemoryUsage) {
        this.durations[index] = duration;
        this.cpuUsages[index] = cpuUsage;
        this.gpuUsages[index] = gpuUsage;
        this.gpuMemoryUsages[index] = gpuMemoryUsage;
    }

    @Override
    public long checkpointInterval() {
        return checkpointInterval;
    }

    @Override
    public long checkpointDuration() {
        return checkpointDuration;
    }

    @Override
    public double checkpointIntervalScaling() {
        return checkpointIntervalScaling;
    }

    public double getMaxCpuDemand() {
        return maxCpuDemand;
    }

    public double getMaxGpuDemand() {
        return maxGpuDemand;
    }

    public int getMaxGpuMemoryDemand() {
        return maxGpuMemoryDemand;
    }

    public int getTaskId() {
        return taskId;
    }

    public long failureDelay() {
        return failureDelay;
    }

    public long checkpointDelay() {
        return checkpointDelay;
    }

    @Override
    public SimWorkload startWorkload(FlowSupplier supplier) {
        return new SimTraceWorkload(supplier, this);
    }

    @Override
    public SimWorkload startWorkload(List<FlowSupplier> supplier, SimMachine machine, Consumer<Exception> completion) {
        return new SimTraceWorkload(supplier, this);
    }

    public static EfficientBuilder efficientBuilder(
            long checkpointInterval,
            long checkpointDuration,
            double checkpointIntervalScaling,
            ScalingPolicy scalingPolicy,
            int taskId,
            int numFragments) {
        return new EfficientBuilder(checkpointInterval, checkpointDuration, checkpointIntervalScaling, scalingPolicy, taskId, numFragments);
    }

    public static Builder builder(
        long checkpointInterval,
        long checkpointDuration,
        double checkpointIntervalScaling,
        ScalingPolicy scalingPolicy,
        int taskId) {
        return new Builder(checkpointInterval, checkpointDuration, checkpointIntervalScaling, scalingPolicy, taskId);
    }

    public int getStartingIndex() {
        return startingIndex;
    }

    public void setStartingIndex(int startingIndex) {
        this.startingIndex = startingIndex;
    }

    public static final class Builder {
        private static final int INITIAL_CAPACITY = 8;

        private final long checkpointInterval;
        private final long checkpointDuration;
        private final double checkpointIntervalScaling;
        private final ScalingPolicy scalingPolicy;
        private final int taskId;
        private final boolean[] usedResourceTypes = new boolean[ResourceType.values().length];

        private long[] durations;
        private double[] cpuUsages;
        private double[] gpuUsages;
        private int[] gpuMemoryUsages;
        private int size = 0;

        private double maxCpuDemand = 0.0;
        private double maxGpuDemand = 0.0;

        /**
         * Construct a new {@link Builder} instance.
         */
        private Builder(
                long checkpointInterval,
                long checkpointDuration,
                double checkpointIntervalScaling,
                ScalingPolicy scalingPolicy,
                int taskId) {
            this.checkpointInterval = checkpointInterval;
            this.checkpointDuration = checkpointDuration;
            this.checkpointIntervalScaling = checkpointIntervalScaling;
            this.scalingPolicy = scalingPolicy;
            this.taskId = taskId;

            this.usedResourceTypes[ResourceType.CPU.ordinal()] = true;

            this.durations = new long[INITIAL_CAPACITY];
            this.cpuUsages = new double[INITIAL_CAPACITY];
            this.gpuUsages = new double[INITIAL_CAPACITY];
            this.gpuMemoryUsages = new int[INITIAL_CAPACITY];
        }

        /**
         * Add a fragment to the trace.
         *
         * @param duration The timestamp at which the fragment ends (in epoch millis).
         * @param cpuUsage The CPU usage at this fragment.
         * @param gpuUsage The GPU usage at this fragment.
         * @param gpuMemoryUsage The GPU memory usage at this fragment.
         */
        public void add(long duration, double cpuUsage, double gpuUsage, int gpuMemoryUsage) {
            if (gpuUsage > 0.0) {
                this.usedResourceTypes[ResourceType.GPU.ordinal()] = true;
            }

            if (cpuUsage > maxCpuDemand) {
                maxCpuDemand = cpuUsage;
            }

            if (gpuUsage > maxGpuDemand) {
                maxGpuDemand = gpuUsage;
            }

            if (size == durations.length) {
                grow();
            }

            durations[size] = duration;
            cpuUsages[size] = cpuUsage;
            gpuUsages[size] = gpuUsage;
            gpuMemoryUsages[size] = gpuMemoryUsage;
            size++;
        }

        /**
         * Grow the backing arrays by 1.5x (the same growth factor {@code java.util.ArrayList} uses),
         * avoiding the boxing overhead of storing fragments in {@code ArrayList<Long>}/{@code Double}/{@code Integer}.
         */
        private void grow() {
            int newCapacity = durations.length + (durations.length >> 1);

            durations = Arrays.copyOf(durations, newCapacity);
            cpuUsages = Arrays.copyOf(cpuUsages, newCapacity);
            gpuUsages = Arrays.copyOf(gpuUsages, newCapacity);
            gpuMemoryUsages = Arrays.copyOf(gpuMemoryUsages, newCapacity);
        }

        /**
         * Build the {@link TraceWorkload} instance.
         */
        public TraceWorkload build() {
            long[] duration_array = Arrays.copyOf(durations, size);
            double[] cpuUsage_array = Arrays.copyOf(cpuUsages, size);

            double[] gpuUsage_array = null;
            int[] gpuMemoryUsage_array = null;
            if (this.usedResourceTypes[ResourceType.GPU.ordinal()]) {
                gpuUsage_array = Arrays.copyOf(gpuUsages, size);
                gpuMemoryUsage_array = Arrays.copyOf(gpuMemoryUsages, size);
            }

            return new TraceWorkload(
                    duration_array,
                    cpuUsage_array,
                    gpuUsage_array,
                    gpuMemoryUsage_array,
                    maxCpuDemand,
                    maxGpuDemand,
                    this.checkpointInterval,
                    this.checkpointDuration,
                    this.checkpointIntervalScaling,
                    this.scalingPolicy,
                    this.taskId,
                    this.usedResourceTypes);
        }
    }

    public static final class EfficientBuilder {
        private final long checkpointInterval;
        private final long checkpointDuration;
        private final double checkpointIntervalScaling;
        private final ScalingPolicy scalingPolicy;
        private final int taskId;
        private final boolean[] usedResourceTypes = new boolean[ResourceType.values().length];

        private final long[] durations;
        private final double[] cpuUsages;
        private double[] gpuUsages;
        private int[] gpuMemoryUsages;

        private final int numFragments;
        private int size = 0;

        private double maxCpuDemand = 0.0;
        private double maxGpuDemand = 0.0;

        /**
         * Construct a new {@link Builder} instance.
         */
        private EfficientBuilder(
            long checkpointInterval,
            long checkpointDuration,
            double checkpointIntervalScaling,
            ScalingPolicy scalingPolicy,
            int taskId,
            int numFragments) {
            this.checkpointInterval = checkpointInterval;
            this.checkpointDuration = checkpointDuration;
            this.checkpointIntervalScaling = checkpointIntervalScaling;
            this.scalingPolicy = scalingPolicy;
            this.taskId = taskId;

            this.usedResourceTypes[ResourceType.CPU.ordinal()] = true;

            this.numFragments = numFragments;
            this.durations = new long[numFragments];
            this.cpuUsages = new double[numFragments];
            this.gpuUsages = new double[numFragments];
            this.gpuMemoryUsages = new int[numFragments];
        }

        /**
         * Add a fragment to the trace.
         *
         * @param duration The timestamp at which the fragment ends (in epoch millis).
         * @param cpuUsage The CPU usage at this fragment.
         * @param gpuUsage The GPU usage at this fragment.
         * @param gpuMemoryUsage The GPU memory usage at this fragment.
         */
        public void add(long duration, double cpuUsage, double gpuUsage, int gpuMemoryUsage) {
            if (gpuUsage > 0.0) {
                this.usedResourceTypes[ResourceType.GPU.ordinal()] = true;
            }

            if (cpuUsage > maxCpuDemand) {
                maxCpuDemand = cpuUsage;
            }

            if (gpuUsage > maxGpuDemand) {
                maxGpuDemand = gpuUsage;
            }

            durations[size] = duration;
            cpuUsages[size] = cpuUsage;
            gpuUsages[size] = gpuUsage;
            gpuMemoryUsages[size] = gpuMemoryUsage;
            size++;
        }

        /**
         * Build the {@link TraceWorkload} instance.
         */
        public TraceWorkload build() throws Exception {
            if (size != numFragments) {
                throw new Exception("The number of Fragments does not match the actual number of values");
            }

            if (!this.usedResourceTypes[ResourceType.GPU.ordinal()]) {
                gpuUsages = null;
                gpuMemoryUsages = null;
            }

            return new TraceWorkload(
                durations,
                cpuUsages,
                gpuUsages,
                gpuMemoryUsages,
                maxCpuDemand,
                maxGpuDemand,
                this.checkpointInterval,
                this.checkpointDuration,
                this.checkpointIntervalScaling,
                this.scalingPolicy,
                this.taskId,
                this.usedResourceTypes);
        }
    }
}
