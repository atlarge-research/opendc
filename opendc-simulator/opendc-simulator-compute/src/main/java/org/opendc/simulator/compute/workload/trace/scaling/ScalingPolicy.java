package org.opendc.simulator.compute.workload.trace.scaling;

public interface ScalingPolicy {

    double getFinishedWork(double cpuFreqDemand, double cpuFreqSupplied, long passedTime);

    long getRemainingDuration(double cpuFreqDemand, double cpuFreqSupplied, double remainingWork);

    double getRemainingWork(double cpuFreqDemand, long duration);
}
