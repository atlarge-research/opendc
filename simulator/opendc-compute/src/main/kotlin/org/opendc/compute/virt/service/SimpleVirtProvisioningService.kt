/*
 * Copyright (c) 2020 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.compute.virt.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.Server
import org.opendc.compute.core.ServerEvent
import org.opendc.compute.core.ServerState
import org.opendc.compute.core.image.Image
import org.opendc.compute.core.image.SimWorkloadImage
import org.opendc.compute.metal.service.ProvisioningService
import org.opendc.compute.virt.HypervisorEvent
import org.opendc.compute.virt.driver.InsufficientMemoryOnServerException
import org.opendc.compute.virt.driver.SimVirtDriver
import org.opendc.compute.virt.driver.SimVirtDriverWorkload
import org.opendc.compute.virt.driver.VirtDriver
import org.opendc.compute.virt.service.allocation.AllocationPolicy
import org.opendc.utils.flow.EventFlow
import java.time.Clock
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.math.max

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalCoroutinesApi::class)
public class SimpleVirtProvisioningService(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
    private val provisioningService: ProvisioningService,
    override val allocationPolicy: AllocationPolicy
) : VirtProvisioningService {
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

    public var submittedVms: Int = 0
    public var queuedVms: Int = 0
    public var runningVms: Int = 0
    public var finishedVms: Int = 0
    public var unscheduledVms: Int = 0

    private var maxCores = 0
    private var maxMemory = 0L

    /**
     * The allocation logic to use.
     */
    private val allocationLogic = allocationPolicy()

    /**
     * The [EventFlow] to emit the events.
     */
    internal val eventFlow = EventFlow<VirtProvisioningEvent>()

    override val events: Flow<VirtProvisioningEvent> = eventFlow

    init {
        coroutineScope.launch {
            val provisionedNodes = provisioningService.nodes()
            provisionedNodes.forEach { node ->
                val workload = SimVirtDriverWorkload()
                val hypervisorImage = SimWorkloadImage(UUID.randomUUID(), "vmm", emptyMap(), workload)
                launch {
                    var init = false
                    val deployedNode = provisioningService.deploy(node, hypervisorImage)
                    deployedNode.server!!.events.onEach { event ->
                        when (event) {
                            is ServerEvent.StateChanged -> {
                                if (!init) {
                                    init = true
                                }
                                stateChanged(event.server)
                            }
                        }
                    }.launchIn(this)

                    delay(1)
                    onHypervisorAvailable(deployedNode.server, workload.driver)
                }
            }
        }
    }

    override suspend fun drivers(): Set<VirtDriver> {
        return availableHypervisors.map { it.driver }.toSet()
    }

    override suspend fun deploy(
        name: String,
        image: Image,
        flavor: Flavor
    ): Server {
        eventFlow.emit(
            VirtProvisioningEvent.MetricsAvailable(
                this@SimpleVirtProvisioningService,
                hypervisors.size,
                availableHypervisors.size,
                ++submittedVms,
                runningVms,
                finishedVms,
                ++queuedVms,
                unscheduledVms
            )
        )

        return suspendCancellableCoroutine<Server> { cont ->
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

        val quantum = 300000 // 5 minutes in milliseconds
        // We assume that the provisioner runs at a fixed slot every time quantum (e.g t=0, t=60, t=120).
        // This is important because the slices of the VMs need to be aligned.
        // We calculate here the delay until the next scheduling slot.
        val delay = quantum - (clock.millis() % quantum)

        val call = coroutineScope.launch {
            delay(delay)
            this@SimpleVirtProvisioningService.call = null
            schedule()
        }
        this.call = call
    }

    private suspend fun schedule() {
        val imagesToBeScheduled = incomingImages.toSet()

        for (imageInstance in imagesToBeScheduled) {
            val requiredMemory = imageInstance.image.tags["required-memory"] as Long
            val selectedHv = allocationLogic.select(availableHypervisors, imageInstance)

            if (selectedHv == null) {
                if (requiredMemory > maxMemory || imageInstance.flavor.cpuCount > maxCores) {
                    eventFlow.emit(
                        VirtProvisioningEvent.MetricsAvailable(
                            this@SimpleVirtProvisioningService,
                            hypervisors.size,
                            availableHypervisors.size,
                            submittedVms,
                            runningVms,
                            finishedVms,
                            queuedVms,
                            ++unscheduledVms
                        )
                    )

                    incomingImages -= imageInstance

                    logger.warn("Failed to spawn ${imageInstance.image}: does not fit [${clock.millis()}]")
                    continue
                } else {
                    break
                }
            }

            try {
                logger.info { "[${clock.millis()}] Spawning ${imageInstance.image} on ${selectedHv.server.uid} ${selectedHv.server.name} ${selectedHv.server.flavor}" }
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

                eventFlow.emit(
                    VirtProvisioningEvent.MetricsAvailable(
                        this@SimpleVirtProvisioningService,
                        hypervisors.size,
                        availableHypervisors.size,
                        submittedVms,
                        ++runningVms,
                        finishedVms,
                        --queuedVms,
                        unscheduledVms
                    )
                )
                activeImages += imageInstance

                server.events
                    .onEach { event ->
                        when (event) {
                            is ServerEvent.StateChanged -> {
                                if (event.server.state == ServerState.SHUTOFF) {
                                    logger.info { "[${clock.millis()}] Server ${event.server.uid} ${event.server.name} ${event.server.flavor} finished." }

                                    eventFlow.emit(
                                        VirtProvisioningEvent.MetricsAvailable(
                                            this@SimpleVirtProvisioningService,
                                            hypervisors.size,
                                            availableHypervisors.size,
                                            submittedVms,
                                            --runningVms,
                                            ++finishedVms,
                                            queuedVms,
                                            unscheduledVms
                                        )
                                    )

                                    activeImages -= imageInstance
                                    selectedHv.provisionedCores -= server.flavor.cpuCount

                                    // Try to reschedule if needed
                                    if (incomingImages.isNotEmpty()) {
                                        requestCycle()
                                    }
                                }
                            }
                        }
                    }
                    .launchIn(coroutineScope)
            } catch (e: InsufficientMemoryOnServerException) {
                logger.error("Failed to deploy VM", e)

                selectedHv.numberOfActiveServers--
                selectedHv.provisionedCores -= imageInstance.flavor.cpuCount
                selectedHv.availableMemory += requiredMemory
            } catch (e: Throwable) {
                logger.error("Failed to deploy VM", e)
            }
        }
    }

    private fun stateChanged(server: Server) {
        when (server.state) {
            ServerState.ACTIVE -> {
                logger.debug { "[${clock.millis()}] Server ${server.uid} available: ${server.state}" }

                if (server in hypervisors) {
                    // Corner case for when the hypervisor already exists
                    availableHypervisors += hypervisors.getValue(server)
                } else {
                    val hv = HypervisorView(
                        server.uid,
                        server,
                        0,
                        server.flavor.memorySize,
                        0
                    )
                    maxCores = max(maxCores, server.flavor.cpuCount)
                    maxMemory = max(maxMemory, server.flavor.memorySize)
                    hypervisors[server] = hv
                }

                eventFlow.emit(
                    VirtProvisioningEvent.MetricsAvailable(
                        this@SimpleVirtProvisioningService,
                        hypervisors.size,
                        availableHypervisors.size,
                        submittedVms,
                        runningVms,
                        finishedVms,
                        queuedVms,
                        unscheduledVms
                    )
                )

                // Re-schedule on the new machine
                if (incomingImages.isNotEmpty()) {
                    requestCycle()
                }
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                logger.debug { "[${clock.millis()}] Server ${server.uid} unavailable: ${server.state}" }
                val hv = hypervisors[server] ?: return
                availableHypervisors -= hv

                eventFlow.emit(
                    VirtProvisioningEvent.MetricsAvailable(
                        this@SimpleVirtProvisioningService,
                        hypervisors.size,
                        availableHypervisors.size,
                        submittedVms,
                        runningVms,
                        finishedVms,
                        queuedVms,
                        unscheduledVms
                    )
                )

                if (incomingImages.isNotEmpty()) {
                    requestCycle()
                }
            }
            else -> throw IllegalStateException()
        }
    }

    private fun onHypervisorAvailable(server: Server, hypervisor: SimVirtDriver) {
        val hv = hypervisors[server] ?: return
        hv.driver = hypervisor
        availableHypervisors += hv

        eventFlow.emit(
            VirtProvisioningEvent.MetricsAvailable(
                this@SimpleVirtProvisioningService,
                hypervisors.size,
                availableHypervisors.size,
                submittedVms,
                runningVms,
                finishedVms,
                queuedVms,
                unscheduledVms
            )
        )

        hv.driver.events
            .onEach { event ->
                if (event is HypervisorEvent.VmsUpdated) {
                    hv.numberOfActiveServers = event.numberOfActiveServers
                    hv.availableMemory = event.availableMemory
                }
            }.launchIn(coroutineScope)

        requestCycle()
    }

    public data class ImageView(
        public val name: String,
        public val image: Image,
        public val flavor: Flavor,
        public val continuation: Continuation<Server>,
        public var server: Server? = null
    )
}
