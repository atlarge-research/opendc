/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.compute.machine;

public class CpuPerformanceCounters {
    private long cpuActiveTime = 0;
    private long cpuIdleTime = 0;
    private long cpuStealTime = 0;
    private long cpuLostTime = 0;

    private double cpuCapacity = 0.0f;
    private double cpuDemand = 0.0f;
    private double cpuSupply = 0.0f;

    public long getCpuActiveTime() {
        return cpuActiveTime;
    }

    public void setCpuActiveTime(long cpuActiveTime) {
        this.cpuActiveTime = cpuActiveTime;
    }

    public void addCpuActiveTime(long cpuActiveTime) {
        this.cpuActiveTime += cpuActiveTime;
    }

    public long getCpuIdleTime() {
        return cpuIdleTime;
    }

    public void setCpuIdleTime(long cpuIdleTime) {
        this.cpuIdleTime = cpuIdleTime;
    }

    public void addCpuIdleTime(long cpuIdleTime) {
        this.cpuIdleTime += cpuIdleTime;
    }

    public long getCpuStealTime() {
        return cpuStealTime;
    }

    public void setCpuStealTime(long cpuStealTime) {
        this.cpuStealTime = cpuStealTime;
    }

    public void addCpuStealTime(long cpuStealTime) {
        this.cpuStealTime += cpuStealTime;
    }

    public long getCpuLostTime() {
        return cpuLostTime;
    }

    public void setCpuLostTime(long cpuLostTime) {
        this.cpuLostTime = cpuLostTime;
    }

    public double getCpuCapacity() {
        return cpuCapacity;
    }

    public void setCpuCapacity(double cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    public double getCpuDemand() {
        return cpuDemand;
    }

    public void setCpuDemand(double cpuDemand) {
        this.cpuDemand = cpuDemand;
    }

    public double getCpuSupply() {
        return cpuSupply;
    }

    public void setCpuSupply(double cpuSupply) {
        this.cpuSupply = cpuSupply;
    }
}
