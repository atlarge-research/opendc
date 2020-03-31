package com.atlarge.opendc.compute.virt.service

import com.atlarge.odcsim.SimulationContext
import com.atlarge.odcsim.flow.EventFlow
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.HypervisorEvent
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.HypervisorImage
import com.atlarge.opendc.compute.virt.driver.InsufficientMemoryOnServerException
import com.atlarge.opendc.compute.virt.service.allocation.AllocationPolicy
import com.atlarge.opendc.core.services.ServiceKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

    override val hypervisorEvents: Flow<HypervisorEvent> = EventFlow()

    /**
     * The allocation logic to use.
     */
    private val allocationLogic = allocationPolicy()

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
                }.launchIn(this)
            }
        }
    }

    override suspend fun drivers(): Set<VirtDriver> = withContext(coroutineContext) {
        availableHypervisors.map { it.driver }.toSet()
    }

    override suspend fun deploy(
        name: String,
        image: Image,
        flavor: Flavor
    ): Server = withContext(coroutineContext) {
        suspendCancellableCoroutine<Server> { cont ->
            val vmInstance = ImageView(name, image, flavor, cont)
            incomingImages += vmInstance
            requestCycle()
        }
    }

    override suspend fun terminate() {
        val provisionedNodes = provisioningService.nodes()
        provisionedNodes.forEach { node -> provisioningService.stop(node) }
    }

    private var call: Job? = null

    private fun requestCycle() {
        if (call != null) {
            return
        }

        val call = launch {
            delay(1)
            this@SimpleVirtProvisioningService.call = null
            schedule()
        }
        this.call = call
    }

    private suspend fun schedule() {
        val log = simulationContext.log
        val imagesToBeScheduled = incomingImages.toSet()

        for (imageInstance in imagesToBeScheduled) {
            val requiredMemory = (imageInstance.image as VmImage).requiredMemory
            val selectedHv = allocationLogic.select(availableHypervisors, imageInstance) ?: break
            try {
                log.info("Spawning ${imageInstance.image} on ${selectedHv.server}")
                incomingImages -= imageInstance

                // Speculatively update the hypervisor view information to prevent other images in the queue from
                // deciding on stale values.
                selectedHv.numberOfActiveServers++
                selectedHv.provisionedCores += imageInstance.flavor.cpuCount
                selectedHv.availableMemory -= requiredMemory // XXX Temporary hack

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

                selectedHv.numberOfActiveServers--
                selectedHv.provisionedCores -= imageInstance.flavor.cpuCount
                selectedHv.availableMemory += requiredMemory
            }
        }
    }

    private fun stateChanged(server: Server) {
        when (server.state) {
            ServerState.ACTIVE -> {
                val hv = HypervisorView(
                    server.uid,
                    server,
                    0,
                    server.flavor.memorySize,
                    0
                )
                hypervisors[server] = hv
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                val hv = hypervisors[server] ?: return
                hv.provisionedCores -= server.flavor.cpuCount
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

            hv.driver.events
                .onEach { event ->
                    if (event is HypervisorEvent.VmsUpdated) {
                        hv.numberOfActiveServers = event.numberOfActiveServers
                        hv.availableMemory = event.availableMemory
                    }
                }.launchIn(this)

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
