package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.HypervisorView

/**
 * Allocation policy that selects the node with the most available memory.
 */
class AvailableMemoryAllocationPolicy : AllocationPolicy {
    override fun invoke(): Comparator<HypervisorView> = Comparator { o1, o2 ->
        compareValuesBy(o1, o2) { -it.availableMemory }
    }
}
