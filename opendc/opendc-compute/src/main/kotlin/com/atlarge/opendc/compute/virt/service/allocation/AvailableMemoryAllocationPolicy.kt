package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.HypervisorView
import com.atlarge.opendc.compute.virt.service.VirtProvisioningServiceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Allocation policy that selects the node with the most available memory.
 */
public class AvailableMemoryAllocationPolicy : AllocationPolicy {
    override fun invoke(scope: CoroutineScope, events: Flow<VirtProvisioningServiceEvent>): Comparator<HypervisorView> =
        compareBy { -it.availableMemory }
}
