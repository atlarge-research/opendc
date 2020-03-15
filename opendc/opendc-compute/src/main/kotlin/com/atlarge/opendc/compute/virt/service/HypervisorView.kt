package com.atlarge.opendc.compute.virt.service

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.virt.driver.hypervisor.HypervisorImage

class HypervisorView(
    var server: Server,
    val hypervisor: HypervisorImage,
    var numberOfActiveServers: Int,
    var availableMemory: Long
)
