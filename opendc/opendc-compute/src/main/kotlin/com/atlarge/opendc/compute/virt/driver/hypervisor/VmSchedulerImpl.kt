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

import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.execution.ProcessorContext
import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A basic implementation of the [VmScheduler] interface.
 *
 * @property hostContext The [ServerContext] of the host.
 * @property hypervisorMonitor The [HypervisorMonitor] to inform with hypervisor scheduling events.
 */
public class VmSchedulerImpl(
    private val hostContext: ServerContext,
    private val hypervisorMonitor: HypervisorMonitor
) : VmScheduler {
    /**
     * The available physical CPUs to schedule.
     */
    private val cpus = hostContext.cpus.map { HostProcessorContext(it, hostContext, hypervisorMonitor) }

    override fun createVirtualCpus(flavor: Flavor): List<ProcessorContext> {
        // TODO At the moment, the first N cores get filled the first. Distribute over all cores instead
        require(flavor.cpuCount <= cpus.size) { "Flavor cannot fit on machine" }

        return cpus
            .asSequence()
            .take(flavor.cpuCount)
            .sortedBy { it.vcpus.size }
            .map { VirtualProcessorContext(it) }
            .toList()
    }

    /**
     * A wrapper around a host [ProcessorContext] that carries additional information about the vCPUs scheduled on the
     * processor.
     */
    internal class HostProcessorContext(
        delegate: ProcessorContext,
        private val hostContext: ServerContext,
        private val hypervisorMonitor: HypervisorMonitor
    ) : ProcessorContext by delegate {
        /**
         * The set of vCPUs scheduled on this processor.
         */
        var vcpus: MutableSet<VirtualProcessorContext> = mutableSetOf()

        /**
         * The deferred run call.
         */
        var call: Job? = null

        /**
         * Schedule the vCPUs on the physical CPU.
         */
        suspend fun reschedule() {
            flush()

            val vcpus = HashSet(vcpus) // Create snapshot of the vCPUs that were scheduled at this moment
            val call = processContext.launch {
                var duration: Long = Long.MAX_VALUE
                var deadline: Long = Long.MAX_VALUE

                for (vcpu in vcpus) {
                    // Limit each vCPU to at most an equal share of the host CPU
                    vcpu.actualUsage = min(vcpu.requestedUsage, info.clockRate / vcpus.size)

                    // The duration that we want to run is that of the shortest request from a vCPU
                    duration = min(duration, ceil(vcpu.requestedBurst / (vcpu.actualUsage * 1_000_000L)).toLong())
                    deadline = min(deadline, vcpu.requestedDeadline)
                }

                var burst: Long = 0
                var usage: Double = 0.0

                for (vcpu in vcpus) {
                    vcpu.actualBurst = (duration * vcpu.actualUsage * 1_000_000L).toLong()
                    burst += vcpu.actualBurst
                    usage += vcpu.actualUsage
                }

                // Ignore time slice if no work to request
                if (burst <= 0L) {
                    return@launch
                }

                // We run the total burst on the host processor. Note that this call may be cancelled at any moment in
                // time, so not all of the burst may be executed.
                val remainder = run(burst, usage, deadline)
                val time = processContext.clock.millis()
                val totalGrantedBurst: Long = burst - remainder

                // Compute for each vCPU the
                for (vcpu in vcpus) {
                    // Compute the fraction of compute time allocated to the VM
                    val fraction = vcpu.actualUsage / usage
                    // Compute the burst time that the VM was actually granted
                    val grantedBurst = max(0, vcpu.actualBurst - ceil(remainder * fraction).toLong())
                    // Compute remaining burst time to be executed for the request
                    vcpu.requestedBurst = max(0, vcpu.requestedBurst - grantedBurst)

                    if (vcpu.requestedBurst == 0L || vcpu.requestedDeadline <= time) {
                        // Return vCPU `run` call: the requested burst was completed or deadline was exceeded
                        vcpu.chan.send(Unit)
                    }
                }

                hypervisorMonitor.onSliceFinish(
                    time,
                    burst,
                    totalGrantedBurst,
                    vcpus.size,
                    hostContext.server
                )
            }

            this.call = call
            call.invokeOnCompletion { this.call = null }
        }

        /**
         * Flush the progress of the current active VMs.
         */
        fun flush() {
            val call = call ?: return // If there is no active call, there is nothing to flush
            // The progress is actually flushed in the coroutine when it notices we cancel it and wait for its
            // completion.
            call.cancel()
        }
    }

    /**
     * An implementation of [ProcessorContext] that delegates the work to a physical CPU.
     */
    internal class VirtualProcessorContext(val host: HostProcessorContext) : ProcessorContext {
        var actualBurst: Long = 0
        var actualUsage: Double = 0.0
        var requestedBurst: Long = 0
        var requestedUsage: Double = 0.0
        var requestedDeadline: Long = 0
        var chan = Channel<Unit>(Channel.RENDEZVOUS)

        override val info: ProcessingUnit
            get() = host.info

        override suspend fun run(burst: Long, maxUsage: Double, deadline: Long): Long {
            requestedBurst = burst
            requestedUsage = maxUsage
            requestedDeadline = deadline

            // Wait until the burst has been run or the coroutine is cancelled
            try {
                host.vcpus.add(this)
                host.reschedule()
                chan.receive()
            } catch (_: CancellationException) {
                // On cancellation, we compute and return the remaining burst
            }

            host.vcpus.remove(this)
            host.reschedule()
            return requestedBurst
        }
    }
}
