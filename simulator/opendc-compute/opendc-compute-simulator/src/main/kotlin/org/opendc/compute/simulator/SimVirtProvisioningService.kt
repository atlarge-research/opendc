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
import org.opendc.compute.core.*
import org.opendc.compute.core.image.Image
import org.opendc.compute.core.metal.Node
import org.opendc.compute.core.metal.NodeEvent
import org.opendc.compute.core.metal.NodeState
import org.opendc.compute.core.metal.service.ProvisioningService
import org.opendc.compute.core.virt.*
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
) : VirtProvisioningService, HostListener {
    /**
     * The logger instance to use.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * A mapping from host to hypervisor view.
     */
    private val hostToHv = mutableMapOf<Host, HypervisorView>()

    /**
     * The hypervisors that have been launched by the service.
     */
    private val hypervisors: MutableMap<Node, HypervisorView> = mutableMapOf()

    /**
     * The available hypervisors.
     */
    private val availableHypervisors: MutableSet<HypervisorView> = mutableSetOf()

    /**
     * The servers that should be launched by the service.
     */
    private val queue: Deque<LaunchRequest> = ArrayDeque()

    /**
     * The active servers in the system.
     */
    private val activeServers: MutableSet<Server> = mutableSetOf()

    /**
     * The [Random] instance used to generate unique identifiers for the objects.
     */
    private val random = Random(0)

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

    override val events: Flow<VirtProvisioningEvent>
        get() = _events
    private val _events = EventFlow<VirtProvisioningEvent>()

    /**
     * The [TimerScheduler] to use for scheduling the scheduler cycles.
     */
    private var scheduler: TimerScheduler<Unit> = TimerScheduler(coroutineScope, clock)

    init {
        coroutineScope.launch {
            val provisionedNodes = provisioningService.nodes()
            provisionedNodes.forEach { node ->
                val workload = SimHost(UUID(random.nextLong(), random.nextLong()), coroutineScope, hypervisor)
                workload.addListener(this@SimVirtProvisioningService)
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

    override suspend fun drivers(): Set<Host> {
        return availableHypervisors.map { it.driver }.toSet()
    }

    override val hostCount: Int = hypervisors.size

    override suspend fun deploy(
        name: String,
        image: Image,
        flavor: Flavor
    ): Server {
        tracer.commit(VmSubmissionEvent(name, image, flavor))

        _events.emit(
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

        return suspendCancellableCoroutine { cont ->
            val request = LaunchRequest(createServer(name, image, flavor), cont)
            queue += request
            requestCycle()
        }
    }

    override suspend fun terminate() {
        val provisionedNodes = provisioningService.nodes()
        provisionedNodes.forEach { node -> provisioningService.stop(node) }
    }

    private fun createServer(
        name: String,
        image: Image,
        flavor: Flavor
    ): Server {
        return ServerImpl(
            uid = UUID(random.nextLong(), random.nextLong()),
            name = name,
            flavor = flavor,
            image = image
        )
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
            schedule()
        }
    }

    private fun schedule() {
        while (queue.isNotEmpty()) {
            val (server, cont) = queue.peekFirst()
            val requiredMemory = server.flavor.memorySize
            val selectedHv = allocationLogic.select(availableHypervisors, server)

            if (selectedHv == null || !selectedHv.driver.canFit(server)) {
                logger.trace { "Server $server selected for scheduling but no capacity available for it." }

                if (requiredMemory > maxMemory || server.flavor.cpuCount > maxCores) {
                    tracer.commit(VmSubmissionInvalidEvent(server.name))

                    _events.emit(
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
                    queue.poll()

                    logger.warn("Failed to spawn $server: does not fit [${clock.millis()}]")
                    continue
                } else {
                    break
                }
            }

            try {
                logger.info { "[${clock.millis()}] Spawning $server on ${selectedHv.node.uid} ${selectedHv.node.name} ${selectedHv.node.flavor}" }
                queue.poll()

                // Speculatively update the hypervisor view information to prevent other images in the queue from
                // deciding on stale values.
                selectedHv.numberOfActiveServers++
                selectedHv.provisionedCores += server.flavor.cpuCount
                selectedHv.availableMemory -= requiredMemory // XXX Temporary hack

                coroutineScope.launch {
                    try {
                        cont.resume(ClientServer(server))
                        selectedHv.driver.spawn(server)
                        activeServers += server

                        tracer.commit(VmScheduledEvent(server.name))
                        _events.emit(
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
                    } catch (e: InsufficientMemoryOnServerException) {
                        logger.error("Failed to deploy VM", e)

                        selectedHv.numberOfActiveServers--
                        selectedHv.provisionedCores -= server.flavor.cpuCount
                        selectedHv.availableMemory += requiredMemory
                    }
                }
            } catch (e: Throwable) {
                logger.error("Failed to deploy VM", e)
            }
        }
    }

    private fun stateChanged(node: Node, hypervisor: SimHost) {
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
                            if (event is HostEvent.VmsUpdated) {
                                hv.numberOfActiveServers = event.numberOfActiveServers
                                hv.availableMemory = event.availableMemory
                            }
                        }.launchIn(coroutineScope)

                    maxCores = max(maxCores, node.flavor.cpuCount)
                    maxMemory = max(maxMemory, node.flavor.memorySize)
                    hypervisors[node] = hv
                    hostToHv[hypervisor] = hv
                    availableHypervisors += hv
                }

                tracer.commit(HypervisorAvailableEvent(node.uid))

                _events.emit(
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
                if (queue.isNotEmpty()) {
                    requestCycle()
                }
            }
            NodeState.SHUTOFF, NodeState.ERROR -> {
                logger.debug { "[${clock.millis()}] Server ${node.uid} unavailable: ${node.state}" }
                val hv = hypervisors[node] ?: return
                availableHypervisors -= hv

                tracer.commit(HypervisorUnavailableEvent(hv.uid))

                _events.emit(
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

                if (queue.isNotEmpty()) {
                    requestCycle()
                }
            }
            else -> throw IllegalStateException()
        }
    }

    override fun onStateChange(host: Host, server: Server, newState: ServerState) {
        val serverImpl = server as ServerImpl
        serverImpl.state = newState
        serverImpl.watchers.forEach { it.onStateChanged(server, newState) }

        if (newState == ServerState.SHUTOFF) {
            logger.info { "[${clock.millis()}] Server ${server.uid} ${server.name} ${server.flavor} finished." }

            tracer.commit(VmStoppedEvent(server.name))

            _events.emit(
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

            activeServers -= server
            val hv = hostToHv[host]
            if (hv != null) {
                hv.provisionedCores -= server.flavor.cpuCount
            } else {
                logger.error { "Unknown host $host" }
            }

            // Try to reschedule if needed
            if (queue.isNotEmpty()) {
                requestCycle()
            }
        }
    }

    public data class LaunchRequest(val server: Server, val cont: Continuation<Server>)

    private class ServerImpl(
        override val uid: UUID,
        override val name: String,
        override val flavor: Flavor,
        override val image: Image
    ) : Server {
        val watchers = mutableListOf<ServerWatcher>()

        override fun watch(watcher: ServerWatcher) {
            watchers += watcher
        }

        override fun unwatch(watcher: ServerWatcher) {
            watchers -= watcher
        }

        override suspend fun refresh() {
            // No-op: this object is the source-of-truth
        }

        override val tags: Map<String, String> = emptyMap()

        override var state: ServerState = ServerState.BUILD
    }
}
