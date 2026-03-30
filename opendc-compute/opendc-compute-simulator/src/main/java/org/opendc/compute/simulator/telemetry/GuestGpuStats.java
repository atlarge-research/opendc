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

package org.opendc.compute.simulator.telemetry;

/**
 * Statistics about the GPUs of a guest.
 *
 * @param activeTime The cumulative time (in seconds) that the GPUs of the guest were actively running.
 * @param idleTime The cumulative time (in seconds) the GPUs of the guest were idle.
 * @param stealTime The cumulative GPU time (in seconds) that the guest was ready to run, but not granted time by the host.
 * @param lostTime The cumulative GPU time (in seconds) that was lost due to interference with other machines.
 * @param capacity The available GPU capacity of the guest (in MHz).
 * @param usage Amount of GPU resources (in MHz) actually used by the guest.
 * @param utilization The utilization of the GPU resources (in %) relative to the total GPU capacity.
 */
public record GuestGpuStats(
        long activeTime,
        long idleTime,
        long stealTime,
        long lostTime,
        double capacity,
        double usage,
        double demand,
        double utilization) {}
