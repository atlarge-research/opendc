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

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.common.Labels
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.*
import org.opendc.simulator.compute.*
import org.opendc.simulator.compute.cpufreq.PerformanceScalingGovernor
import org.opendc.simulator.compute.cpufreq.ScalingDriver
import org.opendc.simulator.compute.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.cpufreq.SimpleScalingDriver
import org.opendc.simulator.compute.interference.IMAGE_PERF_INTERFERENCE_MODEL
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.failures.FailureDomain
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
    meter: Meter,
    hypervisor: SimHypervisorProvider,
    scalingGovernor: ScalingGovernor,
    scalingDriver: ScalingDriver,
    private val mapper: SimWorkloadMapper = SimMetaWorkloadMapper(),
) : Host, FailureDomain, AutoCloseable {

    public constructor(
        uid: UUID,
        name: String,
        model: SimMachineModel,
        meta: Map<String, Any>,
        context: CoroutineContext,
        clock: Clock,
        meter: Meter,
        hypervisor: SimHypervisorProvider,
        powerModel: PowerModel = ConstantPowerModel(0.0),
        mapper: SimWorkloadMapper = SimMetaWorkloadMapper(),
    ) : this(uid, name, model, meta, context, clock, meter, hypervisor, PerformanceScalingGovernor(), SimpleScalingDriver(powerModel), mapper)

    /**
     * The [CoroutineScope] of the host bounded by the lifecycle of the host.
     */
    override val scope: CoroutineScope = CoroutineScope(context + Job())

    /**
     * The logger instance of this server.
     */
    private val logger = KotlinLogging.logger {}

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
    public val machine: SimBareMetalMachine = SimBareMetalMachine(context, clock, model, scalingGovernor, scalingDriver)

    /**
     * The hypervisor to run multiple workloads.
     */
    public val hypervisor: SimHypervisor = hypervisor.create(
        scope.coroutineContext, clock,
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

                _batch.put(_cpuWork, requestedWork.toDouble())
                _batch.put(_cpuWorkGranted, grantedWork.toDouble())
                _batch.put(_cpuWorkOvercommit, overcommittedWork.toDouble())
                _batch.put(_cpuWorkInterference, interferedWork.toDouble())
                _batch.put(_cpuUsage, cpuUsage)
                _batch.put(_cpuDemand, cpuDemand)
                _batch.put(_cpuPower, machine.powerDraw)
                _batch.record()
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

    /**
     * The number of guests on the host.
     */
    private val _guests = meter.longUpDownCounterBuilder("guests.total")
        .setDescription("Number of guests")
        .setUnit("1")
        .build()
        .bind(Labels.of("host", uid.toString()))

    /**
     * The number of active guests on the host.
     */
    private val _activeGuests = meter.longUpDownCounterBuilder("guests.active")
        .setDescription("Number of active guests")
        .setUnit("1")
        .build()
        .bind(Labels.of("host", uid.toString()))

    /**
     * The CPU usage on the host.
     */
    private val _cpuUsage = meter.doubleValueRecorderBuilder("cpu.usage")
        .setDescription("The amount of CPU resources used by the host")
        .setUnit("MHz")
        .build()

    /**
     * The CPU demand on the host.
     */
    private val _cpuDemand = meter.doubleValueRecorderBuilder("cpu.demand")
        .setDescription("The amount of CPU resources the guests would use if there were no CPU contention or CPU limits")
        .setUnit("MHz")
        .build()

    /**
     * The requested work for the CPU.
     */
    private val _cpuPower = meter.doubleValueRecorderBuilder("power.usage")
        .setDescription("The amount of power used by the CPU")
        .setUnit("W")
        .build()

    /**
     * The requested work for the CPU.
     */
    private val _cpuWork = meter.doubleValueRecorderBuilder("cpu.work.total")
        .setDescription("The amount of work supplied to the CPU")
        .setUnit("1")
        .build()

    /**
     * The work actually performed by the CPU.
     */
    private val _cpuWorkGranted = meter.doubleValueRecorderBuilder("cpu.work.granted")
        .setDescription("The amount of work performed by the CPU")
        .setUnit("1")
        .build()

    /**
     * The work that could not be performed by the CPU due to overcommitting resource.
     */
    private val _cpuWorkOvercommit = meter.doubleValueRecorderBuilder("cpu.work.overcommit")
        .setDescription("The amount of work not performed by the CPU due to overcommitment")
        .setUnit("1")
        .build()

    /**
     * The work that could not be performed by the CPU due to interference.
     */
    private val _cpuWorkInterference = meter.doubleValueRecorderBuilder("cpu.work.interference")
        .setDescription("The amount of work not performed by the CPU due to interference")
        .setUnit("1")
        .build()

    /**
     * The batch recorder used to record multiple metrics atomically.
     */
    private val _batch = meter.newBatchRecorder("host", uid.toString())

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
        _guests.add(1)

        if (start) {
            guest.start()
        }
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
        _guests.add(-1)
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

        _activeGuests.add(1)
        listeners.forEach { it.onStateChanged(this, vm.server, vm.state) }
    }

    private fun onGuestStop(vm: Guest) {
        guests.forEach { (_, guest) ->
            if (guest.state == ServerState.RUNNING) {
                vm.performanceInterferenceModel?.onStop(vm.server.image.name)
            }
        }

        _activeGuests.add(-1)
        listeners.forEach { it.onStateChanged(this, vm.server, vm.state) }
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

            job = scope.launch {
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
                    job = null
                }
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
