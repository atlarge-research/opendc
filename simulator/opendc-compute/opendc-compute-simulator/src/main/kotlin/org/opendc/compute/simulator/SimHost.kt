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
import org.opendc.compute.service.driver.*
import org.opendc.simulator.compute.*
import org.opendc.simulator.compute.interference.IMAGE_PERF_INTERFERENCE_MODEL
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.MachinePowerModel
import org.opendc.simulator.failures.FailureDomain
import org.opendc.utils.flow.EventFlow
import java.time.Clock
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * A [Host] that is simulates virtual machines on a physical machine using [SimHypervisor].
 */
public class SimHost(
    override val uid: UUID,
    override val name: String,
    model: SimMachineModel,
    override val meta: Map<String, Any>,
    context: CoroutineContext,
    clock: Clock,
    hypervisor: SimHypervisorProvider,
    powerModel: MachinePowerModel = ConstantPowerModel(0.0),
    private val mapper: SimWorkloadMapper = SimMetaWorkloadMapper(),
) : Host, FailureDomain, AutoCloseable {
    /**
     * The [CoroutineScope] of the host bounded by the lifecycle of the host.
     */
    override val scope: CoroutineScope = CoroutineScope(context + Job())

    /**
     * The logger instance of this server.
     */
    private val logger = KotlinLogging.logger {}

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
    private var availableMemory: Long = model.memory.map { it.size }.sum()

    /**
     * The machine to run on.
     */
    public val machine: SimBareMetalMachine = SimBareMetalMachine(context, clock, model, powerModel)

    /**
     * The hypervisor to run multiple workloads.
     */
    public val hypervisor: SimHypervisor = hypervisor.create(
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
                        guests.size
                    )
                )
            }
        }
    )

    /**
     * The virtual machines running on the hypervisor.
     */
    private val guests = HashMap<Server, Guest>()

    override val state: HostState
        get() = _state
    private var _state: HostState = HostState.DOWN
        set(value) {
            if (value != field) {
                listeners.forEach { it.onStateChanged(this, value) }
            }
            field = value
        }

    override val model: HostModel = HostModel(model.cpus.size, model.memory.map { it.size }.sum())

    init {
        // Launch hypervisor onto machine
        scope.launch {
            try {
                _state = HostState.UP
                machine.run(this@SimHost.hypervisor, emptyMap())
            } catch (_: CancellationException) {
                // Ignored
            } catch (cause: Throwable) {
                logger.error(cause) { "Host failed" }
                throw cause
            } finally {
                _state = HostState.DOWN
            }
        }
    }

    override fun canFit(server: Server): Boolean {
        val sufficientMemory = availableMemory > server.flavor.memorySize
        val enoughCpus = machine.model.cpus.size >= server.flavor.cpuCount
        val canFit = hypervisor.canFit(server.flavor.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    override suspend fun spawn(server: Server, start: Boolean) {
        // Return if the server already exists on this host
        if (server in this) {
            return
        }

        require(canFit(server)) { "Server does not fit" }
        val guest = Guest(server, hypervisor.createMachine(server.flavor.toMachineModel()))
        guests[server] = guest

        if (start) {
            guest.start()
        }

        _events.emit(HostEvent.VmsUpdated(this, guests.count { it.value.state == ServerState.RUNNING }, availableMemory))
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

    override suspend fun delete(server: Server) {
        val guest = guests.remove(server) ?: return
        guest.terminate()
    }

    override fun addListener(listener: HostListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: HostListener) {
        listeners.remove(listener)
    }

    override fun close() {
        scope.cancel()
        machine.close()
        _state = HostState.DOWN
    }

    override fun toString(): String = "SimHost[uid=$uid,name=$name,model=$model]"

    /**
     * Convert flavor to machine model.
     */
    private fun Flavor.toMachineModel(): SimMachineModel {
        val originalCpu = machine.model.cpus[0]
        val processingNode = originalCpu.node.copy(coreCount = cpuCount)
        val processingUnits = (0 until cpuCount).map { originalCpu.copy(id = it, node = processingNode) }
        val memoryUnits = listOf(MemoryUnit("Generic", "Generic", 3200.0, memorySize))

        return SimMachineModel(processingUnits, memoryUnits)
    }

    private fun onGuestStart(vm: Guest) {
        guests.forEach { (_, guest) ->
            if (guest.state == ServerState.RUNNING) {
                vm.performanceInterferenceModel?.onStart(vm.server.image.name)
            }
        }

        listeners.forEach { it.onStateChanged(this, vm.server, vm.state) }
    }

    private fun onGuestStop(vm: Guest) {
        guests.forEach { (_, guest) ->
            if (guest.state == ServerState.RUNNING) {
                vm.performanceInterferenceModel?.onStop(vm.server.image.name)
            }
        }

        listeners.forEach { it.onStateChanged(this, vm.server, vm.state) }

        _events.emit(HostEvent.VmsUpdated(this@SimHost, guests.count { it.value.state == ServerState.RUNNING }, availableMemory))
    }

    override suspend fun fail() {
        _state = HostState.DOWN
    }

    override suspend fun recover() {
        _state = HostState.UP
    }

    /**
     * A virtual machine instance that the driver manages.
     */
    private inner class Guest(val server: Server, val machine: SimMachine) {
        val performanceInterferenceModel: PerformanceInterferenceModel? = server.meta[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?

        var state: ServerState = ServerState.TERMINATED

        suspend fun start() {
            when (state) {
                ServerState.TERMINATED -> {
                    logger.info { "User requested to start server ${server.uid}" }
                    launch()
                }
                ServerState.RUNNING -> return
                ServerState.DELETED -> {
                    logger.warn { "User tried to start terminated server" }
                    throw IllegalArgumentException("Server is terminated")
                }
                else -> assert(false) { "Invalid state transition" }
            }
        }

        suspend fun stop() {
            when (state) {
                ServerState.RUNNING, ServerState.ERROR -> {
                    val job = job ?: throw IllegalStateException("Server should be active")
                    job.cancel()
                    job.join()
                }
                ServerState.TERMINATED, ServerState.DELETED -> return
                else -> assert(false) { "Invalid state transition" }
            }
        }

        suspend fun terminate() {
            stop()
            state = ServerState.DELETED
        }

        private var job: Job? = null

        private suspend fun launch() = suspendCancellableCoroutine<Unit> { cont ->
            assert(job == null) { "Concurrent job running" }
            val workload = mapper.createWorkload(server)

            val job = scope.launch {
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
            state = ServerState.RUNNING
            onGuestStart(this)
        }

        private fun exit(cause: Throwable?) {
            state =
                if (cause == null)
                    ServerState.TERMINATED
                else
                    ServerState.ERROR

            availableMemory += server.flavor.memorySize
            onGuestStop(this)
        }
    }
}
