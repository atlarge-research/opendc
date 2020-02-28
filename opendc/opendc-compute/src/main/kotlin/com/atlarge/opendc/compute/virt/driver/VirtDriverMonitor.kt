package com.atlarge.opendc.compute.virt.driver

/**
 * Monitor for entities interested in the state of a [VirtDriver].
 */
interface VirtDriverMonitor {
    /**
     * Called when the number of active servers on the server managed by this driver is updated.
     *
     * @param numberOfActiveServers The number of active servers.
     * @param availableMemory The available memory, in MB.
     */
    public suspend fun onUpdate(numberOfActiveServers: Int, availableMemory: Long)
}
