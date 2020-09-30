package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.HypervisorView

/**
 * Allocation policy that selects the node with the least amount of active servers.
 *
 * @param reversed A flag to reverse the order, such that the node with the most active servers is selected.
 */
public class NumberOfActiveServersAllocationPolicy(val reversed: Boolean = false) : AllocationPolicy {
    override fun invoke(): AllocationPolicy.Logic = object : ComparableAllocationPolicyLogic {
        override val comparator: Comparator<HypervisorView> = compareBy<HypervisorView> { it.numberOfActiveServers }
            .run { if (reversed) reversed() else this }
    }
}
