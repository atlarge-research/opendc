/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.virt.driver

import com.atlarge.odcsim.Domain
import com.atlarge.odcsim.flow.EventFlow
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.execution.ShutdownException
import com.atlarge.opendc.compute.core.execution.assertFailure
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.virt.HypervisorEvent
import com.atlarge.opendc.core.services.ServiceKey
import com.atlarge.opendc.core.services.ServiceRegistry
import com.atlarge.opendc.core.workload.IMAGE_PERF_INTERFERENCE_MODEL
import com.atlarge.opendc.core.workload.PerformanceInterferenceModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.Objects
import java.util.TreeSet
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A [VirtDriver] that is backed by a simple hypervisor implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SimpleVirtDriver(
    private val hostContext: ServerContext,
    scope: CoroutineScope
) : VirtDriver, CoroutineScope by scope {
    /**
     * The [Server] on which this hypervisor runs.
     */
    val server: Server
        get() = hostContext.server

    /**
     * A set for tracking the VM context objects.
     */
    internal val vms: MutableSet<VmServerContext> = mutableSetOf()

    /**
     * Current total memory use of the images on this hypervisor.
     */
    private var availableMemory: Long = hostContext.server.flavor.memorySize

    /**
     * The [EventFlow] to emit the events.
     */
    internal val eventFlow = EventFlow<HypervisorEvent>()

    override val events: Flow<HypervisorEvent> = eventFlow

    init {
        events.filter { it is HypervisorEvent.VmsUpdated }.onEach {
            val imagesRunning = vms.map { it.server.image }.toSet()
            vms.forEach {
                val performanceModel =
                    it.server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?
                performanceModel?.computeIntersectingItems(imagesRunning)
            }
        }.launchIn(this)

        launch {
            try {
                scheduler()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    simulationContext.log.error("Hypervisor scheduler failed", e)
                }
                throw e
            }
        }
    }

    override suspend fun spawn(
        name: String,
        image: Image,
        flavor: Flavor
    ): Server {
        val requiredMemory = flavor.memorySize
        if (availableMemory - requiredMemory < 0) {
            throw InsufficientMemoryOnServerException()
        }
        require(flavor.cpuCount <= hostContext.server.flavor.cpuCount) { "Machine does not fit" }

        val events = EventFlow<ServerEvent>()
        val server = Server(
            UUID.randomUUID(), name, emptyMap(), flavor, image, ServerState.BUILD,
            ServiceRegistry(), events
        )
        availableMemory -= requiredMemory
        vms.add(VmServerContext(server, events, simulationContext.domain))
        eventFlow.emit(HypervisorEvent.VmsUpdated(this, vms.size, availableMemory))
        return server
    }

    internal fun cancel() {
        eventFlow.close()
    }

    /**
     * A scheduling command processed by the scheduler.
     */
    private sealed class SchedulerCommand {
        /**
         * Schedule the specified vCPUs of a single VM.
         */
        data class Schedule(val vm: VmServerContext, val requests: Collection<CpuRequest>) : SchedulerCommand()

        /**
         * Interrupt the scheduler.
         */
        object Interrupt : SchedulerCommand()
    }

    /**
     * A flag to indicate the driver is stopped.
     */
    private var stopped: Boolean = false

    /**
     * The channel for scheduling new CPU requests.
     */
    private val schedulingQueue = Channel<SchedulerCommand>(Channel.UNLIMITED)

    /**
     * The scheduling process of the hypervisor.
     */
    private suspend fun scheduler() {
        val clock = simulationContext.clock
        val maxUsage = hostContext.cpus.sumByDouble { it.frequency }
        val pCPUs = hostContext.cpus.indices.sortedBy { hostContext.cpus[it].frequency }

        val vms = mutableMapOf<VmServerContext, Collection<CpuRequest>>()
        val requests = TreeSet<CpuRequest>()

        val usage = DoubleArray(hostContext.cpus.size)
        val burst = LongArray(hostContext.cpus.size)

        fun process(command: SchedulerCommand) {
            when (command) {
                is SchedulerCommand.Schedule -> {
                    vms[command.vm] = command.requests
                    requests.removeAll(command.requests)
                    requests.addAll(command.requests)
                }
            }
        }

        fun processRemaining() {
            var command = schedulingQueue.poll()
            while (command != null) {
                process(command)
                command = schedulingQueue.poll()
            }
        }

        while (!stopped) {
            // Wait for a request to be submitted if we have no work yet.
            if (requests.isEmpty()) {
                process(schedulingQueue.receive())
            }

            processRemaining()

            val start = clock.millis()

            var duration: Double = Double.POSITIVE_INFINITY
            var deadline: Long = Long.MAX_VALUE
            var availableUsage = maxUsage

            // Divide the available host capacity fairly across the vCPUs using max-min fair sharing
            for ((i, req) in requests.withIndex()) {
                val remaining = requests.size - i
                val availableShare = availableUsage / remaining
                val grantedUsage = min(req.limit, availableShare)

                req.allocatedUsage = grantedUsage
                availableUsage -= grantedUsage

                // The duration that we want to run is that of the shortest request from a vCPU
                duration = min(duration, req.burst / grantedUsage)
                deadline = min(deadline, req.vm.deadline)
            }

            val totalUsage = maxUsage - availableUsage
            var totalBurst = 0L
            availableUsage = totalUsage
            val serverLoad = totalUsage / maxUsage

            // Divide the requests over the available capacity of the pCPUs fairly
            for (i in pCPUs) {
                val remaining = hostContext.cpus.size - i
                val availableShare = availableUsage / remaining
                val grantedUsage = min(hostContext.cpus[i].frequency, availableShare)
                val pBurst = (duration * grantedUsage).toLong()

                usage[i] = grantedUsage
                burst[i] = pBurst
                totalBurst += pBurst
                availableUsage -= grantedUsage
            }

            // We run the total burst on the host processor. Note that this call may be cancelled at any moment in
            // time, so not all of the burst may be executed.
            val interrupted = select<Boolean> {
                schedulingQueue.onReceive { schedulingQueue.offer(it); true }
                hostContext.onRun(burst, usage, deadline).invoke { false }
            }

            val end = clock.millis()

            // No work was performed
            if ((end - start) <= 0) {
                continue
            }

            val totalRemainder = burst.sum()

            val entryIterator = vms.entries.iterator()
            while (entryIterator.hasNext()) {
                val (vm, vmRequests) = entryIterator.next()

                // Apply performance interference model
                val performanceModel =
                    vm.server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?
                val performanceScore = performanceModel?.apply(serverLoad) ?: 1.0
                var hasFinished = false

                for ((i, req) in vmRequests.withIndex()) {
                    // Compute the fraction of compute time allocated to the VM
                    val fraction = req.allocatedUsage / totalUsage

                    // Derive the burst that was allocated to this vCPU
                    val allocatedBurst = ceil(totalBurst * fraction).toLong()

                    // Compute the burst time that the VM was actually granted
                    val grantedBurst = (performanceScore * (allocatedBurst - ceil(totalRemainder * fraction))).toLong()

                    // Compute remaining burst time to be executed for the request
                    req.burst = max(0, vm.burst[i] - grantedBurst)
                    vm.burst[i] = req.burst

                    if (req.burst <= 0L || req.isCancelled) {
                        hasFinished = true
                    }
                }

                if (hasFinished || vm.deadline <= end) {
                    // Deschedule all requests from this VM
                    entryIterator.remove()
                    requests.removeAll(vmRequests)

                    // Return vCPU `run` call: the requested burst was completed or deadline was exceeded
                    vm.chan.send(Unit)
                }
            }

            eventFlow.emit(
                HypervisorEvent.SliceFinished(
                    this@SimpleVirtDriver,
                    totalBurst,
                    totalBurst - totalRemainder,
                    vms.size,
                    server
                )
            )
        }
    }

    /**
     * A request to schedule a virtual CPU on the host cpu.
     */
    internal data class CpuRequest(
        val vm: VmServerContext,
        val vcpu: ProcessingUnit,
        var burst: Long,
        val limit: Double
    ) : Comparable<CpuRequest> {
        /**
         * The usage that was actually granted.
         */
        var allocatedUsage: Double = 0.0

        /**
         * A flag to indicate the request was cancelled.
         */
        var isCancelled: Boolean = false

        override fun equals(other: Any?): Boolean = other is CpuRequest && vm == other.vm && vcpu == other.vcpu
        override fun hashCode(): Int = Objects.hash(vm, vcpu)

        override fun compareTo(other: CpuRequest): Int {
            var cmp = limit.compareTo(other.limit)

            if (cmp != 0) {
                return cmp
            }

            cmp = vm.server.uid.compareTo(other.vm.server.uid)

            if (cmp != 0) {
                return cmp
            }

            return vcpu.id.compareTo(other.vcpu.id)
        }
    }

    internal inner class VmServerContext(
        server: Server,
        val events: EventFlow<ServerEvent>,
        val domain: Domain
    ) : ServerManagementContext {
        private var finalized: Boolean = false
        lateinit var burst: LongArray
        var deadline: Long = 0L
        var chan = Channel<Unit>(Channel.RENDEZVOUS)
        private var initialized: Boolean = false

        internal val job: Job = launch {
            delay(1) // TODO Introduce boot time
            init()
            try {
                server.image(this@VmServerContext)
                exit()
            } catch (cause: Throwable) {
                exit(cause)
            }
        }

        override var server: Server = server
            set(value) {
                if (field.state != value.state) {
                    events.emit(ServerEvent.StateChanged(value, field.state))
                }

                field = value
            }

        override val cpus: List<ProcessingUnit> = hostContext.cpus.take(server.flavor.cpuCount)

        override suspend fun <T : Any> publishService(key: ServiceKey<T>, service: T) {
            server = server.copy(services = server.services.put(key, service))
            events.emit(ServerEvent.ServicePublished(server, key))
        }

        override suspend fun init() {
            assert(!finalized) { "VM is already finalized" }

            server = server.copy(state = ServerState.ACTIVE)
            initialized = true
        }

        override suspend fun exit(cause: Throwable?) {
            finalized = true

            val serverState =
                if (cause == null || (cause is ShutdownException && cause.cause == null))
                    ServerState.SHUTOFF
                else
                    ServerState.ERROR
            server = server.copy(state = serverState)
            availableMemory += server.flavor.memorySize
            vms.remove(this)
            eventFlow.emit(HypervisorEvent.VmsUpdated(this@SimpleVirtDriver, vms.size, availableMemory))
            events.close()
        }

        override suspend fun run(burst: LongArray, limit: DoubleArray, deadline: Long) {
            require(burst.size == limit.size) { "Array dimensions do not match" }

            this.deadline = deadline
            this.burst = burst
            val requests = cpus.asSequence()
                .take(burst.size)
                .mapIndexed { i, cpu ->
                    CpuRequest(
                        this,
                        cpu,
                        burst[i],
                        limit[i]
                    )
                }
                .toList()

            // Wait until the burst has been run or the coroutine is cancelled
            try {
                schedulingQueue.send(SchedulerCommand.Schedule(this, requests))
                chan.receive()
            } catch (e: CancellationException) {
                // Deschedule the VM
                withContext(NonCancellable) {
                    requests.forEach { it.isCancelled = true }
                    schedulingQueue.send(SchedulerCommand.Interrupt)
                    chan.receive()
                }

                e.assertFailure()
            }
        }

        @OptIn(InternalCoroutinesApi::class)
        override fun onRun(burst: LongArray, limit: DoubleArray, deadline: Long): SelectClause0 = TODO()
    }
}
