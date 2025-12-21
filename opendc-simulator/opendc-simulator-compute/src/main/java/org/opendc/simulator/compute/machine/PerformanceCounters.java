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

public class PerformanceCounters {

    private long activeTime = 0;
    private long idleTime = 0;
    private long stealTime = 0;
    private long lostTime = 0;

    private double capacity = 0.0f;
    private double demand = 0.0f;
    private double supply = 0.0f;
    private double powerDraw = 0.0f;
    private double totalComponentValue = 0.0f;

    public long getActiveTime() {
        return this.activeTime;
    }

    public long getIdleTime() {
        return this.idleTime;
    }

    public long getStealTime() {
        return this.stealTime;
    }

    public long getLostTime() {
        return this.lostTime;
    }

    public double getCapacity() {
        return this.capacity;
    }

    public double getDemand() {
        return this.demand;
    }

    public double getSupply() {
        return this.supply;
    }

    public double getPowerDraw() {
        return powerDraw;
    }

    public double getTotalComponentValue() {return this.totalComponentValue;}

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    public void setIdleTime(long idleTime) {
        this.idleTime = idleTime;
    }

    public void setStealTime(long stealTime) {
        this.stealTime = stealTime;
    }

    public void setLostTime(long lostTime) {
        this.lostTime = lostTime;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public void setSupply(double supply) {
        this.supply = supply;
    }

    public void setPowerDraw(double powerDraw) {
        this.powerDraw = powerDraw;
    }

    public void setTotalComponentValue(double totalComponentValue) {
        this.totalComponentValue = totalComponentValue;
    }

    public void addActiveTime(long activeTime) {
        this.activeTime += activeTime;
    }

    public void addIdleTime(long idleTime) {
        this.idleTime += idleTime;
    }

    public void addStealTime(long stealTime) {
        this.stealTime += stealTime;
    }

    public void addTotalComponentValue(double componentValue) {
        this.totalComponentValue += componentValue;
    }

    public void addLostTime(long lostTime) {
        this.lostTime += lostTime;
    }

    public void addCapacity(double capacity) {
        this.capacity += capacity;
    }

    public void addDemand(double demand) {
        this.demand += demand;
    }

    public void addSupply(double supply) {
        this.supply += supply;
    }

    public void addPowerDraw(double powerDraw) {
        this.powerDraw += powerDraw;
    }
}
