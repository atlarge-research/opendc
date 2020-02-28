package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.virt.service.NodeView
import kotlinx.coroutines.runBlocking

/**
 * Allocation policy that selects the node with the least amount of active servers.
 */
class NumberOfActiveServersAllocationPolicy : AllocationPolicy {
    override fun invoke(): Comparator<NodeView> = Comparator { o1, o2 ->
        runBlocking {
            compareValuesBy(o1, o2) { it.numberOfActiveServers }
        }
    }
}
