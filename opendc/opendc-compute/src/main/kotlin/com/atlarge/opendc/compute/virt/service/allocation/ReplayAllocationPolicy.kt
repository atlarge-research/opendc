package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.HypervisorView
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Policy replaying VM-cluster assignment.
 *
 * Within each cluster, the active servers on each node determine which node gets
 * assigned the VM image.
 */
class ReplayAllocationPolicy(val vmPlacements: Map<String, String>, val random: Random = Random(0)) : AllocationPolicy {
    override fun invoke(): AllocationPolicy.Logic = object : AllocationPolicy.Logic {
        override fun select(
            hypervisors: Set<HypervisorView>,
            image: SimpleVirtProvisioningService.ImageView
        ): HypervisorView? {
            val clusterName = vmPlacements[image.name]
                ?: throw IllegalStateException("Could not find placement data in VM placement file for VM ${image.name}")
            val machinesInCluster = hypervisors.filter { it.server.name.contains(clusterName) }

            if (machinesInCluster.isEmpty()) {
                logger.info { "Could not find any machines belonging to cluster $clusterName for image ${image.name}, assigning randomly." }
                return hypervisors.random(random)
            }

            return machinesInCluster.maxBy { it.availableMemory }
                ?: throw IllegalStateException("Cloud not find any machine and could not randomly assign")
        }
    }
}
