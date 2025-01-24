/*
 * Copyright (c) 2025 AtLarge Research
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
