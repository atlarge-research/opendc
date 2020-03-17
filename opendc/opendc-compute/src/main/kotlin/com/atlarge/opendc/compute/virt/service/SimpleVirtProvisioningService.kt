package com.atlarge.opendc.compute.virt.service

import com.atlarge.odcsim.SimulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriverMonitor
import com.atlarge.opendc.compute.virt.driver.hypervisor.HypervisorImage
import com.atlarge.opendc.compute.virt.driver.hypervisor.InsufficientMemoryOnServerException
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import com.atlarge.opendc.compute.virt.service.allocation.AllocationPolicy
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class SimpleVirtProvisioningService(
    public override val allocationPolicy: AllocationPolicy,
    private val ctx: SimulationContext,
    private val provisioningService: ProvisioningService,
    private val hypervisorMonitor: HypervisorMonitor
) : VirtProvisioningService, ServerMonitor {
    /**
     * The hypervisors that have been launched by the service.
     */
    private val hypervisors: MutableMap<Server, HypervisorView> = mutableMapOf()

    /**
     * The available hypervisors.
     */
    private val availableHypervisors: MutableSet<HypervisorView> = mutableSetOf()

    /**
     * The incoming images to be processed by the provisioner.
     */
    private val incomingImages: MutableSet<ImageView> = mutableSetOf()

    /**
     * The active images in the system.
     */
    private val activeImages: MutableSet<ImageView> = mutableSetOf()

    init {
        ctx.domain.launch {
            val provisionedNodes = provisioningService.nodes()
            provisionedNodes.forEach { node ->
                val hypervisorImage = HypervisorImage(hypervisorMonitor)
                val deployedNode = provisioningService.deploy(node, hypervisorImage, this@SimpleVirtProvisioningService)
                val server = deployedNode.server!!
                val hvView = HypervisorView(
                    server,
                    hypervisorImage,
                    0,
                    server.flavor.memorySize
                )
                hypervisors[server] = hvView
                yield()
                server.serviceRegistry[VirtDriver.Key].addMonitor(object : VirtDriverMonitor {
                    override suspend fun onUpdate(numberOfActiveServers: Int, availableMemory: Long) {
                        hvView.numberOfActiveServers = numberOfActiveServers
                        hvView.availableMemory = availableMemory
                    }
                })
            }
        }
    }

    override suspend fun deploy(image: Image, monitor: ServerMonitor, flavor: Flavor) {
        val vmInstance = ImageView(image, monitor, flavor)
        incomingImages += vmInstance
        requestCycle()
    }

    private fun requestCycle() {
        ctx.domain.launch {
            schedule()
        }
    }

    private suspend fun schedule() {
        val imagesToBeScheduled = incomingImages.toSet()

        for (imageInstance in imagesToBeScheduled) {
            val selectedNode = availableHypervisors.minWith(allocationPolicy().thenBy { it.server.uid }) ?: break
            try {
                println("Spawning ${imageInstance.image}")
                incomingImages -= imageInstance
                imageInstance.server = selectedNode.server.serviceRegistry[VirtDriver.Key].spawn(
                    imageInstance.image,
                    imageInstance.monitor,
                    imageInstance.flavor
                )
                activeImages += imageInstance
            } catch (e: InsufficientMemoryOnServerException) {
                println("Unable to deploy image due to insufficient memory")
            }
        }
    }

    override suspend fun onUpdate(server: Server, previousState: ServerState) {
        when (server.state) {
            ServerState.ACTIVE -> {
                val hv = hypervisors[server] ?: return
                availableHypervisors += hv
                requestCycle()
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                val hv = hypervisors[server] ?: return
                availableHypervisors -= hv
                requestCycle()
            }
            else -> throw IllegalStateException()
        }
    }

    data class ImageView(
        val image: Image,
        val monitor: ServerMonitor,
        val flavor: Flavor,
        var server: Server? = null
    )
}
