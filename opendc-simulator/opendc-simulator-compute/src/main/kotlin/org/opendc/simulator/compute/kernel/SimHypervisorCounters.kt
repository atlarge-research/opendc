/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.compute.kernel

/**
 * Performance counters of a [SimHypervisor].
 */
public interface SimHypervisorCounters {
    /**
     * The amount of time (in milliseconds) the CPUs of the hypervisor were actively running.
     */
    public val cpuActiveTime: Long

    /**
     * The amount of time (in milliseconds) the CPUs of the hypervisor were idle.
     */
    public val cpuIdleTime: Long

    /**
     * The amount of CPU time (in milliseconds) that virtual machines were ready to run, but were not able to.
     */
    public val cpuStealTime: Long

    /**
     * The amount of CPU time (in milliseconds) that was lost due to interference between virtual machines.
     */
    public val cpuLostTime: Long

    /**
     * Flush the counter values.
     */
    public fun flush()
}
