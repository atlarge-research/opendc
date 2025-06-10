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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.machine.SimMachine;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.Workload;
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class TraceWorkload implements Workload {
    private final ArrayList<TraceFragment> fragments;
    private final long checkpointInterval;
    private final long checkpointDuration;
    private final double checkpointIntervalScaling;
    private final double maxCpuDemand;
    private final int maxCpuCoreCount;
    private final double maxGpuDemand;
    private final int maxGpuCoreCount;
    private final long maxGpuMemoryDemand;
    private final String taskName;
    private ResourceType[] resourceTypes = new ResourceType[ResourceType.values().length];

    public ScalingPolicy getScalingPolicy() {
        return scalingPolicy;
    }

    private final ScalingPolicy scalingPolicy;

    public TraceWorkload(
            ArrayList<TraceFragment> fragments,
            long checkpointInterval,
            long checkpointDuration,
            double checkpointIntervalScaling,
            ScalingPolicy scalingPolicy,
            String taskName,
            ResourceType[] resourceTypes) {
        this.fragments = fragments;
        this.checkpointInterval = checkpointInterval;
        this.checkpointDuration = checkpointDuration;
        this.checkpointIntervalScaling = checkpointIntervalScaling;
        this.scalingPolicy = scalingPolicy;
        this.taskName = taskName;

        // TODO: remove if we decide not to use it.
        this.maxCpuDemand = fragments.stream()
                .max(Comparator.comparing(TraceFragment::cpuUsage))
                .get()
//                .cpuUsage();
                .getResourceUsage(ResourceType.CPU);
        this.maxCpuCoreCount = fragments.stream()
                .max(Comparator.comparing(TraceFragment::cpuCoreCount))
                .get()
//                .cpuCoreCount();
                .getCoreCount(ResourceType.CPU);

        this.maxGpuDemand = fragments.stream()
                .max(Comparator.comparing(TraceFragment::gpuUsage))
                .get()
                .getResourceUsage(ResourceType.GPU);
        this.maxGpuCoreCount = fragments.stream()
                .max(Comparator.comparing(TraceFragment::gpuCoreCount))
                .get()
                .getCoreCount(ResourceType.GPU);
        this.maxGpuMemoryDemand = 0L; // TODO: add GPU memory demand to the trace fragments

        this.resourceTypes = resourceTypes;

    }

    public ArrayList<TraceFragment> getFragments() {
        return fragments;
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

    public int getMaxCoreCount() {
        return maxCpuCoreCount;
    }

    public double getMaxCpuDemand() {
        return maxCpuDemand;
    }

    public double getMaxGpuDemand() { return maxGpuDemand; }
    public int getMaxGpuCoreCount() { return maxGpuCoreCount; }
    public long getMaxGpuMemoryDemand() { return maxGpuMemoryDemand; }

    public String getTaskName() { return taskName; }

    public void removeFragments(int numberOfFragments) {
        if (numberOfFragments <= 0) {
            return;
        }
        this.fragments.subList(0, numberOfFragments).clear();
    }

    public void addFirst(TraceFragment fragment) {
        this.fragments.addFirst(fragment);
    }

    public ResourceType[] getResourceTypes() {
        return Arrays.stream(resourceTypes).filter(Objects::nonNull).toArray(ResourceType[]::new);
    }

    @Override
    public SimWorkload startWorkload(FlowSupplier supplier) {
        return new SimTraceWorkload(supplier, this);
//        ArrayList<FlowSupplier> flowSuppliers = new ArrayList<>();
//        flowSuppliers.add(supplier);
//        return new SimTraceWorkload(flowSuppliers, this);
    }

    @Override
    public SimWorkload startWorkload(List<FlowSupplier> supplier, SimMachine machine, Consumer<Exception> completion) {
//        return this.startWorkload(supplier);
        return new SimTraceWorkload(supplier, this);
    }

    public static Builder builder(
            long checkpointInterval,
            long checkpointDuration,
            double checkpointIntervalScaling,
            ScalingPolicy scalingPolicy,
            String taskName) {
        return new Builder(checkpointInterval, checkpointDuration, checkpointIntervalScaling, scalingPolicy, taskName);
    }

    public static final class Builder {
        private final ArrayList<TraceFragment> fragments;
        private final long checkpointInterval;
        private final long checkpointDuration;
        private final double checkpointIntervalScaling;
        private final ScalingPolicy scalingPolicy;
        private final String taskName;
        private final ResourceType[] resourceTypes = new ResourceType[ResourceType.values().length];

        /**
         * Construct a new {@link Builder} instance.
         */
        private Builder(
                long checkpointInterval,
                long checkpointDuration,
                double checkpointIntervalScaling,
                ScalingPolicy scalingPolicy,
                String taskName) {
            this.fragments = new ArrayList<>();
            this.checkpointInterval = checkpointInterval;
            this.checkpointDuration = checkpointDuration;
            this.checkpointIntervalScaling = checkpointIntervalScaling;
            this.scalingPolicy = scalingPolicy;
            this.taskName = taskName;
        }

        /**
         * Add a fragment to the trace.
         *
         * @param duration The timestamp at which the fragment ends (in epoch millis).
         * @param cpuUsage The CPU usage at this fragment.
         * @param cpuCores The number of cores used during this fragment.
         * @param gpuUsage The GPU usage at this fragment.
         * @param gpuCores The number of GPU cores used during this fragment.
         * @param gpuMemoryUsage The GPU memory usage at this fragment.
         */
        public void add(long duration, double cpuUsage, int cpuCores,
                       double gpuUsage, int gpuCores, long gpuMemoryUsage) {
            if (cpuUsage > 0.0){
                this.resourceTypes[ResourceType.CPU.ordinal()] = ResourceType.CPU;
            }
            if (gpuUsage > 0.0){
                this.resourceTypes[ResourceType.GPU.ordinal()] = ResourceType.GPU;
            }
            fragments.add(fragments.size(), new TraceFragment(duration, cpuUsage, cpuCores, gpuUsage, gpuCores, gpuMemoryUsage));
        }

        /**
         * Build the {@link TraceWorkload} instance.
         */
        public TraceWorkload build() {
            return new TraceWorkload(
                    this.fragments,
                    this.checkpointInterval,
                    this.checkpointDuration,
                    this.checkpointIntervalScaling,
                    this.scalingPolicy,
                    this.taskName,
                    this.resourceTypes);
        }
    }
}
