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
import com.atlarge.opendc.compute.core.workload.IMAGE_PERF_INTERFERENCE_MODEL
import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.select
import java.util.Objects
import java.util.TreeSet
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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
        vmStarted(server)
        eventFlow.emit(HypervisorEvent.VmsUpdated(this, vms.size, availableMemory))
        return server
    }

    internal fun cancel() {
        eventFlow.close()
    }

    private fun vmStarted(server: Server) {
        vms.forEach {
            val performanceModel =
                it.server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?
            performanceModel?.vmStarted(server)
        }
    }

    private fun vmStopped(server: Server) {
        vms.forEach {
            val performanceModel =
                it.server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?
            performanceModel?.vmStopped(server)
        }
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
        val requests = TreeSet(cpuRequestComparator)

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

            val vmCount = vms.size
            var duration: Double = Double.POSITIVE_INFINITY
            var deadline: Long = Long.MAX_VALUE
            var availableUsage = maxUsage
            var totalRequestedUsage = 0.0
            var totalRequestedBurst = 0L

            // Divide the available host capacity fairly across the vCPUs using max-min fair sharing
            for ((i, req) in requests.withIndex()) {
                val remaining = requests.size - i
                val availableShare = availableUsage / remaining
                val grantedUsage = min(req.limit, availableShare)

                totalRequestedUsage += req.limit
                totalRequestedBurst += req.burst

                req.allocatedUsage = grantedUsage
                availableUsage -= grantedUsage

                // The duration that we want to run is that of the shortest request from a vCPU
                duration = min(duration, req.burst / grantedUsage)
                deadline = min(deadline, req.vm.deadline)
            }

            // XXX We set the minimum duration to 5 minutes here to prevent the rounding issues that are occurring with the FLOPs.
            duration = 300.0

            val totalAllocatedUsage = maxUsage - availableUsage
            var totalAllocatedBurst = 0L
            availableUsage = totalAllocatedUsage
            val serverLoad = totalAllocatedUsage / maxUsage

            // Divide the requests over the available capacity of the pCPUs fairly
            for (i in pCPUs) {
                val maxCpuUsage = hostContext.cpus[i].frequency
                val fraction = maxCpuUsage / maxUsage
                val grantedUsage = min(maxCpuUsage, totalAllocatedUsage * fraction)
                val grantedBurst = ceil(duration * grantedUsage).toLong()

                usage[i] = grantedUsage
                burst[i] = grantedBurst
                totalAllocatedBurst += grantedBurst
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

            // The total requested burst that the VMs wanted to run in the time-frame that we ran.
            val totalRequestedSubBurst = requests.map { ceil((duration * 1000) / (it.vm.deadline - start) * it.burst).toLong() }.sum()
            val totalRemainder = burst.sum()
            val totalGrantedBurst = totalAllocatedBurst - totalRemainder

            // The burst that was lost due to overcommissioning of CPU resources
            var totalOvercommissionedBurst = 0L
            // The burst that was lost due to interference.
            var totalInterferedBurst = 0L

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
                    val fraction = req.allocatedUsage / totalAllocatedUsage

                    // Compute the burst time that the VM was actually granted
                    val grantedBurst = ceil(totalGrantedBurst * fraction).toLong()

                    // The burst that was actually used by the VM
                    val usedBurst = ceil(grantedBurst * performanceScore).toLong()

                    totalInterferedBurst += grantedBurst - usedBurst

                    // Compute remaining burst time to be executed for the request
                    req.burst = max(0, vm.burst[i] - usedBurst)
                    vm.burst[i] = req.burst

                    if (req.burst <= 0L || req.isCancelled) {
                        hasFinished = true
                    } else if (vm.deadline <= end && hostContext.server.state != ServerState.ERROR) {
                        // Request must have its entire burst consumed or otherwise we have overcommission
                        // Note that we count the overcommissioned burst if the hypervisor has failed.
                        totalOvercommissionedBurst += req.burst
                    }
                }

                if (hasFinished || vm.deadline <= end) {
                    // Deschedule all requests from this VM
                    entryIterator.remove()
                    requests.removeAll(vmRequests)

                    // Return vCPU `run` call: the requested burst was completed or deadline was exceeded
                    vm.chan?.resume(Unit)
                }
            }

            eventFlow.emit(
                HypervisorEvent.SliceFinished(
                    this@SimpleVirtDriver,
                    totalRequestedBurst,
                    min(totalRequestedSubBurst, totalGrantedBurst), // We can run more than requested due to timing
                    totalOvercommissionedBurst,
                    totalInterferedBurst, // Might be smaller than zero due to FP rounding errors,
                    totalAllocatedUsage,
                    totalRequestedUsage,
                    vmCount, // Some VMs might already have finished, so keep initial VM count
                    server
                )
            )
        }
    }

    /**
     * The [Comparator] for [CpuRequest].
     */
    private val cpuRequestComparator: Comparator<CpuRequest> = Comparator { lhs, rhs ->
        var cmp = lhs.limit.compareTo(rhs.limit)

        if (cmp != 0) {
            return@Comparator cmp
        }

        cmp = lhs.vm.server.uid.compareTo(rhs.vm.server.uid)

        if (cmp != 0) {
            return@Comparator cmp
        }

        lhs.vcpu.id.compareTo(rhs.vcpu.id)
    }

    /**
     * A request to schedule a virtual CPU on the host cpu.
     */
    internal data class CpuRequest(
        val vm: VmServerContext,
        val vcpu: ProcessingUnit,
        var burst: Long,
        var limit: Double
    ) {
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
    }

    internal inner class VmServerContext(
        server: Server,
        val events: EventFlow<ServerEvent>,
        val domain: Domain
    ) : ServerManagementContext {
        private var finalized: Boolean = false
        lateinit var burst: LongArray
        var deadline: Long = 0L
        var chan: Continuation<Unit>? = null
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
            vmStopped(server)
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
                schedulingQueue.offer(SchedulerCommand.Schedule(this, requests))
                suspendCoroutine<Unit> { chan = it }
            } catch (e: CancellationException) {
                // Deschedule the VM
                requests.forEach { it.isCancelled = true }
                schedulingQueue.offer(SchedulerCommand.Interrupt)
                suspendCoroutine<Unit> { chan = it }
                e.assertFailure()
            }
        }

        @OptIn(InternalCoroutinesApi::class)
        override fun onRun(burst: LongArray, limit: DoubleArray, deadline: Long): SelectClause0 = TODO()
    }
}
