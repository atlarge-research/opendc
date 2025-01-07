package org.opendc.simulator.compute.workload.trace.scaling;

/**
 * The NoDelay scaling policy states that there will be no delay
 * when less CPU can be provided than needed.
 *
 * This could be used in situations where the data is streamed.
 * This will also result in the same behaviour as older OpenDC.
 */
public class NoDelayScaling implements ScalingPolicy {
    @Override
    public double getFinishedWork(double cpuFreqDemand, double cpuFreqSupplied, long passedTime) {
        return cpuFreqDemand * passedTime;
    }

    @Override
    public long getRemainingDuration(double cpuFreqDemand, double cpuFreqSupplied, double remainingWork) {
        return (long) (remainingWork / cpuFreqDemand);
    }

    @Override
    public double getRemainingWork(double cpuFreqDemand, long duration) {
        return cpuFreqDemand * duration;
    }
}
