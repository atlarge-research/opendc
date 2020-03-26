package com.atlarge.opendc.compute.virt.service

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.virt.driver.VirtDriver

class HypervisorView(
    var server: Server,
    var numberOfActiveServers: Int,
    var availableMemory: Long
) {
    lateinit var driver: VirtDriver
}
