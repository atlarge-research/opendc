package org.opendc.simulator.compute;

import org.opendc.simulator.compute.machine.ResourcePerformanceCounters;

public interface ComputeResource {

    public ResourcePerformanceCounters getPerformanceCounters();
    public int getId();
}
