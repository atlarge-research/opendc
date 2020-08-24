package com.atlarge.opendc.compute.virt.service

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import java.util.UUID

class HypervisorView(
    val uid: UUID,
    var server: Server,
    var numberOfActiveServers: Int,
    var availableMemory: Long,
    var provisionedCores: Int
) {
    lateinit var driver: VirtDriver
}
