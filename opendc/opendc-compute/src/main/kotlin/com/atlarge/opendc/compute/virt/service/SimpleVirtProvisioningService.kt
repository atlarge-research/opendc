package com.atlarge.opendc.compute.virt.service

import com.atlarge.odcsim.ProcessContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.driver.hypervisor.HypervisorImage
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import kotlinx.coroutines.launch

class SimpleVirtProvisioningService(
    private val ctx: ProcessContext,
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
    internal val availableNodes: MutableSet<Node> = mutableSetOf()

    /**
     * The available hypervisors.
     */
    internal val hypervisorByNode: MutableMap<Node, HypervisorImage> = mutableMapOf()

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
        ctx.launch {
            val provisionedNodes = provisioningService.nodes().toList()
            val deployedNodes = provisionedNodes.map { node ->
                val hypervisorImage =
                    HypervisorImage(
                        hypervisorMonitor
                    )
                hypervisorByNode[node] = hypervisorImage
                provisioningService.deploy(node, hypervisorImage, this@SimpleVirtProvisioningService)
            }
            nodes = deployedNodes
            availableNodes.addAll(deployedNodes)
        }
    }

    override suspend fun deploy(image: Image, monitor: ServerMonitor) {
        val vmInstance = ImageView(image, monitor)
        incomingImages += vmInstance
        requestCycle()
    }

    private fun requestCycle() {
        ctx.launch {
            schedule()
        }
    }

    private suspend fun schedule() {
        val imagesToBeScheduled = incomingImages.toSet()

        for (imageInstance in imagesToBeScheduled) {
            println("Spawning $imageInstance")

            val selectedNode = availableNodes.minBy {
                it.server!!.serviceRegistry[VirtDriver.Key].getNumberOfSpawnedImages()
            }

            imageInstance.server = selectedNode?.server!!.serviceRegistry[VirtDriver.Key].spawn(
                imageInstance.image,
                imageInstance.monitor,
                nodes[0].server!!.flavor
            )

            incomingImages -= imageInstance
            activeImages += imageInstance
            imagesByServer.putIfAbsent(imageInstance.server!!, mutableSetOf())
            imagesByServer[imageInstance.server!!]!!.add(imageInstance)
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
        var server: Server? = null
    )
}
