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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    private val coroutineScope: CoroutineScope
) : VirtDriver {
    /**
     * The [Server] on which this hypervisor runs.
     */
    public val server: Server
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

    /**
     * A flag to indicate the driver is stopped.
     */
    private var stopped: Boolean = false

    /**
     * The set of [VmServerContext] instances that is being scheduled at the moment.
     */
    private val activeVms = mutableSetOf<VmServerContext>()

    /**
     * The deferred run call.
     */
    private var call: Job? = null

    /**
     * Schedule the vCPUs on the physical CPUs.
     */
    private suspend fun reschedule() {
        flush()

        // Do not schedule a call if there is no work to schedule or the driver stopped.
        if (stopped || activeVms.isEmpty()) {
            return
        }

        val call = coroutineScope.launch {
            val start = simulationContext.clock.millis()
            val vms = activeVms.toSet()

            var duration: Double = Double.POSITIVE_INFINITY
            var deadline: Long = Long.MAX_VALUE

            val maxUsage = hostContext.cpus.sumByDouble { it.frequency }
            var availableUsage = maxUsage
            val requests = vms.asSequence()
                .flatMap { it.requests.asSequence() }
                .sortedBy { it.limit }
                .toList()

            // Divide the available host capacity fairly across the vCPUs using max-min fair sharing
            for ((i, req) in requests.withIndex()) {
                val remaining = requests.size - i
                val availableShare = availableUsage / remaining
                val grantedUsage = min(req.limit, availableShare)

                req.allocatedUsage = grantedUsage
                availableUsage -= grantedUsage

                // The duration that we want to run is that of the shortest request from a vCPU
                duration = min(duration, req.burst / req.allocatedUsage)
                deadline = min(deadline, req.vm.deadline)
            }

            val usage = DoubleArray(hostContext.cpus.size)
            val burst = LongArray(hostContext.cpus.size)
            val totalUsage = maxUsage - availableUsage
            availableUsage = totalUsage
            val serverLoad = totalUsage / maxUsage

            // Divide the requests over the available capacity of the pCPUs fairly
            for (i in hostContext.cpus.indices.sortedBy { hostContext.cpus[it].frequency }) {
                val remaining = hostContext.cpus.size - i
                val availableShare = availableUsage / remaining
                val grantedUsage = min(hostContext.cpus[i].frequency, availableShare)

                usage[i] = grantedUsage
                burst[i] = (duration * grantedUsage).toLong()
                availableUsage -= grantedUsage
            }

            val remainder = burst.clone()
            // We run the total burst on the host processor. Note that this call may be cancelled at any moment in
            // time, so not all of the burst may be executed.
            hostContext.run(remainder, usage, deadline)
            val end = simulationContext.clock.millis()

            // No work was performed
            if ((end - start) <= 0) {
                return@launch
            }

            val totalRemainder = remainder.sum()
            val totalBurst = burst.sum()
            val imagesRunning = vms.map { it.server.image }.toSet()

            for (vm in vms) {
                // Apply performance interference model
                val performanceModel =
                    vm.server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?
                val performanceScore = performanceModel?.apply(imagesRunning, serverLoad) ?: 1.0

                for ((i, req) in vm.requests.withIndex()) {
                    // Compute the fraction of compute time allocated to the VM
                    val fraction = req.allocatedUsage / totalUsage

                    // Derive the burst that was allocated to this vCPU
                    val allocatedBurst = ceil(duration * req.allocatedUsage).toLong()

                    // Compute the burst time that the VM was actually granted
                    val grantedBurst = (performanceScore * (allocatedBurst - ceil(totalRemainder * fraction))).toLong()

                    // Compute remaining burst time to be executed for the request
                    req.burst = max(0, vm.burst[i] - grantedBurst)
                    vm.burst[i] = req.burst
                }

                if (vm.burst.any { it == 0L } || vm.deadline <= end) {
                    // Return vCPU `run` call: the requested burst was completed or deadline was exceeded
                    vm.chan.send(Unit)
                }
            }

            eventFlow.emit(HypervisorEvent.SliceFinished(this@SimpleVirtDriver, totalBurst, totalBurst - totalRemainder, vms.size))
        }
        this.call = call
    }

    /**
     * Flush the progress of the current active VMs.
     */
    private suspend fun flush() {
        val call = call ?: return // If there is no active call, there is nothing to flush
        // The progress is actually flushed in the coroutine when it notices: we cancel it and wait for its
        // completion.
        call.cancelAndJoin()
        this.call = null
    }

    /**
     * A request to schedule a virtual CPU on the host cpu.
     */
    internal data class CpuRequest(
        val vm: VmServerContext,
        val vcpu: ProcessingUnit,
        var burst: Long,
        val limit: Double
    ) {
        /**
         * The usage that was actually granted.
         */
        var allocatedUsage: Double = 0.0
    }

    internal inner class VmServerContext(
        server: Server,
        val events: EventFlow<ServerEvent>,
        val domain: Domain
    ) : ServerManagementContext {
        private var finalized: Boolean = false
        lateinit var requests: List<CpuRequest>
        lateinit var burst: LongArray
        var deadline: Long = 0L
        var chan = Channel<Unit>(Channel.RENDEZVOUS)
        private var initialized: Boolean = false

        internal val job: Job = coroutineScope.launch {
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
            events.close()
            eventFlow.emit(HypervisorEvent.VmsUpdated(this@SimpleVirtDriver, vms.size, availableMemory))
        }

        override suspend fun run(burst: LongArray, limit: DoubleArray, deadline: Long) {
            require(burst.size == limit.size) { "Array dimensions do not match" }

            this.deadline = deadline
            this.burst = burst
            requests = cpus.asSequence()
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
                activeVms += this
                reschedule()
                chan.receive()
            } catch (e: CancellationException) {
                // On cancellation, we compute and return the remaining burst
                e.assertFailure()
            } finally {
                activeVms -= this
                reschedule()
            }
        }
    }
}
