package org.opendc.simulator.compute;

import org.opendc.simulator.compute.machine.ResourcePerformanceCounters;

public interface ComputeResource {

    public int getId();
    public ResourcePerformanceCounters getPerformanceCounters();
    public double getCapacity();
    public double getDemand();
    public double getSupply();
}
