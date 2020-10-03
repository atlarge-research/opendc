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

package org.opendc.simulator.compute

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectInstance
import kotlinx.coroutines.selects.select
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimWorkload
import java.time.Clock
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * SimHypervisor distributes the computing requirements of multiple [SimWorkload] on a single [SimBareMetalMachine] concurrently.
 *
 * @param coroutineScope The [CoroutineScope] to run the simulated workloads in.
 * @param clock The virtual clock to track the simulation time.
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
public class SimHypervisor(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
    private val listener: Listener? = null
) : SimWorkload {
    /**
     * A set for tracking the VM context objects.
     */
    private val vms: MutableSet<VmExecutionContext> = mutableSetOf()

    /**
     * A flag to indicate the driver is stopped.
     */
    private var stopped: Boolean = false

    /**
     * The channel for scheduling new CPU requests.
     */
    private val schedulingQueue = Channel<SchedulerCommand>(Channel.UNLIMITED)

    /**
     * Create a [SimMachine] instance on which users may run a [SimWorkload].
     *
     * @param model The machine to create.
     */
    public fun createMachine(model: SimMachineModel): SimMachine {
        val vmCtx = VmExecutionContext(model)

        return object : SimMachine {
            override val model: SimMachineModel
                get() = vmCtx.machine

            override val usage: StateFlow<Double>
                get() = vmCtx.session.usage

            /**
             * The current active workload.
             */
            private var activeWorkload: SimWorkload? = null

            override suspend fun run(workload: SimWorkload) {
                require(activeWorkload == null) { "Run should not be called concurrently" }

                try {
                    activeWorkload = workload
                    workload.run(vmCtx)
                } finally {
                    activeWorkload = null
                }
            }

            override fun toString(): String = "SimVirtualMachine"
        }
    }

    /**
     * Run the scheduling process of the hypervisor.
     */
    override suspend fun run(ctx: SimExecutionContext) {
        val maxUsage = ctx.machine.cpus.sumByDouble { it.frequency }
        val pCPUs = ctx.machine.cpus.indices.sortedBy { ctx.machine.cpus[it].frequency }

        val vms = mutableSetOf<VmSession>()
        val vcpus = mutableListOf<VCpu>()

        val usage = DoubleArray(ctx.machine.cpus.size)
        val burst = LongArray(ctx.machine.cpus.size)

        fun process(command: SchedulerCommand) {
            when (command) {
                is SchedulerCommand.Schedule -> {
                    vms += command.vm
                    vcpus.addAll(command.vm.vcpus)
                }
                is SchedulerCommand.Deschedule -> {
                    vms -= command.vm
                    vcpus.removeAll(command.vm.vcpus)
                }
                is SchedulerCommand.Interrupt -> {
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
            if (vcpus.isEmpty()) {
                process(schedulingQueue.receive())
            }

            processRemaining()

            val start = clock.millis()

            var duration: Double = Double.POSITIVE_INFINITY
            var deadline: Long = Long.MAX_VALUE
            var availableUsage = maxUsage
            var totalRequestedUsage = 0.0
            var totalRequestedBurst = 0L

            // Sort the vCPUs based on their requested usage
            // Profiling shows that it is faster to sort every slice instead of maintaining some kind of sorted set
            vcpus.sort()

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

            val totalAllocatedUsage = maxUsage - availableUsage
            var totalAllocatedBurst = 0L
            availableUsage = totalAllocatedUsage

            // Divide the requests over the available capacity of the pCPUs fairly
            for (i in pCPUs) {
                val maxCpuUsage = ctx.machine.cpus[i].frequency
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
                ctx.onRun(SimExecutionContext.Slice(burst, usage, deadline), SimExecutionContext.TriggerMode.DEADLINE)
                    .invoke { false }
            }

            val end = clock.millis()

            // No work was performed
            if ((end - start) <= 0) {
                continue
            }

            // The total requested burst that the VMs wanted to run in the time-frame that we ran.
            val totalRequestedSubBurst =
                vcpus.map { ceil((duration * 1000) / (it.vm.deadline - start) * it.burst).toLong() }.sum()
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
                val performanceScore = 1.0 // TODO Performance interference
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
                    } else if (vm.deadline <= end) {
                        // Request must have its entire burst consumed or otherwise we have overcommission
                        // Note that we count the overcommissioned burst if the hypervisor has failed.
                        totalOvercommissionedBurst += vcpu.burst
                    }
                }

                if (hasFinished || vm.deadline <= end) {
                    // Mark the VM as finished and deschedule the VMs if needed
                    if (vm.finish()) {
                        vmIterator.remove()
                        vcpus.removeAll(vm.vcpus)
                    }
                }
            }

            listener?.onSliceFinish(
                this,
                totalRequestedBurst,
                min(totalRequestedSubBurst, totalGrantedBurst), // We can run more than requested due to timing
                totalOvercommissionedBurst,
                totalInterferedBurst, // Might be smaller than zero due to FP rounding errors,
                min(
                    totalAllocatedUsage,
                    totalRequestedUsage
                ), // The allocated usage might be slightly higher due to FP rounding
                totalRequestedUsage
            )
        }
    }

    /**
     * Event listener for hypervisor events.
     */
    public interface Listener {
        /**
         * This method is invoked when a slice is finished.
         */
        public fun onSliceFinish(
            hypervisor: SimHypervisor,
            requestedBurst: Long,
            grantedBurst: Long,
            overcommissionedBurst: Long,
            interferedBurst: Long,
            cpuUsage: Double,
            cpuDemand: Double
        )
    }

    /**
     * A scheduling command processed by the scheduler.
     */
    private sealed class SchedulerCommand {
        /**
         * Schedule the specified VM on the hypervisor.
         */
        data class Schedule(val vm: VmSession) : SchedulerCommand()

        /**
         * De-schedule the specified VM on the hypervisor.
         */
        data class Deschedule(val vm: VmSession) : SchedulerCommand()

        /**
         * Interrupt the scheduler.
         */
        object Interrupt : SchedulerCommand()
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
    private data class VmSession(
        val model: SimMachineModel,
        var triggerMode: SimExecutionContext.TriggerMode = SimExecutionContext.TriggerMode.FIRST,
        var merge: (SimExecutionContext.Slice, SimExecutionContext.Slice) -> SimExecutionContext.Slice = { _, r -> r },
        var select: () -> Unit = {}
    ) {
        /**
         * The vCPUs of this virtual machine.
         */
        val vcpus: List<VCpu>

        /**
         * The slices that the VM wants to run.
         */
        var queue: Iterator<SimExecutionContext.Slice> = emptyList<SimExecutionContext.Slice>().iterator()

        /**
         * The current active slice.
         */
        var activeSlice: SimExecutionContext.Slice? = null

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

        /**
         * The usage of the virtual machine.
         */
        val usage: MutableStateFlow<Double> = MutableStateFlow(0.0)

        init {
            vcpus = model.cpus.mapIndexed { i, model -> VCpu(this, model, i) }
        }

        /**
         * Schedule the given slices on this vCPU, replacing the existing slices.
         */
        fun schedule(slices: Sequence<SimExecutionContext.Slice>) {
            queue = slices.iterator()

            if (queue.hasNext()) {
                activeSlice = queue.next()
                refresh()
            }
        }

        /**
         * Cancel the existing workload on the VM.
         */
        fun cancel() {
            queue = emptyList<SimExecutionContext.Slice>().iterator()
            activeSlice = null
            refresh()
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

                // Update the vCPU cache
                refresh()

                false
            } else {
                this.activeSlice = null
                select()
                true
            }
        }

        /**
         * Refresh the vCPU cache.
         */
        fun refresh() {
            vcpus.forEach { it.refresh() }
            usage.value = vcpus.sumByDouble { it.burst / it.limit } / vcpus.size
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
        val vm: VmSession,
        val model: ProcessingUnit,
        val id: Int
    ) : Comparable<VCpu> {
        /**
         * The current limit on the vCPU.
         */
        var limit: Double = 0.0

        /**
         * The limit allocated by the hypervisor.
         */
        var allocatedLimit: Double = 0.0

        /**
         * The current burst running on the vCPU.
         */
        var burst: Long = 0L

        /**
         * Consume the specified burst on this vCPU.
         */
        fun consume(burst: Long): Boolean {
            this.burst = max(0, this.burst - burst)

            // Flush the result to the slice if it exists
            vm.activeSlice?.burst?.takeIf { id < it.size }?.set(id, this.burst)

            return allocatedLimit > 0.0 && this.burst == 0L
        }

        /**
         * Refresh the information of this vCPU based on the current slice.
         */
        fun refresh() {
            limit = vm.activeSlice?.limit?.takeIf { id < it.size }?.get(id) ?: 0.0
            burst = vm.activeSlice?.burst?.takeIf { id < it.size }?.get(id) ?: 0
        }

        /**
         * Compare to another vCPU based on the current load of the vCPU.
         */
        override fun compareTo(other: VCpu): Int {
            return limit.compareTo(other.limit)
        }

        /**
         * Create a string representation of the vCPU.
         */
        override fun toString(): String =
            "vCPU(id=$id,burst=$burst,limit=$limit,allocatedLimit=$allocatedLimit)"
    }

    /**
     * The execution context in which a VM runs.
     *
     */
    private inner class VmExecutionContext(override val machine: SimMachineModel) :
        SimExecutionContext,
        DisposableHandle {
        private var finalized: Boolean = false
        private var initialized: Boolean = false
        val session: VmSession = VmSession(machine)

        override val clock: Clock
            get() = this@SimHypervisor.clock

        @OptIn(InternalCoroutinesApi::class)
        override fun onRun(
            batch: Sequence<SimExecutionContext.Slice>,
            triggerMode: SimExecutionContext.TriggerMode,
            merge: (SimExecutionContext.Slice, SimExecutionContext.Slice) -> SimExecutionContext.Slice
        ): SelectClause0 = object : SelectClause0 {
            @InternalCoroutinesApi
            override fun <R> registerSelectClause0(select: SelectInstance<R>, block: suspend () -> R) {
                session.triggerMode = triggerMode
                session.merge = merge
                session.select = {
                    if (select.trySelect()) {
                        block.startCoroutineCancellable(select.completion)
                    }
                }
                session.schedule(batch)
                // Indicate to the hypervisor that the VM should be re-scheduled
                schedulingQueue.offer(SchedulerCommand.Schedule(session))
                select.disposeOnSelect(this@VmExecutionContext)
            }
        }

        override fun dispose() {
            if (!session.isIdle) {
                session.cancel()
                schedulingQueue.offer(SchedulerCommand.Deschedule(session))
            }
        }
    }
}
