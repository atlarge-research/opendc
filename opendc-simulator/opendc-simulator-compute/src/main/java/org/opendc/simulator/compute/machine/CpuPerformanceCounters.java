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

public class CpuPerformanceCounters implements ResourcePerformanceCounters {
    // public class CpuPerformanceCounters {
    private long cpuActiveTime = 0;
    private long cpuIdleTime = 0;
    private long cpuStealTime = 0;
    private long cpuLostTime = 0;

    private double cpuCapacity = 0.0f;
    private double cpuDemand = 0.0f;
    private double cpuSupply = 0.0f;

    @Override
    public long getActiveTime() {
        return this.cpuActiveTime;
    }

    @Override
    public long getIdleTime() {
        return this.cpuIdleTime;
    }

    @Override
    public long getStealTime() {
        return this.cpuStealTime;
    }

    @Override
    public long getLostTime() {
        return this.cpuLostTime;
    }

    @Override
    public double getCapacity() {
        return this.cpuCapacity;
    }

    @Override
    public double getDemand() {
        return this.cpuDemand;
    }

    @Override
    public double getSupply() {
        return this.cpuSupply;
    }

    @Override
    public void setActiveTime(long activeTime) {
        this.cpuActiveTime = activeTime;
    }

    @Override
    public void setIdleTime(long idleTime) {
        this.cpuIdleTime = idleTime;
    }

    @Override
    public void setStealTime(long stealTime) {
        this.cpuStealTime = stealTime;
    }

    @Override
    public void setLostTime(long lostTime) {
        this.cpuLostTime = lostTime;
    }

    @Override
    public void setCapacity(double capacity) {
        this.cpuCapacity = capacity;
    }

    @Override
    public void setDemand(double demand) {
        this.cpuDemand = demand;
    }

    @Override
    public void setSupply(double supply) {
        this.cpuSupply = supply;
    }

    @Override
    public void addActiveTime(long activeTime) {
        this.cpuActiveTime += activeTime;
    }

    @Override
    public void addIdleTime(long idleTime) {
        this.cpuIdleTime += idleTime;
    }

    @Override
    public void addStealTime(long stealTime) {
        this.cpuStealTime += stealTime;
    }

    @Override
    public void addLostTime(long lostTime) {
        this.cpuLostTime += lostTime;
    }

    @Override
    public void addCapacity(double capacity) {
        this.cpuCapacity += capacity;
    }

    @Override
    public void addDemand(double demand) {
        this.cpuDemand += demand;
    }

    @Override
    public void addSupply(double supply) {
        this.cpuSupply += supply;
    }
}
