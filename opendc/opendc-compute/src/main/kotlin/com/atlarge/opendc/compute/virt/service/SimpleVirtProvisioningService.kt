package com.atlarge.opendc.compute.virt.service

import com.atlarge.odcsim.SimulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.HypervisorImage
import com.atlarge.opendc.compute.virt.driver.InsufficientMemoryOnServerException
import com.atlarge.opendc.compute.virt.service.allocation.AllocationPolicy
import com.atlarge.opendc.core.services.ServiceKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class SimpleVirtProvisioningService(
    public override val allocationPolicy: AllocationPolicy,
    private val ctx: SimulationContext,
    private val provisioningService: ProvisioningService
) : VirtProvisioningService, CoroutineScope by ctx.domain {
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
        launch {
            val provisionedNodes = provisioningService.nodes()
            provisionedNodes.forEach { node ->
                val hypervisorImage = HypervisorImage
                val node = provisioningService.deploy(node, hypervisorImage)
                node.server!!.events.onEach { event ->
                    when (event) {
                        is ServerEvent.StateChanged -> stateChanged(event.server)
                        is ServerEvent.ServicePublished -> servicePublished(event.server, event.key)
                    }
                }.collect()
            }
        }
    }

    override suspend fun deploy(
        name: String,
        image: Image,
        flavor: Flavor
    ): Server = suspendCancellableCoroutine { cont ->
        val vmInstance = ImageView(name, image, flavor, cont)
        incomingImages += vmInstance
        requestCycle()
    }

    private var call: Job? = null

    private fun requestCycle() {
        if (call != null) {
            return
        }

        val call = launch {
            schedule()
        }
        call.invokeOnCompletion { this.call = null }
        this.call = call
    }

    private suspend fun schedule() {
        val imagesToBeScheduled = incomingImages.toSet()

        for (imageInstance in imagesToBeScheduled) {
            val selectedHv = availableHypervisors.minWith(allocationPolicy().thenBy { it.server.uid }) ?: break
            try {
                println("Spawning ${imageInstance.image}")
                incomingImages -= imageInstance
                val server = selectedHv.driver.spawn(
                    imageInstance.name,
                    imageInstance.image,
                    imageInstance.flavor
                )
                imageInstance.server = server
                imageInstance.continuation.resume(server)
                activeImages += imageInstance
            } catch (e: InsufficientMemoryOnServerException) {
                println("Unable to deploy image due to insufficient memory")
            }
        }
    }

    private fun stateChanged(server: Server) {
        when (server.state) {
            ServerState.ACTIVE -> {
                val hvView = HypervisorView(
                    server,
                    0,
                    server.flavor.memorySize
                )
                hypervisors[server] = hvView
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                val hv = hypervisors[server] ?: return
                availableHypervisors -= hv
                requestCycle()
            }
            else -> throw IllegalStateException()
        }
    }

    private fun servicePublished(server: Server, key: ServiceKey<*>) {
        if (key == VirtDriver.Key) {
            val hv = hypervisors[server] ?: return
            hv.driver = server.services[VirtDriver]
            availableHypervisors += hv
            requestCycle()
        }
    }

    data class ImageView(
        val name: String,
        val image: Image,
        val flavor: Flavor,
        val continuation: Continuation<Server>,
        var server: Server? = null
    )
}
