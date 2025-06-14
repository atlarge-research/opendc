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

package org.opendc.simulator.compute.machine;

public interface ResourcePerformanceCounters {

    //    public void updateCounters(long time, double supply, double demand, double capacity);

    public long getActiveTime();

    public long getIdleTime();

    public long getStealTime();

    public long getLostTime();

    public double getCapacity();

    public double getDemand();

    public double getSupply();

    public void setActiveTime(long activeTime);

    public void setIdleTime(long idleTime);

    public void setStealTime(long stealTime);

    public void setLostTime(long lostTime);

    public void setCapacity(double capacity);

    public void setDemand(double demand);

    public void setSupply(double supply);

    public void addActiveTime(long activeTime);

    public void addIdleTime(long idleTime);

    public void addStealTime(long stealTime);

    public void addLostTime(long lostTime);

    public void addCapacity(double capacity);

    public void addDemand(double demand);

    public void addSupply(double supply);
}
