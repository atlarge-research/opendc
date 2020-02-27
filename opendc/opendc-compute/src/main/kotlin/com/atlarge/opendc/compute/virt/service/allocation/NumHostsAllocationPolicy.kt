package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.runBlocking

class NumHostsAllocationPolicy : AllocationPolicy {
    override suspend fun invoke(): Comparator<Node> = Comparator { o1, o2 ->
        runBlocking {
            compareValuesBy(o1, o2) {
                it.server!!.serviceRegistry[VirtDriver.Key].getNumberOfSpawnedImages()
            }
        }
    }
}
