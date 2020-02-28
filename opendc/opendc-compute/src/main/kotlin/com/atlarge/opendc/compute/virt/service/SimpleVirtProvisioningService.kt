package com.atlarge.opendc.compute.virt.service

import com.atlarge.odcsim.SimulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriverMonitor
import com.atlarge.opendc.compute.virt.driver.hypervisor.HypervisorImage
import com.atlarge.opendc.compute.virt.driver.hypervisor.InsufficientMemoryOnServerException
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import com.atlarge.opendc.compute.virt.service.allocation.AllocationPolicy
import kotlinx.coroutines.launch

class SimpleVirtProvisioningService(
    public override val allocationPolicy: AllocationPolicy,
    private val ctx: SimulationContext,
    private val provisioningService: ProvisioningService,
    private val hypervisorMonitor: HypervisorMonitor
) : VirtProvisioningService, ServerMonitor {
    /**
     * The nodes that are controlled by the service.
     */
    internal lateinit var nodes: List<Node>

    /**
     * The available nodes.
     */
    internal val availableNodes: MutableSet<NodeView> = mutableSetOf()

    /**
     * The incoming images to be processed by the provisioner.
     */
    internal val incomingImages: MutableSet<ImageView> = mutableSetOf()

    /**
     * The active images in the system.
     */
    internal val activeImages: MutableSet<ImageView> = mutableSetOf()

    /**
     * The images hosted on each server.
     */
    internal val imagesByServer: MutableMap<Server, MutableSet<ImageView>> = mutableMapOf()

    init {
        ctx.domain.launch {
            val provisionedNodes = provisioningService.nodes().toList()
            val deployedNodes = provisionedNodes.map { node ->
                val hypervisorImage = HypervisorImage(hypervisorMonitor)
                val nodeView = NodeView(
                    provisioningService.deploy(node, hypervisorImage, this@SimpleVirtProvisioningService),
                    hypervisorImage,
                    0,
                    node.server!!.flavor.memorySize
                )
                node.server.serviceRegistry[VirtDriver.Key].addMonitor(object : VirtDriverMonitor {
                    override suspend fun onUpdate(numberOfActiveServers: Int, availableMemory: Long) {
                        nodeView.numberOfActiveServers = numberOfActiveServers
                        nodeView.availableMemory = availableMemory
                    }
                })
                nodeView
            }
            nodes = deployedNodes.map { it.node }
            availableNodes.addAll(deployedNodes)
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
            println("Spawning $imageInstance")

            val selectedNode = availableNodes.minWith(allocationPolicy().thenBy { it.node.uid })

            try {
                imageInstance.server = selectedNode?.node!!.server!!.serviceRegistry[VirtDriver.Key].spawn(
                    imageInstance.image,
                    imageInstance.monitor,
                    imageInstance.flavor
                )
                activeImages += imageInstance
                imagesByServer.putIfAbsent(imageInstance.server!!, mutableSetOf())
                imagesByServer[imageInstance.server!!]!!.add(imageInstance)
            } catch (e: InsufficientMemoryOnServerException) {
                println("Unable to deploy image due to insufficient memory")
            }

            incomingImages -= imageInstance
        }
    }

    override suspend fun onUpdate(server: Server, previousState: ServerState) {
        when (server.state) {
            ServerState.ACTIVE -> {
                // TODO handle hypervisor server becoming active
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                // TODO handle hypervisor server shutting down or failing
            }
            else -> throw IllegalStateException()
        }
    }

    class ImageView(
        val image: Image,
        val monitor: ServerMonitor,
        val flavor: Flavor,
        var server: Server? = null
    )
}
