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
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.virt.HypervisorEvent
import com.atlarge.opendc.core.services.ServiceKey
import com.atlarge.opendc.core.services.ServiceRegistry
import com.atlarge.opendc.compute.core.workload.IMAGE_PERF_INTERFERENCE_MODEL
import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectInstance
import kotlinx.coroutines.selects.select
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
    private val vms: MutableSet<VmServerContext> = mutableSetOf()

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
        vms.add(VmServerContext(server, events))
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
         * Refresh the dirty datastructures of the specified VM.
         */
        data class Refresh(val vm: Vm) : SchedulerCommand()

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

        val vms = mutableSetOf<Vm>()
        val vcpus = TreeSet<VCpu>()

        val usage = DoubleArray(hostContext.cpus.size)
        val burst = LongArray(hostContext.cpus.size)

        fun process(command: SchedulerCommand) {
            when (command) {
                is SchedulerCommand.Refresh -> {
                    if (command.vm.isIdle) {
                        vms -= command.vm
                        vcpus.removeAll(command.vm.vcpus)
                    } else {
                        vms += command.vm
                        vcpus.removeAll(command.vm.vcpus)
                        vcpus.addAll(command.vm.vcpus)
                    }
                }
                is SchedulerCommand.Interrupt -> {}
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
            if (vcpus.isEmpty()) {
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
            for ((i, req) in vcpus.withIndex()) {
                val remaining = vcpus.size - i
                val availableShare = availableUsage / remaining
                val grantedUsage = min(req.limit, availableShare)

                // Take into account the minimum deadline of this slice before we possible continue
                deadline = min(deadline, req.vm.deadline)

                // Ignore empty CPUs
                if (grantedUsage <= 0 || req.burst <= 0) {
                    req.allocatedLimit = 0.0
                    continue
                }

                totalRequestedUsage += req.limit
                totalRequestedBurst += req.burst

                req.allocatedLimit = grantedUsage
                availableUsage -= grantedUsage

                // The duration that we want to run is that of the shortest request from a vCPU
                duration = min(duration, req.burst / grantedUsage)
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
            select<Boolean> {
                schedulingQueue.onReceive { schedulingQueue.offer(it); true }
                hostContext.onRun(ServerContext.Slice(burst, usage, deadline), ServerContext.TriggerMode.DEADLINE).invoke { false }
            }

            val end = clock.millis()

            // No work was performed
            if ((end - start) <= 0) {
                continue
            }

            // The total requested burst that the VMs wanted to run in the time-frame that we ran.
            val totalRequestedSubBurst = vcpus.map { ceil((duration * 1000) / (it.vm.deadline - start) * it.burst).toLong() }.sum()
            val totalRemainder = burst.sum()
            val totalGrantedBurst = totalAllocatedBurst - totalRemainder

            // The burst that was lost due to overcommissioning of CPU resources
            var totalOvercommissionedBurst = 0L
            // The burst that was lost due to interference.
            var totalInterferedBurst = 0L

            val vmIterator = vms.iterator()
            while (vmIterator.hasNext()) {
                val vm = vmIterator.next()

                // Apply performance interference model
                val performanceModel =
                    vm.ctx.server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?
                val performanceScore = performanceModel?.apply(serverLoad) ?: 1.0
                var hasFinished = false

                for (vcpu in vm.vcpus) {
                    // Compute the fraction of compute time allocated to the VM
                    val fraction = vcpu.allocatedLimit / totalAllocatedUsage

                    // Compute the burst time that the VM was actually granted
                    val grantedBurst = ceil(totalGrantedBurst * fraction).toLong()

                    // The burst that was actually used by the VM
                    val usedBurst = ceil(grantedBurst * performanceScore).toLong()

                    totalInterferedBurst += grantedBurst - usedBurst

                    // Compute remaining burst time to be executed for the request
                    if (vcpu.consume(usedBurst)) {
                        hasFinished = true
                    } else if (vm.deadline <= end && hostContext.server.state != ServerState.ERROR) {
                        // Request must have its entire burst consumed or otherwise we have overcommission
                        // Note that we count the overcommissioned burst if the hypervisor has failed.
                        totalOvercommissionedBurst += vcpu.burst
                    }
                }

                if (hasFinished || vm.deadline <= end) {
                    vcpus.removeAll(vm.vcpus)
                    // Mark the VM as finished and deschedule the VMs if needed
                    if (vm.finish()) {
                        vmIterator.remove()
                    } else {
                        vcpus.addAll(vm.vcpus)
                    }
                }
            }

            eventFlow.emit(
                HypervisorEvent.SliceFinished(
                    this@SimpleVirtDriver,
                    totalRequestedBurst,
                    min(totalRequestedSubBurst, totalGrantedBurst), // We can run more than requested due to timing
                    totalOvercommissionedBurst,
                    totalInterferedBurst, // Might be smaller than zero due to FP rounding errors,
                    min(totalAllocatedUsage, totalRequestedUsage), // The allocated usage might be slightly higher due to FP rounding
                    totalRequestedUsage,
                    vmCount, // Some VMs might already have finished, so keep initial VM count
                    server
                )
            )
        }
    }

    /**
     * A virtual machine running on the hypervisor.
     *
     * @param ctx The execution context the vCPU runs in.
     * @param triggerMode The mode when to trigger the VM exit.
     * @param merge The function to merge consecutive slices on spillover.
     * @param select The function to select on finish.
     */
    @OptIn(InternalCoroutinesApi::class)
    private data class Vm(
        val ctx: VmServerContext,
        var triggerMode: ServerContext.TriggerMode = ServerContext.TriggerMode.FIRST,
        var merge: (ServerContext.Slice, ServerContext.Slice) -> ServerContext.Slice = { _, r -> r },
        var select: () -> Unit = {}
    ) {
        /**
         * The vCPUs of this virtual machine.
         */
        val vcpus: List<VCpu>

        /**
         * The slices that the VM wants to run.
         */
        var queue: Iterator<ServerContext.Slice> = emptyList<ServerContext.Slice>().iterator()

        /**
         * The current active slice.
         */
        var activeSlice: ServerContext.Slice? = null

        /**
         * The current deadline of the VM.
         */
        val deadline: Long
            get() = activeSlice?.deadline ?: Long.MAX_VALUE

        /**
         * A flag to indicate that the VM is idle.
         */
        val isIdle: Boolean
            get() = activeSlice == null

        init {
            vcpus = ctx.cpus.mapIndexed { i, model -> VCpu(this, model, i) }
        }

        /**
         * Schedule the given slices on this vCPU, replacing the existing slices.
         */
        fun schedule(slices: List<ServerContext.Slice>) {
            queue = slices.iterator()

            if (queue.hasNext()) {
                activeSlice = queue.next()
            }
        }

        /**
         * Cancel the existing workload on the VM.
         */
        fun cancel() {
            queue = emptyList<ServerContext.Slice>().iterator()
            activeSlice = null
        }

        /**
         * Finish the current slice of the VM.
         *
         * @return `true` if the vCPUs may be descheduled, `false` otherwise.
         */
        fun finish(): Boolean {
            val activeSlice = activeSlice ?: return true

            return if (queue.hasNext()) {
                val needsMerge = activeSlice.burst.any { it > 0 }
                val candidateSlice = queue.next()
                val slice = if (needsMerge) merge(activeSlice, candidateSlice) else candidateSlice

                this.activeSlice = slice
                false
            } else {
                this.activeSlice = null
                select()
                true
            }
        }
    }

    /**
     * A virtual CPU that can be scheduled on a physical CPU.
     *
     * @param vm The VM of which this vCPU is part.
     * @param model The model of CPU that this vCPU models.
     * @param id The id of the vCPU with respect to the VM.
     */
    private data class VCpu(
        val vm: Vm,
        val model: ProcessingUnit,
        val id: Int
    ) : Comparable<VCpu> {
        /**
         * The current limit on the vCPU.
         */
        val limit: Double
            get() = vm.activeSlice?.limit?.takeIf { id < it.size }?.get(id) ?: 0.0

        /**
         * The limit allocated by the hypervisor.
         */
        var allocatedLimit: Double = 0.0

        /**
         * The current burst running on the vCPU.
         */
        var burst: Long
            get() = vm.activeSlice?.burst?.takeIf { id < it.size }?.get(id) ?: 0
            set(value) {
                vm.activeSlice?.burst?.takeIf { id < it.size }?.set(id, value)
            }

        /**
         * Consume the specified burst on this vCPU.
         */
        fun consume(burst: Long): Boolean {
            this.burst = max(0, this.burst - burst)
            return this.burst == 0L
        }

        /**
         * Compare to another vCPU based on the current load of the vCPU.
         */
        override fun compareTo(other: VCpu): Int {
            var cmp = limit.compareTo(other.limit)

            if (cmp != 0) {
                return cmp
            }

            cmp = vm.ctx.server.uid.compareTo(other.vm.ctx.server.uid)

            if (cmp != 0) {
                return cmp
            }

            return id.compareTo(other.id)
        }

        /**
         * Create a string representation of the vCPU.
         */
        override fun toString(): String =
            "vCPU(vm=${vm.ctx.server.uid},id=$id,burst=$burst,limit=$limit,allocatedLimit=$allocatedLimit)"
    }


    /**
     * The execution context in which a VM runs.
     *
     * @param server The details of the VM.
     * @param events The event stream to publish to.
     */
    private inner class VmServerContext(server: Server, val events: EventFlow<ServerEvent>) : ServerManagementContext, DisposableHandle {
        private var finalized: Boolean = false
        private var initialized: Boolean = false
        private val vm: Vm

        internal val job: Job = launch {
            delay(1) // TODO Introduce boot time
            init()
            try {
                server.image(this@VmServerContext)
                exit()
            } catch (cause: Throwable) {
                cause.printStackTrace()
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

        init {
            vm = Vm(this)
        }

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

        @OptIn(InternalCoroutinesApi::class)
        override fun onRun(
            batch: List<ServerContext.Slice>,
            triggerMode: ServerContext.TriggerMode,
            merge: (ServerContext.Slice, ServerContext.Slice) -> ServerContext.Slice
        ): SelectClause0 = object : SelectClause0 {
            @InternalCoroutinesApi
            override fun <R> registerSelectClause0(select: SelectInstance<R>, block: suspend () -> R) {
                vm.triggerMode = triggerMode
                vm.merge = merge
                vm.select = {
                    if (select.trySelect()) {
                        block.startCoroutineCancellable(select.completion)
                    }
                }
                vm.schedule(batch)
                // Indicate to the hypervisor that the VM should be re-scheduled
                schedulingQueue.offer(SchedulerCommand.Refresh(vm))
                select.disposeOnSelect(this@VmServerContext)
            }
        }

        override fun dispose() {
            if (!vm.isIdle) {
                vm.cancel()
                schedulingQueue.offer(SchedulerCommand.Refresh(vm))
            }
        }
    }
}
