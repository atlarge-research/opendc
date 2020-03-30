package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.HypervisorView
import com.atlarge.opendc.compute.virt.service.VirtProvisioningServiceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Allocation policy that selects the node with the least amount of active servers.
 */
public class NumberOfActiveServersAllocationPolicy : AllocationPolicy {
    override fun invoke(scope: CoroutineScope, events: Flow<VirtProvisioningServiceEvent>): Comparator<HypervisorView> =
        compareBy { it.numberOfActiveServers }
}
