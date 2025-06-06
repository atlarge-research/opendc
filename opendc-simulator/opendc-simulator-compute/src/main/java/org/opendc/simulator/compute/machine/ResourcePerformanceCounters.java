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
