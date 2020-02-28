package com.atlarge.opendc.compute.virt.service

import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.virt.service.allocation.AllocationPolicy

/**
 * A service for VM provisioning on a cloud.
 */
interface VirtProvisioningService {
    val allocationPolicy: AllocationPolicy

    /**
     * Submit the specified [Image] to the provisioning service.
     *
     * @param image The image to be deployed.
     * @param monitor The monitor to inform on events.
     * @param flavor The flavor of the machine instance to run this [image] on.
     */
    public suspend fun deploy(image: Image, monitor: ServerMonitor, flavor: Flavor)
}
