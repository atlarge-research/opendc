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

package org.opendc.compute.simulator

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
import org.opendc.compute.core.metal.Node
import org.opendc.compute.core.metal.NodeEvent
import org.opendc.compute.core.metal.NodeState
import org.opendc.compute.core.metal.service.ProvisioningService
import org.opendc.compute.core.virt.HypervisorEvent
import org.opendc.compute.core.virt.driver.InsufficientMemoryOnServerException
import org.opendc.compute.core.virt.driver.VirtDriver
import org.opendc.compute.core.virt.service.VirtProvisioningEvent
import org.opendc.compute.core.virt.service.VirtProvisioningService
import org.opendc.compute.core.virt.service.events.*
import org.opendc.compute.simulator.allocation.AllocationPolicy
import org.opendc.simulator.compute.SimHypervisorProvider
import org.opendc.trace.core.EventTracer
import org.opendc.utils.TimerScheduler
import org.opendc.utils.flow.EventFlow
import java.time.Clock
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.math.max

@OptIn(ExperimentalCoroutinesApi::class)
public class SimVirtProvisioningService(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
    private val provisioningService: ProvisioningService,
    public val allocationPolicy: AllocationPolicy,
    private val tracer: EventTracer,
    private val hypervisor: SimHypervisorProvider,
    private val schedulingQuantum: Long = 300000, // 5 minutes in milliseconds
) : VirtProvisioningService {
    /**
     * The logger instance to use.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The hypervisors that have been launched by the service.
     */
    private val hypervisors: MutableMap<Node, HypervisorView> = mutableMapOf()

    /**
     * The available hypervisors.
     */
    private val availableHypervisors: MutableSet<HypervisorView> = mutableSetOf()

    /**
     * The incoming images to be processed by the provisioner.
     */
    private val incomingImages: Deque<ImageView> = ArrayDeque()

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

    /**
     * The [TimerScheduler] to use for scheduling the scheduler cycles.
     */
    private var scheduler: TimerScheduler<Unit> = TimerScheduler(coroutineScope, clock)

    init {
        coroutineScope.launch {
            val provisionedNodes = provisioningService.nodes()
            provisionedNodes.forEach { node ->
                val workload = SimVirtDriver(coroutineScope, hypervisor)
                val hypervisorImage = Image(UUID.randomUUID(), "vmm", mapOf("workload" to workload))
                launch {
                    val deployedNode = provisioningService.deploy(node, hypervisorImage)
                    deployedNode.events.onEach { event ->
                        when (event) {
                            is NodeEvent.StateChanged -> stateChanged(event.node, workload)
                        }
                    }.launchIn(this)
                }
            }
        }
    }

    override suspend fun drivers(): Set<VirtDriver> {
        return availableHypervisors.map { it.driver }.toSet()
    }

    override val hostCount: Int = hypervisors.size

    override suspend fun deploy(
        name: String,
        image: Image,
        flavor: Flavor
    ): Server {
        tracer.commit(VmSubmissionEvent(name, image, flavor))

        eventFlow.emit(
            VirtProvisioningEvent.MetricsAvailable(
                this@SimVirtProvisioningService,
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

    private fun requestCycle() {
        // Bail out in case we have already requested a new cycle.
        if (scheduler.isTimerActive(Unit)) {
            return
        }

        // We assume that the provisioner runs at a fixed slot every time quantum (e.g t=0, t=60, t=120).
        // This is important because the slices of the VMs need to be aligned.
        // We calculate here the delay until the next scheduling slot.
        val delay = schedulingQuantum - (clock.millis() % schedulingQuantum)

        scheduler.startSingleTimer(Unit, delay) {
            coroutineScope.launch { schedule() }
        }
    }

    private suspend fun schedule() {
        while (incomingImages.isNotEmpty()) {
            val imageInstance = incomingImages.peekFirst()
            val requiredMemory = imageInstance.flavor.memorySize
            val selectedHv = allocationLogic.select(availableHypervisors, imageInstance)

            if (selectedHv == null || !selectedHv.driver.canFit(imageInstance.flavor)) {
                logger.trace { "Image ${imageInstance.image} selected for scheduling but no capacity available for it." }

                if (requiredMemory > maxMemory || imageInstance.flavor.cpuCount > maxCores) {
                    tracer.commit(VmSubmissionInvalidEvent(imageInstance.name))

                    eventFlow.emit(
                        VirtProvisioningEvent.MetricsAvailable(
                            this@SimVirtProvisioningService,
                            hypervisors.size,
                            availableHypervisors.size,
                            submittedVms,
                            runningVms,
                            finishedVms,
                            --queuedVms,
                            ++unscheduledVms
                        )
                    )

                    // Remove the incoming image
                    incomingImages.poll()

                    logger.warn("Failed to spawn ${imageInstance.image}: does not fit [${clock.millis()}]")
                    continue
                } else {
                    break
                }
            }

            try {
                logger.info { "[${clock.millis()}] Spawning ${imageInstance.image} on ${selectedHv.node.uid} ${selectedHv.node.name} ${selectedHv.node.flavor}" }
                incomingImages.poll()

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

                tracer.commit(VmScheduledEvent(imageInstance.name))

                eventFlow.emit(
                    VirtProvisioningEvent.MetricsAvailable(
                        this@SimVirtProvisioningService,
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

                                    tracer.commit(VmStoppedEvent(event.server.name))

                                    eventFlow.emit(
                                        VirtProvisioningEvent.MetricsAvailable(
                                            this@SimVirtProvisioningService,
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

    private fun stateChanged(node: Node, hypervisor: SimVirtDriver) {
        when (node.state) {
            NodeState.ACTIVE -> {
                logger.debug { "[${clock.millis()}] Server ${node.uid} available: ${node.state}" }

                if (node in hypervisors) {
                    // Corner case for when the hypervisor already exists
                    availableHypervisors += hypervisors.getValue(node)
                } else {
                    val hv = HypervisorView(
                        node.uid,
                        node,
                        0,
                        node.flavor.memorySize,
                        0
                    )
                    hv.driver = hypervisor
                    hv.driver.events
                        .onEach { event ->
                            if (event is HypervisorEvent.VmsUpdated) {
                                hv.numberOfActiveServers = event.numberOfActiveServers
                                hv.availableMemory = event.availableMemory
                            }
                        }.launchIn(coroutineScope)

                    maxCores = max(maxCores, node.flavor.cpuCount)
                    maxMemory = max(maxMemory, node.flavor.memorySize)
                    hypervisors[node] = hv
                    availableHypervisors += hv
                }

                tracer.commit(HypervisorAvailableEvent(node.uid))

                eventFlow.emit(
                    VirtProvisioningEvent.MetricsAvailable(
                        this@SimVirtProvisioningService,
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
            NodeState.SHUTOFF, NodeState.ERROR -> {
                logger.debug { "[${clock.millis()}] Server ${node.uid} unavailable: ${node.state}" }
                val hv = hypervisors[node] ?: return
                availableHypervisors -= hv

                tracer.commit(HypervisorUnavailableEvent(hv.uid))

                eventFlow.emit(
                    VirtProvisioningEvent.MetricsAvailable(
                        this@SimVirtProvisioningService,
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

    public data class ImageView(
        public val name: String,
        public val image: Image,
        public val flavor: Flavor,
        public val continuation: Continuation<Server>,
        public var server: Server? = null
    )
}
