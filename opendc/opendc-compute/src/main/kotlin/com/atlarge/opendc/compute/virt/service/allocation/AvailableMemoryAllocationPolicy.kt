package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.NodeView

/**
 * Allocation policy that selects the node with the most available memory.
 */
class AvailableMemoryAllocationPolicy : AllocationPolicy {
    override fun invoke(): Comparator<NodeView> = Comparator { o1, o2 ->
        compareValuesBy(o1, o2) { -it.availableMemory }
    }
}
