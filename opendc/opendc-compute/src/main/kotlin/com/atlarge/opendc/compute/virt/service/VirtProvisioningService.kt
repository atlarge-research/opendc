package com.atlarge.opendc.compute.virt.service

import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor

/**
 * A service for VM provisioning on a cloud.
 */
interface VirtProvisioningService {
    /**
     * Submit the specified [Image] to the provisioning service.
     */
    public suspend fun deploy(image: Image, monitor: ServerMonitor)
}
