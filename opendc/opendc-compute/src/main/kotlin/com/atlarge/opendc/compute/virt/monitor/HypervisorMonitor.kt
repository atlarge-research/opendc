package com.atlarge.opendc.compute.virt.monitor

import com.atlarge.opendc.compute.core.Server

/**
 * Monitoring interface for hypervisor-specific events.
 */
interface HypervisorMonitor {
    /**
     * Invoked after a scheduling slice has finished processed.
     *
     * @param time The current time (in ms).
     * @param totalRequestedCpuTime The total requested CPU time (can be above capacity).
     * @param totalCpuTimeCapacity The actual total capacity of the machine managed by this hypervisor.
     * @param numberOfDeployedImages The number of images deployed on this hypervisor.
     * @param hostServer The server hosting this hypervisor.
     */
    fun onSliceFinish(
        time: Long,
        totalRequestedCpuTime: Long,
        totalCpuTimeCapacity: Long,
        numberOfDeployedImages: Int,
        hostServer: Server
    )
}
