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

import java.util.Objects;

/**
 * A fragment of the workload trace.
 */
public final class SimTraceFragment {
    final long timestamp;
    final long duration;
    final double usage;
    final int cores;

    /**
     * Construct a {@link SimTraceFragment}.
     *
     * @param timestamp The timestamp at which the fragment starts (in epoch millis).
     * @param duration The duration of the fragment (in milliseconds).
     * @param usage The CPU usage during the fragment (in MHz).
     * @param cores The amount of cores utilized during the fragment.
     */
    public SimTraceFragment(long timestamp, long duration, double usage, int cores) {
        this.timestamp = timestamp;
        this.duration = duration;
        this.usage = usage;
        this.cores = cores;
    }

    /**
     * Return the timestamp at which the fragment starts (in epoch millis).
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Return the duration of the fragment (in milliseconds).
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Return the CPU usage during the fragment (in MHz).
     */
    public double getUsage() {
        return usage;
    }

    /**
     * Return the amount of cores utilized during the fragment.
     */
    public int getCores() {
        return cores;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimTraceFragment that = (SimTraceFragment) o;
        return timestamp == that.timestamp
                && duration == that.duration
                && Double.compare(that.usage, usage) == 0
                && cores == that.cores;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, duration, usage, cores);
    }
}
