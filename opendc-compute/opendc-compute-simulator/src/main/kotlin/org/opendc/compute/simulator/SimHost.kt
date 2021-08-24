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

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.*
import org.opendc.simulator.compute.*
import org.opendc.simulator.compute.kernel.SimHypervisor
import org.opendc.simulator.compute.kernel.SimHypervisorProvider
import org.opendc.simulator.compute.kernel.cpufreq.PerformanceScalingGovernor
import org.opendc.simulator.compute.kernel.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.kernel.interference.VmInterferenceDomain
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.PowerDriver
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.failures.FailureDomain
import org.opendc.simulator.resources.SimResourceInterpreter
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A [Host] that is simulates virtual machines on a physical machine using [SimHypervisor].
 */
public class SimHost(
    override val uid: UUID,
    override val name: String,
    model: MachineModel,
    override val meta: Map<String, Any>,
    context: CoroutineContext,
    interpreter: SimResourceInterpreter,
    meter: Meter,
    hypervisor: SimHypervisorProvider,
    scalingGovernor: ScalingGovernor = PerformanceScalingGovernor(),
    powerDriver: PowerDriver = SimplePowerDriver(ConstantPowerModel(0.0)),
    private val mapper: SimWorkloadMapper = SimMetaWorkloadMapper(),
    interferenceDomain: VmInterferenceDomain? = null
) : Host, FailureDomain, AutoCloseable {
    /**
     * The [CoroutineScope] of the host bounded by the lifecycle of the host.
     */
    override val scope: CoroutineScope = CoroutineScope(context + Job())

    /**
     * The clock instance used by the host.
     */
    private val clock = interpreter.clock

    /**
     * The logger instance of this server.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The event listeners registered with this host.
     */
    private val listeners = mutableListOf<HostListener>()

    /**
     * The machine to run on.
     */
    public val machine: SimBareMetalMachine = SimBareMetalMachine(interpreter, model, powerDriver)

    /**
     * The hypervisor to run multiple workloads.
     */
    public val hypervisor: SimHypervisor = hypervisor.create(
        interpreter,
        scalingGovernor = scalingGovernor,
        interferenceDomain = interferenceDomain,
        listener = object : SimHypervisor.Listener {
            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                requestedWork: Double,
                grantedWork: Double,
                overcommittedWork: Double,
                interferedWork: Double,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                _totalWork.add(requestedWork)
                _grantedWork.add(grantedWork)
                _overcommittedWork.add(overcommittedWork)
                _interferedWork.add(interferedWork)
                _cpuDemand.record(cpuDemand)
                _cpuUsage.record(cpuUsage)
                _powerUsage.record(machine.powerDraw)

                reportTime()
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

    override val model: HostModel = HostModel(model.cpus.size, model.memory.sumOf { it.size })

    /**
     * The total number of guests.
     */
    private val _guests = meter.upDownCounterBuilder("guests.total")
        .setDescription("Number of guests")
        .setUnit("1")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The number of active guests on the host.
     */
    private val _activeGuests = meter.upDownCounterBuilder("guests.active")
        .setDescription("Number of active guests")
        .setUnit("1")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The CPU demand of the host.
     */
    private val _cpuDemand = meter.histogramBuilder("cpu.demand")
        .setDescription("The amount of CPU resources the guests would use if there were no CPU contention or CPU limits")
        .setUnit("MHz")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The CPU usage of the host.
     */
    private val _cpuUsage = meter.histogramBuilder("cpu.usage")
        .setDescription("The amount of CPU resources used by the host")
        .setUnit("MHz")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The power usage of the host.
     */
    private val _powerUsage = meter.histogramBuilder("power.usage")
        .setDescription("The amount of power used by the CPU")
        .setUnit("W")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The total amount of work supplied to the CPU.
     */
    private val _totalWork = meter.counterBuilder("cpu.work.total")
        .setDescription("The amount of work supplied to the CPU")
        .setUnit("1")
        .ofDoubles()
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The work performed by the CPU.
     */
    private val _grantedWork = meter.counterBuilder("cpu.work.granted")
        .setDescription("The amount of work performed by the CPU")
        .setUnit("1")
        .ofDoubles()
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The amount not performed by the CPU due to overcommitment.
     */
    private val _overcommittedWork = meter.counterBuilder("cpu.work.overcommit")
        .setDescription("The amount of work not performed by the CPU due to overcommitment")
        .setUnit("1")
        .ofDoubles()
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The amount of work not performed by the CPU due to interference.
     */
    private val _interferedWork = meter.counterBuilder("cpu.work.interference")
        .setDescription("The amount of work not performed by the CPU due to interference")
        .setUnit("1")
        .ofDoubles()
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The amount of time in the system.
     */
    private val _totalTime = meter.counterBuilder("host.time.total")
        .setDescription("The amount of time in the system")
        .setUnit("ms")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The uptime of the host.
     */
    private val _upTime = meter.counterBuilder("host.time.up")
        .setDescription("The uptime of the host")
        .setUnit("ms")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

    /**
     * The downtime of the host.
     */
    private val _downTime = meter.counterBuilder("host.time.down")
        .setDescription("The downtime of the host")
        .setUnit("ms")
        .build()
        .bind(Attributes.of(ResourceAttributes.HOST_ID, uid.toString()))

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

    private var _lastReport = clock.millis()

    private fun reportTime() {
        if (!scope.isActive)
            return

        val now = clock.millis()
        val duration = now - _lastReport

        _totalTime.add(duration)
        when (_state) {
            HostState.UP -> _upTime.add(duration)
            HostState.DOWN -> _downTime.add(duration)
        }

        _lastReport = now
    }

    override fun canFit(server: Server): Boolean {
        val sufficientMemory = machine.model.memory.size >= server.flavor.memorySize
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
        val guest = Guest(server, hypervisor.createMachine(server.flavor.toMachineModel(), server.name))
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
        _guests.add(-1)
        guest.terminate()
    }

    override fun addListener(listener: HostListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: HostListener) {
        listeners.remove(listener)
    }

    override fun close() {
        reportTime()
        scope.cancel()
        machine.close()
    }

    override fun toString(): String = "SimHost[uid=$uid,name=$name,model=$model]"

    /**
     * Convert flavor to machine model.
     */
    private fun Flavor.toMachineModel(): MachineModel {
        val originalCpu = machine.model.cpus[0]
        val processingNode = originalCpu.node.copy(coreCount = cpuCount)
        val processingUnits = (0 until cpuCount).map { originalCpu.copy(id = it, node = processingNode) }
        val memoryUnits = listOf(MemoryUnit("Generic", "Generic", 3200.0, memorySize))

        return MachineModel(processingUnits, memoryUnits)
    }

    private fun onGuestStart(vm: Guest) {
        _activeGuests.add(1)
        listeners.forEach { it.onStateChanged(this, vm.server, vm.state) }
    }

    private fun onGuestStop(vm: Guest) {
        _activeGuests.add(-1)
        listeners.forEach { it.onStateChanged(this, vm.server, vm.state) }
    }

    override suspend fun fail() {
        reportTime()
        _state = HostState.DOWN
        for (guest in guests.values) {
            guest.fail()
        }
    }

    override suspend fun recover() {
        reportTime()
        _state = HostState.UP
        for (guest in guests.values) {
            guest.start()
        }
    }

    /**
     * A virtual machine instance that the driver manages.
     */
    private inner class Guest(val server: Server, val machine: SimMachine) {
        var state: ServerState = ServerState.TERMINATED

        suspend fun start() {
            when (state) {
                ServerState.TERMINATED, ServerState.ERROR -> {
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
            machine.close()
            state = ServerState.DELETED
        }

        suspend fun fail() {
            stop()
            state = ServerState.ERROR
        }

        private var job: Job? = null

        private suspend fun launch() = suspendCancellableCoroutine<Unit> { cont ->
            assert(job == null) { "Concurrent job running" }
            val workload = mapper.createWorkload(server)

            job = scope.launch {
                try {
                    delay(1) // TODO Introduce boot time
                    init()
                    cont.resume(Unit)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                }
                try {
                    machine.run(workload, mapOf("driver" to this@SimHost, "server" to server))
                    exit(null)
                } catch (cause: Throwable) {
                    exit(cause)
                } finally {
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

            onGuestStop(this)
        }
    }
}
