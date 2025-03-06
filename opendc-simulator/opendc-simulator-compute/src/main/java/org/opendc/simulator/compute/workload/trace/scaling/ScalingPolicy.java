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
 * Interface for the scaling policy.
 * A scaling decides how a TaskFragment should scale when it is not getting the demanded capacity
 */
public interface ScalingPolicy {

    /**
     * Calculate how much work was finished based on the demanded and supplied cpu
     *
     * @param cpuFreqDemand
     * @param cpuFreqSupplied
     * @param passedTime
     * @return
     */
    double getFinishedWork(double cpuFreqDemand, double cpuFreqSupplied, long passedTime);

    /**
     * Calculate the remaining duration of this fragment based on the demanded and supplied cpu
     *
     * @param cpuFreqDemand
     * @param cpuFreqSupplied
     * @param remainingWork
     * @return
     */
    long getRemainingDuration(double cpuFreqDemand, double cpuFreqSupplied, double remainingWork);

    /**
     * Calculate how much work is remaining based on the demanded and supplied cpu
     *
     * @param cpuFreqDemand
     * @param duration
     * @return
     */
    double getRemainingWork(double cpuFreqDemand, long duration);
}
