package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.HypervisorView

/**
 * Allocation policy that selects the node with the most available memory.
 *
 * @param reversed A flag to reverse the order (least amount of memory scores the best).
 */
public class AvailableMemoryAllocationPolicy(val reversed: Boolean = false) : AllocationPolicy {
    override fun invoke(): AllocationPolicy.Logic = object : ComparableAllocationPolicyLogic {
        override val comparator: Comparator<HypervisorView> = compareBy<HypervisorView> { -it.availableMemory }
            .run { if (reversed) reversed() else this }
    }
}
