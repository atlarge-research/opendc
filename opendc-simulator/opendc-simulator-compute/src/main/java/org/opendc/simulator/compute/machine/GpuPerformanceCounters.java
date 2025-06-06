package org.opendc.simulator.compute.machine;

public class GpuPerformanceCounters implements ResourcePerformanceCounters {
    private long gpuActiveTime = 0;
    private long gpuIdleTime = 0;
    private long gpuStealTime = 0;
    private long gpuLostTime = 0;

    private double gpuCapacity = 0.0f;
    private double gpuDemand = 0.0f;
    private double gpuSupply = 0.0f;

    @Override
    public long getActiveTime() {
        return gpuActiveTime;
    }

    @Override
    public long getIdleTime() {
        return gpuIdleTime;
    }

    @Override
    public long getStealTime() {
        return gpuStealTime;
    }

    @Override
    public long getLostTime() {
        return gpuLostTime;
    }

    @Override
    public double getCapacity() {
        return gpuCapacity;
    }

    @Override
    public double getDemand() {
        return gpuDemand;
    }

    @Override
    public double getSupply() {
        return gpuSupply;
    }

    @Override
    public void setActiveTime(long activeTime) {
        this.gpuActiveTime = activeTime;
    }

    @Override
    public void setIdleTime(long idleTime) {
        this.gpuIdleTime = idleTime;
    }

    @Override
    public void setStealTime(long stealTime) {
        this.gpuStealTime = stealTime;
    }

    @Override
    public void setLostTime(long lostTime) {
        this.gpuLostTime = lostTime;
    }

    @Override
    public void setCapacity(double capacity) {
        this.gpuCapacity = capacity;
    }

    @Override
    public void setDemand(double demand) {
        this.gpuDemand = demand;
    }

    @Override
    public void setSupply(double supply) {
        this.gpuSupply = supply;
    }

    @Override
    public void addActiveTime(long activeTime) {
        this.gpuActiveTime += activeTime;
    }

    @Override
    public void addIdleTime(long idleTime) {
        this.gpuIdleTime += idleTime;
    }

    @Override
    public void addStealTime(long stealTime) {
        this.gpuStealTime += stealTime;
    }

    @Override
    public void addLostTime(long lostTime) {
        this.gpuLostTime += lostTime;
    }

    @Override
    public void addCapacity(double capacity) {
        this.gpuCapacity += capacity;
    }

    @Override
    public void addDemand(double demand) {
        this.gpuDemand += demand;
    }

    @Override
    public void addSupply(double supply) {
        this.gpuSupply += supply;
    }

}
