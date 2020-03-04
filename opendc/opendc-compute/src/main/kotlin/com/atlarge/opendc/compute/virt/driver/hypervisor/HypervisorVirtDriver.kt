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

package com.atlarge.opendc.compute.virt.driver.hypervisor

import com.atlarge.odcsim.SimulationContext
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriverMonitor
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import com.atlarge.opendc.core.workload.PerformanceInterferenceModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A [VirtDriver] that is backed by a simple hypervisor implementation.
 */
class HypervisorVirtDriver(
    private val hostContext: ServerContext,
    private val monitor: HypervisorMonitor
) : VirtDriver {
    /**
     * A set for tracking the VM context objects.
     */
    internal val vms: MutableSet<VmServerContext> = mutableSetOf()

    /**
     * Current total memory use of the images on this hypervisor.
     */
    private var availableMemory: Long = hostContext.server.flavor.memorySize

    /**
     * Monitors to keep informed.
     */
    private val monitors: MutableSet<VirtDriverMonitor> = mutableSetOf()

    override suspend fun spawn(image: Image, monitor: ServerMonitor, flavor: Flavor): Server {
        val requiredMemory = flavor.memorySize
        if (availableMemory - requiredMemory < 0) {
            throw InsufficientMemoryOnServerException()
        }
        require(flavor.cpuCount <= hostContext.server.flavor.cpuCount) { "Machine does not fit" }

        val server = Server(UUID.randomUUID(), "<unnamed>", emptyMap(), flavor, image, ServerState.BUILD)
        availableMemory -= requiredMemory
        vms.add(VmServerContext(server, monitor, simulationContext))
        monitors.forEach { it.onUpdate(vms.size, availableMemory) }
        return server
    }

    override suspend fun addMonitor(monitor: VirtDriverMonitor) {
        monitors.add(monitor)
    }

    override suspend fun removeMonitor(monitor: VirtDriverMonitor) {
        monitors.remove(monitor)
    }

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

        // Do not schedule a call if there is no work to schedule
        if (activeVms.isEmpty()) {
            return
        }

        val call = simulationContext.domain.launch {
            val start = simulationContext.clock.millis()
            val vms = activeVms.toSet()

            var duration: Long = Long.MAX_VALUE
            var deadline: Long = Long.MAX_VALUE
            val usage = DoubleArray(hostContext.cpus.size)

            for (vm in vms) {
                for (i in 0 until min(vm.cpus.size, vm.requestedBurst.size)) {
                    val cpu = vm.cpus[i]

                    // Limit each vCPU to at most an equal share of the host CPU
                    val actualUsage = min(vm.limit[i], cpu.frequency / vms.size)

                    // The duration that we want to run is that of the shortest request from a vCPU
                    duration = min(duration, ceil(vm.requestedBurst[i] / (actualUsage * 1_000_000L)).toLong())
                    deadline = min(deadline, vm.deadline)
                    usage[i] += actualUsage
                }
            }

            val burst = LongArray(hostContext.cpus.size)

            val imagesRunning = vms.map { it.server.image }.toSet()

            for (vm in vms) {
                var performanceScore = 1.0

                // Apply performance interference model
                if (vm.server.image.tags.containsKey("performance-interference")) {
                    performanceScore = (vm.server.image.tags["performance-interference"]
                        as PerformanceInterferenceModel).apply(imagesRunning)
                }

                for (i in 0 until min(vm.cpus.size, vm.requestedBurst.size)) {
                    val cpu = vm.cpus[i]

                    // Limit each vCPU to at most an equal share of the host CPU
                    val actualUsage = min(vm.limit[i], cpu.frequency / vms.size)
                    val actualBurst = (duration * actualUsage * 1_000_000L).toLong()

                    burst[i] += (performanceScore * actualBurst).toLong()
                }
            }

            val granted = burst.clone()
            // We run the total burst on the host processor. Note that this call may be cancelled at any moment in
            // time, so not all of the burst may be executed.
            hostContext.run(granted, usage, deadline)
            val end = simulationContext.clock.millis()

            // No work was performed
            if ((end - start) <= 0) {
                return@launch
            }

            for (vm in vms) {
                for (i in 0 until min(vm.cpus.size, vm.requestedBurst.size)) {
                    val cpu = vm.cpus[i]

                    // Limit each vCPU to at most an equal share of the host CPU
                    val actualUsage = min(vm.limit[i], cpu.frequency / vms.size)
                    val actualBurst = (duration * actualUsage * 1_000_000L).toLong()

                    // Compute the fraction of compute time allocated to the VM
                    val fraction = actualUsage / usage[i]

                    // Compute the burst time that the VM was actually granted
                    val grantedBurst = max(0, actualBurst - ceil(burst[i] * fraction).toLong())

                    // Compute remaining burst time to be executed for the request
                    vm.requestedBurst[i] = max(0, vm.requestedBurst[i] - grantedBurst)
                }

                if (vm.requestedBurst.any { it == 0L } || vm.deadline <= end) {
                    // Return vCPU `run` call: the requested burst was completed or deadline was exceeded
                    vm.chan.send(Unit)
                }
            }

            for (i in burst.indices) {
                monitor.onSliceFinish(
                    end,
                    burst[i],
                    granted[i],
                    vms.size,
                    hostContext.server
                )
            }
        }
        this.call = call
        call.invokeOnCompletion { this.call = null }
    }

    /**
     * Flush the progress of the current active VMs.
     */
    private fun flush() {
        val call = call ?: return // If there is no active call, there is nothing to flush
        // The progress is actually flushed in the coroutine when it notices we cancel it and wait for its
        // completion.
        call.cancel()
    }

    internal inner class VmServerContext(
        override var server: Server,
        val monitor: ServerMonitor,
        ctx: SimulationContext
    ) : ServerManagementContext {
        lateinit var requestedBurst: LongArray
        lateinit var limit: DoubleArray
        var deadline: Long = 0L
        var chan = Channel<Unit>(Channel.RENDEZVOUS)
        private var initialized: Boolean = false

        internal val job: Job = ctx.domain.launch {
            init()
            try {
                server.image(this@VmServerContext)
                exit()
            } catch (cause: Throwable) {
                exit(cause)
            }
        }

        override val cpus: List<ProcessingUnit> = hostContext.cpus.take(server.flavor.cpuCount)

        override suspend fun init() {
            if (initialized) {
                throw IllegalStateException()
            }

            val previousState = server.state
            server = server.copy(state = ServerState.ACTIVE)
            monitor.onUpdate(server, previousState)
            initialized = true
        }

        override suspend fun exit(cause: Throwable?) {
            val previousState = server.state
            val state = if (cause == null) ServerState.SHUTOFF else ServerState.ERROR
            server = server.copy(state = state)
            availableMemory += server.flavor.memorySize
            monitor.onUpdate(server, previousState)
            initialized = false
            vms.remove(this)
            monitors.forEach { it.onUpdate(vms.size, availableMemory) }
        }

        override suspend fun run(burst: LongArray, limit: DoubleArray, deadline: Long) {
            require(burst.size == limit.size) { "Array dimensions do not match" }

            requestedBurst = burst
            this.limit = limit
            this.deadline = deadline

            // Wait until the burst has been run or the coroutine is cancelled
            try {
                activeVms += this
                reschedule()
                chan.receive()
            } catch (_: CancellationException) {
                // On cancellation, we compute and return the remaining burst
            }
            activeVms -= this
            reschedule()
        }
    }
}
