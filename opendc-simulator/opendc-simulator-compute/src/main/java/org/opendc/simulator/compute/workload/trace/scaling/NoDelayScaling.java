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
 * The NoDelay scaling policy states that there will be no delay
 * when less CPU can be provided than needed.
 *
 * This could be used in situations where the data is streamed.
 * This will also result in the same behaviour as older OpenDC.
 */
public class NoDelayScaling implements ScalingPolicy {
    @Override
    public double getFinishedWork(double demand, double supplied, long passedTime) {
        return passedTime;
    }

    @Override
    public long getRemainingDuration(double demand, double supplied, double remainingWork) {
        return (long) remainingWork;
    }

    @Override
    public double getRemainingWork(double demand, long duration) {
        return duration;
    }
}
