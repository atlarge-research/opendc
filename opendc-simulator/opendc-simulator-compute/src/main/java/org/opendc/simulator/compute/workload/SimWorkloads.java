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

import java.time.Duration;

/**
 * Helper methods for constructing {@link SimWorkload}s.
 */
public class SimWorkloads {
    private SimWorkloads() {}

    /**
     * Create a {@link SimWorkload} that executes a specified number of floating point operations (FLOPs) at the given
     * utilization.
     *
     * @param flops The number of floating point operations to perform for this task in MFLOPs.
     * @param utilization The CPU utilization of the workload.
     */
    public static SimWorkload flops(long flops, double utilization) {
        return new SimFlopsWorkload(flops, utilization);
    }

    /**
     * Create a {@link SimWorkload} that consumes the CPU resources for a specified duration at the given utilization.
     *
     * @param duration The duration of the workload in milliseconds.
     * @param utilization The CPU utilization of the workload.
     */
    public static SimWorkload runtime(long duration, double utilization) {
        return runtime(duration, utilization, 0, 0);
    }

    /**
     * Create a {@link SimWorkload} that consumes the CPU resources for a specified duration at the given utilization.
     *
     * @param duration The duration of the workload in milliseconds.
     * @param utilization The CPU utilization of the workload.
     */
    public static SimWorkload runtime(long duration, double utilization, long checkpoint_time, long checkpoint_wait) {
        return new SimRuntimeWorkload(duration, utilization, checkpoint_time, checkpoint_wait);
    }

    /**
     * Create a {@link SimWorkload} that consumes the CPU resources for a specified duration at the given utilization.
     *
     * @param duration The duration of the workload.
     * @param utilization The CPU utilization of the workload.
     */
    public static SimWorkload runtime(
            Duration duration, double utilization, long checkpoint_time, long checkpoint_wait) {
        return runtime(duration.toMillis(), utilization, checkpoint_time, checkpoint_wait);
    }

    /**
     * Chain the specified <code>workloads</code> into a single {@link SimWorkload}.
     */
    public static SimWorkload chain(SimWorkload... workloads) {
        return new SimChainWorkload(workloads);
    }
}
