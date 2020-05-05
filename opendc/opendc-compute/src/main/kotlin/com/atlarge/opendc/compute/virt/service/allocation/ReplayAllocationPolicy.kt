package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.HypervisorView
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService

class ReplayAllocationPolicy(val vmPlacements: Map<String, String>) : AllocationPolicy {
    override fun invoke(): AllocationPolicy.Logic = object : AllocationPolicy.Logic {
        override fun select(
            hypervisors: Set<HypervisorView>,
            image: SimpleVirtProvisioningService.ImageView
        ): HypervisorView? {
            val clusterName = vmPlacements[image.name]
                ?: throw IllegalArgumentException("Could not find placement data in VM placement file for VM ${image.name}")
            val machinesInCluster = hypervisors.filter { it.server.name.contains(clusterName) }
            return machinesInCluster.minBy { it.numberOfActiveServers }
                ?: throw IllegalArgumentException("Cloud not find any machines belonging to cluster $clusterName for image ${image.name}")
        }
    }
}
