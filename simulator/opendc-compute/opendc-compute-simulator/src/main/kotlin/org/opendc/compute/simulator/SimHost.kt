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
import mu.KotlinLogging
import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.core.*
import org.opendc.compute.core.metal.Node
import org.opendc.compute.core.virt.*
import org.opendc.simulator.compute.*
import org.opendc.simulator.compute.interference.IMAGE_PERF_INTERFERENCE_MODEL
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.workload.SimResourceCommand
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.utils.flow.EventFlow
import java.util.*
import kotlin.coroutines.resume

/**
 * A [Host] that is simulates virtual machines on a physical machine using [SimHypervisor].
 */
public class SimHost(
    override val uid: UUID,
    private val coroutineScope: CoroutineScope,
    hypervisor: SimHypervisorProvider
) : Host, SimWorkload {
    /**
     * The logger instance of this server.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The execution context in which the [Host] runs.
     */
    private lateinit var ctx: SimExecutionContext

    override val events: Flow<HostEvent>
        get() = _events
    internal val _events = EventFlow<HostEvent>()

    /**
     * The event listeners registered with this host.
     */
    private val listeners = mutableListOf<HostListener>()

    /**
     * Current total memory use of the images on this hypervisor.
     */
    private var availableMemory: Long = 0

    /**
     * The hypervisor to run multiple workloads.
     */
    private val hypervisor = hypervisor.create(
        object : SimHypervisor.Listener {
            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                requestedWork: Long,
                grantedWork: Long,
                overcommittedWork: Long,
                interferedWork: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                _events.emit(
                    HostEvent.SliceFinished(
                        this@SimHost,
                        requestedWork,
                        grantedWork,
                        overcommittedWork,
                        interferedWork,
                        cpuUsage,
                        cpuDemand,
                        guests.size,
                        node
                    )
                )
            }
        }
    )

    /**
     * The virtual machines running on the hypervisor.
     */
    private val guests = HashMap<Server, SimGuest>()

    /**
     * The node on which the hypervisor runs.
     */
    public val node: Node
        get() = ctx.meta["node"] as Node

    override val state: HostState
        get() = _state
    private var _state: HostState = HostState.UP
        set(value) {
            listeners.forEach { it.onStateChange(this, value) }
            field = value
        }

    override fun canFit(server: Server): Boolean {
        val sufficientMemory = availableMemory > server.flavor.memorySize
        val enoughCpus = ctx.machine.cpus.size >= server.flavor.cpuCount
        val canFit = hypervisor.canFit(server.flavor.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    override suspend fun spawn(server: Server, start: Boolean) {
        // Return if the server already exists on this host
        if (server in this) {
            return
        }

        require(canFit(server)) { "Server does not fit" }
        val guest = SimGuest(server, hypervisor.createMachine(server.flavor.toMachineModel()))
        guests[server] = guest

        if (start) {
            guest.start()
        }

        _events.emit(HostEvent.VmsUpdated(this, guests.count { it.value.state == ServerState.ACTIVE }, availableMemory))
    }

    override fun contains(server: Server): Boolean {
        return server in guests
    }

    override suspend fun start(server: Server) {
        val guest = requireNotNull(guests[server]) { "Unknown server ${server.uid} at host $uid" }
        guest.start()
    }

    override suspend fun stop(server: Server) {
        val guest = requireNotNull(guests[server]) { "Unknown server ${server.uid} at host $uid" }
        guest.stop()
    }

    override suspend fun terminate(server: Server) {
        val guest = guests.remove(server) ?: return
        guest.terminate()
    }

    override fun addListener(listener: HostListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: HostListener) {
        listeners.remove(listener)
    }

    /**
     * Convert flavor to machine model.
     */
    private fun Flavor.toMachineModel(): SimMachineModel {
        val originalCpu = ctx.machine.cpus[0]
        val processingNode = originalCpu.node.copy(coreCount = cpuCount)
        val processingUnits = (0 until cpuCount).map { originalCpu.copy(id = it, node = processingNode) }
        val memoryUnits = listOf(MemoryUnit("Generic", "Generic", 3200.0, memorySize))

        return SimMachineModel(processingUnits, memoryUnits)
    }

    private fun onGuestStart(vm: SimGuest) {
        guests.forEach { _, guest ->
            if (guest.state == ServerState.ACTIVE) {
                vm.performanceInterferenceModel?.onStart(vm.server.image.name)
            }
        }

        listeners.forEach { it.onStateChange(this, vm.server, vm.state) }
    }

    private fun onGuestStop(vm: SimGuest) {
        guests.forEach { _, guest ->
            if (guest.state == ServerState.ACTIVE) {
                vm.performanceInterferenceModel?.onStop(vm.server.image.name)
            }
        }

        listeners.forEach { it.onStateChange(this, vm.server, vm.state) }

        _events.emit(HostEvent.VmsUpdated(this@SimHost, guests.count { it.value.state == ServerState.ACTIVE }, availableMemory))
    }

    /**
     * A virtual machine instance that the driver manages.
     */
    private inner class SimGuest(val server: Server, val machine: SimMachine) {
        val performanceInterferenceModel: PerformanceInterferenceModel? = server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?

        var state: ServerState = ServerState.SHUTOFF

        suspend fun start() {
            when (state) {
                ServerState.SHUTOFF -> {
                    logger.info { "User requested to start server ${server.uid}" }
                    launch()
                }
                ServerState.ACTIVE -> return
                else -> assert(false) { "Invalid state transition" }
            }
        }

        suspend fun stop() {
            when (state) {
                ServerState.ACTIVE, ServerState.ERROR -> {
                    val job = job ?: throw IllegalStateException("Server should be active")
                    job.cancel()
                    job.join()
                }
                ServerState.SHUTOFF -> return
                else -> assert(false) { "Invalid state transition" }
            }
        }

        suspend fun terminate() {
            stop()
        }

        private var job: Job? = null

        private suspend fun launch() = suspendCancellableCoroutine<Unit> { cont ->
            assert(job == null) { "Concurrent job running" }
            val workload = server.image.tags["workload"] as SimWorkload

            val job = coroutineScope.launch {
                delay(1) // TODO Introduce boot time
                init()
                cont.resume(Unit)
                try {
                    machine.run(workload, mapOf("driver" to this@SimHost, "server" to server))
                    exit(null)
                } catch (cause: Throwable) {
                    exit(cause)
                } finally {
                    machine.close()
                }
            }
            this.job = job
            job.invokeOnCompletion {
                this.job = null
            }
        }

        private fun init() {
            state = ServerState.ACTIVE
            onGuestStart(this)
        }

        private fun exit(cause: Throwable?) {
            state =
                if (cause == null)
                    ServerState.SHUTOFF
                else
                    ServerState.ERROR

            availableMemory += server.flavor.memorySize
            onGuestStop(this)
        }
    }

    override fun onStart(ctx: SimExecutionContext) {
        this.ctx = ctx
        this.availableMemory = ctx.machine.memory.map { it.size }.sum()
        this.hypervisor.onStart(ctx)
    }

    override fun onStart(ctx: SimExecutionContext, cpu: Int): SimResourceCommand {
        return hypervisor.onStart(ctx, cpu)
    }

    override fun onNext(ctx: SimExecutionContext, cpu: Int, remainingWork: Double): SimResourceCommand {
        return hypervisor.onNext(ctx, cpu, remainingWork)
    }
}
