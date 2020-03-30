package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.virt.service.HypervisorView
import com.atlarge.opendc.compute.virt.service.VirtProvisioningServiceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * A policy for selecting the [Node] an image should be deployed to,
 */
interface AllocationPolicy {
    /**
     * Builds the logic of the policy.
     */
    operator fun invoke(scope: CoroutineScope, events: Flow<VirtProvisioningServiceEvent>): Comparator<HypervisorView>
}
