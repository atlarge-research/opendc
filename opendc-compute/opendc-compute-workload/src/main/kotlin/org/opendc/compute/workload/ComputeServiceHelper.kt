/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.compute.workload

import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.workload.topology.HostSpec
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.kernel.SimHypervisor
import org.opendc.simulator.flow.FlowEngine
import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Helper class to simulate VM-based workloads in OpenDC.
 *
 * @param context [CoroutineContext] to run the simulation in.
 * @param clock [Clock] instance tracking simulation time.
 * @param scheduler [ComputeScheduler] implementation to use for the service.
 * @param schedulingQuantum The scheduling quantum of the scheduler.
 */
public class ComputeServiceHelper(
    private val context: CoroutineContext,
    private val clock: Clock,
    scheduler: ComputeScheduler,
    seed: Long,
    schedulingQuantum: Duration = Duration.ofMinutes(5)
) : AutoCloseable {
    /**
     * The [ComputeService] that has been configured by the manager.
     */
    public val service: ComputeService = ComputeService(context, clock, scheduler, schedulingQuantum)

    /**
     * The [FlowEngine] to simulate the hosts.
     */
    private val engine = FlowEngine(context, clock)

    /**
     * The hosts that belong to this class.
     */
    private val hosts = mutableSetOf<SimHost>()

    /**
     * The source of randomness.
     */
    private val random = SplittableRandom(seed)

    /**
     * Run a simulation of the [ComputeService] by replaying the workload trace given by [trace].
     *
     * @param trace The trace to simulate.
     * @param submitImmediately A flag to indicate that the servers are scheduled immediately (so not at their start time).
     * @param failureModel A failure model to use for injecting failures.
     * @param interference A flag to indicate that VM interference needs to be enabled.
     */
    public suspend fun run(
        trace: List<VirtualMachine>,
        submitImmediately: Boolean = false,
        failureModel: FailureModel? = null,
        interference: Boolean = false,
    ) {
        service.replay(clock, trace, random.nextLong(), submitImmediately, failureModel, interference)
    }

    /**
     * Register a host for this simulation.
     *
     * @param spec The definition of the host.
     * @param optimize Merge the CPU resources of the host into a single CPU resource.
     * @return The [SimHost] that has been constructed by the runner.
     */
    public fun registerHost(spec: HostSpec, optimize: Boolean = false): SimHost {
        val machine = SimBareMetalMachine(engine, spec.model, spec.powerDriver)
        val hypervisor = SimHypervisor(engine, spec.multiplexerFactory, random)

        val host = SimHost(
            spec.uid,
            spec.name,
            spec.meta,
            context,
            clock,
            machine,
            hypervisor,
            optimize = optimize
        )

        require(hosts.add(host)) { "Host with uid ${spec.uid} already exists" }
        service.addHost(host)

        return host
    }

    override fun close() {
        service.close()

        for (host in hosts) {
            host.close()
        }

        hosts.clear()
    }
}
