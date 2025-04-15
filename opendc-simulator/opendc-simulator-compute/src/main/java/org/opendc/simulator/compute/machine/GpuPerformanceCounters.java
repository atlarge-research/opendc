package org.opendc.simulator.compute.machine;

public class GpuPerformanceCounters {
    private long gpuActiveTime = 0;
    private long gpuIdleTime = 0;
    private long gpuStealTime = 0;
    private long gpuLostTime = 0;

    private double gpuCapacity = 0.0f;
    private double gpuDemand = 0.0f;
    private double gpuSupply = 0.0f;

    public long getGpuActiveTime() {
        return gpuActiveTime;
    }

    public void setGpuActiveTime(long gpuActiveTime) {
        this.gpuActiveTime = gpuActiveTime;
    }

    public void addGpuActiveTime(long gpuActiveTime) {
        this.gpuActiveTime += gpuActiveTime;
    }

    public long getGpuIdleTime() {
        return gpuIdleTime;
    }

    public void setGpuIdleTime(long gpuIdleTime) {
        this.gpuIdleTime = gpuIdleTime;
    }

    public void addGpuIdleTime(long gpuIdleTime) {
        this.gpuIdleTime += gpuIdleTime;
    }

    public long getGpuStealTime() {
        return gpuStealTime;
    }

    public void setGpuStealTime(long gpuStealTime) {
        this.gpuStealTime = gpuStealTime;
    }

    public void addGpuStealTime(long gpuStealTime) {
        this.gpuStealTime += gpuStealTime;
    }

    public long getGpuLostTime() {
        return gpuLostTime;
    }

    public void setGpuLostTime(long gpuLostTime) {
        this.gpuLostTime = gpuLostTime;
    }

    public double getGpuCapacity() {
        return gpuCapacity;
    }

    public void setGpuCapacity(double gpuCapacity) {
        this.gpuCapacity = gpuCapacity;
    }

    public double getGpuDemand() {
        return gpuDemand;
    }

    public void setGpuDemand(double gpuDemand) {
        this.gpuDemand = gpuDemand;
    }

    public double getGpuSupply() {
        return gpuSupply;
    }

    public void setGpuSupply(double gpuSupply) {
        this.gpuSupply = gpuSupply;
    }
}
