package com.atlarge.opendc.compute.virt.service

import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.virt.driver.hypervisor.HypervisorImage

class NodeView(
    val node: Node,
    val hypervisor: HypervisorImage,
    var numberOfActiveServers: Int,
    var availableMemory: Long
)
