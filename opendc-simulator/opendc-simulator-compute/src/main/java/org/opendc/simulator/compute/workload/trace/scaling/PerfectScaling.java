package org.opendc.simulator.compute.workload.trace.scaling;

/**
 * PerfectScaling scales the workload duration perfectly
 * based on the CPU capacity.
 *
 * This means that if a fragment has a duration of 10 min at 4000 mHz,
 * it will take 20 min and 2000 mHz.
 */
public class PerfectScaling implements ScalingPolicy {
    @Override
    public double getFinishedWork(double cpuFreqDemand, double cpuFreqSupplied, long passedTime) {
        return cpuFreqSupplied * passedTime;
    }

    @Override
    public long getRemainingDuration(double cpuFreqDemand, double cpuFreqSupplied, double remainingWork) {
        return (long) (remainingWork / cpuFreqSupplied);
    }

    @Override
    public double getRemainingWork(double cpuFreqDemand, long duration) {
        return cpuFreqDemand * duration;
    }
}
