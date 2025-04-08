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
import java.util.Comparator;
import java.util.function.Consumer;
import org.opendc.simulator.compute.machine.SimMachine;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.Workload;
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class TraceWorkload implements Workload {
    private final ArrayList<TraceFragment> fragments;
    private final ArrayList<TraceFragment> accelFragments;
    private final long checkpointInterval;
    private final long checkpointDuration;
    private final double checkpointIntervalScaling;
    private final double maxCpuDemand;
    private final int maxCoreCount;

    public String getTaskName() {
        return taskName;
    }

    private final String taskName;

    public ScalingPolicy getScalingPolicy() {
        return scalingPolicy;
    }

    private final ScalingPolicy scalingPolicy;

    public TraceWorkload(
            ArrayList<TraceFragment> fragments,
            ArrayList<TraceFragment> accelFragments,
            long checkpointInterval,
            long checkpointDuration,
            double checkpointIntervalScaling,
            ScalingPolicy scalingPolicy,
            String taskName) {
        this.fragments = fragments;
        this.accelFragments = accelFragments;
        this.checkpointInterval = checkpointInterval;
        this.checkpointDuration = checkpointDuration;
        this.checkpointIntervalScaling = checkpointIntervalScaling;
        this.scalingPolicy = scalingPolicy;
        this.taskName = taskName;

        // TODO: remove if we decide not to use it.
        this.maxCpuDemand = fragments.stream()
                .max(Comparator.comparing(TraceFragment::cpuUsage))
                .map(TraceFragment::cpuUsage)
                .orElse(0.0);
        this.maxCoreCount = fragments.stream()
                .max(Comparator.comparing(TraceFragment::coreCount))
                .map(TraceFragment::coreCount)
                .orElse(0);
    }

    public ArrayList<TraceFragment> getFragments() {
        return fragments;
    }

    public ArrayList<TraceFragment> getAccelFragments() {
        return accelFragments;
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
        return maxCoreCount;
    }

    public double getMaxCpuDemand() {
        return maxCpuDemand;
    }

    public void removeFragments(int numberOfFragments) {
        if (numberOfFragments <= 0) {
            return;
        }
        this.fragments.subList(0, numberOfFragments).clear();
    }

    public void addFirst(TraceFragment fragment) {
        this.fragments.addFirst(fragment);
    }

    public void removeAccelFragments(int numberOfFragments) {
        if (numberOfFragments <= 0) {
            return;
        }
        this.accelFragments.subList(0, numberOfFragments).clear();
    }

    public void addFirstAccel(TraceFragment fragment) {
        this.accelFragments.addFirst(fragment);
    }

    @Override
    public SimWorkload startWorkload(FlowSupplier supplier, FlowSupplier accelSupplier) {
        return new SimTraceWorkload(supplier, accelSupplier, this);
    }

    @Override
    public SimWorkload startWorkload(
            FlowSupplier supplier, FlowSupplier accelSupplier, SimMachine machine, Consumer<Exception> completion) {
        return this.startWorkload(supplier, accelSupplier);
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
        private final ArrayList<TraceFragment> accelFragments;
        private final long checkpointInterval;
        private final long checkpointDuration;
        private final double checkpointIntervalScaling;
        private final ScalingPolicy scalingPolicy;
        private final String taskName;

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
            this.accelFragments = new ArrayList<>();
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
         * @param usage The CPU usage at this fragment.
         * @param cores The number of cores used during this fragment.
         */
        public void add(long duration, double usage, int cores, boolean isGpu) {
            if (isGpu) {
                accelFragments.add(new TraceFragment(duration, usage, cores));
            } else {
                fragments.add(new TraceFragment(duration, usage, cores));
            }
        }

        /**
         * Build the {@link TraceWorkload} instance.
         */
        public TraceWorkload build() {
            return new TraceWorkload(
                    this.fragments,
                    this.accelFragments,
                    this.checkpointInterval,
                    this.checkpointDuration,
                    this.checkpointIntervalScaling,
                    this.scalingPolicy,
                    this.taskName);
        }
    }
}
