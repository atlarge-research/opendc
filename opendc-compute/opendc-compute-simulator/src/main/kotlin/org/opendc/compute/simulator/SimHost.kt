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

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.*
import org.opendc.compute.simulator.internal.Guest
import org.opendc.compute.simulator.internal.GuestListener
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
import org.opendc.simulator.flow.FlowEngine
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong

/**
 * A [Host] that is simulates virtual machines on a physical machine using [SimHypervisor].
 */
public class SimHost(
    override val uid: UUID,
    override val name: String,
    model: MachineModel,
    override val meta: Map<String, Any>,
    context: CoroutineContext,
    engine: FlowEngine,
    meterProvider: MeterProvider,
    hypervisor: SimHypervisorProvider,
    scalingGovernor: ScalingGovernor = PerformanceScalingGovernor(),
    powerDriver: PowerDriver = SimplePowerDriver(ConstantPowerModel(0.0)),
    private val mapper: SimWorkloadMapper = SimMetaWorkloadMapper(),
    interferenceDomain: VmInterferenceDomain? = null,
    private val optimize: Boolean = false
) : Host, AutoCloseable {
    /**
     * The [CoroutineScope] of the host bounded by the lifecycle of the host.
     */
    private val scope: CoroutineScope = CoroutineScope(context + Job())

    /**
     * The clock instance used by the host.
     */
    private val clock = engine.clock

    /**
     * The logger instance of this server.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The [Meter] to track metrics of the simulated host.
     */
    private val meter = meterProvider.get("org.opendc.compute.simulator")

    /**
     * The event listeners registered with this host.
     */
    private val listeners = mutableListOf<HostListener>()

    /**
     * The machine to run on.
     */
    public val machine: SimBareMetalMachine = SimBareMetalMachine(engine, model.optimize(), powerDriver)

    /**
     * The hypervisor to run multiple workloads.
     */
    private val hypervisor: SimHypervisor = hypervisor.create(
        engine,
        scalingGovernor = scalingGovernor,
        interferenceDomain = interferenceDomain,
        listener = object : SimHypervisor.Listener {
            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                totalWork: Double,
                grantedWork: Double,
                overcommittedWork: Double,
                interferedWork: Double,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                _cpuDemand = cpuDemand
                _cpuUsage = cpuUsage

                collectTime()
            }
        }
    )
    private var _cpuUsage = 0.0
    private var _cpuDemand = 0.0

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
     * The [GuestListener] that listens for guest events.
     */
    private val guestListener = object : GuestListener {
        override fun onStart(guest: Guest) {
            listeners.forEach { it.onStateChanged(this@SimHost, guest.server, guest.state) }
        }

        override fun onStop(guest: Guest) {
            listeners.forEach { it.onStateChanged(this@SimHost, guest.server, guest.state) }
        }
    }

    init {
        // Launch hypervisor onto machine
        scope.launch {
            try {
                _bootTime = clock.millis()
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

        meter.upDownCounterBuilder("system.guests")
            .setDescription("Number of guests on this host")
            .setUnit("1")
            .buildWithCallback(::collectGuests)
        meter.gaugeBuilder("system.cpu.limit")
            .setDescription("Amount of CPU resources available to the host")
            .buildWithCallback(::collectCpuLimit)
        meter.gaugeBuilder("system.cpu.demand")
            .setDescription("Amount of CPU resources the guests would use if there were no CPU contention or CPU limits")
            .setUnit("MHz")
            .buildWithCallback { result -> result.observe(_cpuDemand) }
        meter.gaugeBuilder("system.cpu.usage")
            .setDescription("Amount of CPU resources used by the host")
            .setUnit("MHz")
            .buildWithCallback { result -> result.observe(_cpuUsage) }
        meter.gaugeBuilder("system.cpu.utilization")
            .setDescription("Utilization of the CPU resources of the host")
            .setUnit("%")
            .buildWithCallback { result -> result.observe(_cpuUsage / _cpuLimit) }
        meter.counterBuilder("system.cpu.time")
            .setDescription("Amount of CPU time spent by the host")
            .setUnit("s")
            .buildWithCallback(::collectCpuTime)
        meter.gaugeBuilder("system.power.usage")
            .setDescription("Power usage of the host ")
            .setUnit("W")
            .buildWithCallback { result -> result.observe(machine.powerDraw) }
        meter.counterBuilder("system.power.total")
            .setDescription("Amount of energy used by the CPU")
            .setUnit("J")
            .ofDoubles()
            .buildWithCallback(::collectPowerTotal)
        meter.counterBuilder("system.time")
            .setDescription("The uptime of the host")
            .setUnit("s")
            .buildWithCallback(::collectTime)
        meter.gaugeBuilder("system.time.boot")
            .setDescription("The boot time of the host")
            .setUnit("1")
            .ofLongs()
            .buildWithCallback(::collectBootTime)
    }

    override fun canFit(server: Server): Boolean {
        val sufficientMemory = model.memorySize >= server.flavor.memorySize
        val enoughCpus = model.cpuCount >= server.flavor.cpuCount
        val canFit = hypervisor.canFit(server.flavor.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    override suspend fun spawn(server: Server, start: Boolean) {
        val guest = guests.computeIfAbsent(server) { key ->
            require(canFit(key)) { "Server does not fit" }

            val machine = hypervisor.createMachine(key.flavor.toMachineModel(), key.name)
            Guest(
                scope.coroutineContext,
                clock,
                this,
                mapper,
                guestListener,
                server,
                machine
            )
        }

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
        val guest = guests[server] ?: return
        guest.terminate()
    }

    override fun addListener(listener: HostListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: HostListener) {
        listeners.remove(listener)
    }

    override fun close() {
        reset()
        scope.cancel()
        machine.close()
    }

    override fun toString(): String = "SimHost[uid=$uid,name=$name,model=$model]"

    public suspend fun fail() {
        reset()

        for (guest in guests.values) {
            guest.fail()
        }
    }

    public suspend fun recover() {
        collectTime()
        _state = HostState.UP
        _bootTime = clock.millis()

        for (guest in guests.values) {
            guest.recover()
        }
    }

    /**
     * Reset the machine.
     */
    private fun reset() {
        collectTime()

        _state = HostState.DOWN
        _cpuUsage = 0.0
        _cpuDemand = 0.0
    }

    /**
     * Convert flavor to machine model.
     */
    private fun Flavor.toMachineModel(): MachineModel {
        val originalCpu = machine.model.cpus[0]
        val processingNode = originalCpu.node.copy(coreCount = cpuCount)
        val processingUnits = (0 until cpuCount).map { originalCpu.copy(id = it, node = processingNode) }
        val memoryUnits = listOf(MemoryUnit("Generic", "Generic", 3200.0, memorySize))

        return MachineModel(processingUnits, memoryUnits).optimize()
    }

    /**
     * Optimize the [MachineModel] for simulation.
     */
    private fun MachineModel.optimize(): MachineModel {
        if (!optimize) {
            return this
        }

        val originalCpu = cpus[0]
        val freq = cpus.sumOf { it.frequency }
        val processingNode = originalCpu.node.copy(coreCount = 1)
        val processingUnits = listOf(originalCpu.copy(frequency = freq, node = processingNode))

        val memorySize = memory.sumOf { it.size }
        val memoryUnits = listOf(MemoryUnit("Generic", "Generic", 3200.0, memorySize))

        return MachineModel(processingUnits, memoryUnits)
    }

    private val STATE_KEY = AttributeKey.stringKey("state")

    private val terminatedState = Attributes.of(STATE_KEY, "terminated")
    private val runningState = Attributes.of(STATE_KEY, "running")
    private val errorState = Attributes.of(STATE_KEY, "error")
    private val invalidState = Attributes.of(STATE_KEY, "invalid")

    /**
     * Helper function to collect the guest counts on this host.
     */
    private fun collectGuests(result: ObservableLongMeasurement) {
        var terminated = 0L
        var running = 0L
        var error = 0L
        var invalid = 0L

        for ((_, guest) in guests) {
            when (guest.state) {
                ServerState.TERMINATED -> terminated++
                ServerState.RUNNING -> running++
                ServerState.ERROR -> error++
                else -> invalid++
            }
        }

        result.observe(terminated, terminatedState)
        result.observe(running, runningState)
        result.observe(error, errorState)
        result.observe(invalid, invalidState)
    }

    private val _cpuLimit = machine.model.cpus.sumOf { it.frequency }

    /**
     * Helper function to collect the CPU limits of a machine.
     */
    private fun collectCpuLimit(result: ObservableDoubleMeasurement) {
        result.observe(_cpuLimit)

        for (guest in guests.values) {
            guest.collectCpuLimit(result)
        }
    }

    private var _lastCpuTimeCallback = clock.millis()

    /**
     * Helper function to track the CPU time of a machine.
     */
    private fun collectCpuTime(result: ObservableLongMeasurement) {
        val now = clock.millis()
        val duration = now - _lastCpuTimeCallback

        try {
            collectCpuTime(duration, result)
        } finally {
            _lastCpuTimeCallback = now
        }
    }

    private val _activeState = Attributes.of(STATE_KEY, "active")
    private val _stealState = Attributes.of(STATE_KEY, "steal")
    private val _lostState = Attributes.of(STATE_KEY, "lost")
    private val _idleState = Attributes.of(STATE_KEY, "idle")
    private var _totalTime = 0.0

    /**
     * Helper function to track the CPU time of a machine.
     */
    private fun collectCpuTime(duration: Long, result: ObservableLongMeasurement) {
        val coreCount = this.model.cpuCount
        val d = coreCount / _cpuLimit

        val counters = hypervisor.counters
        val grantedWork = counters.actual
        val overcommittedWork = counters.overcommit
        val interferedWork = counters.interference

        _totalTime += (duration / 1000.0) * coreCount
        val activeTime = (grantedWork * d).roundToLong()
        val idleTime = (_totalTime - grantedWork * d).roundToLong()
        val stealTime = (overcommittedWork * d).roundToLong()
        val lostTime = (interferedWork * d).roundToLong()

        result.observe(activeTime, _activeState)
        result.observe(idleTime, _idleState)
        result.observe(stealTime, _stealState)
        result.observe(lostTime, _lostState)

        for (guest in guests.values) {
            guest.collectCpuTime(duration, result)
        }
    }

    private var _lastPowerCallback = clock.millis()
    private var _totalPower = 0.0

    /**
     * Helper function to collect the total power usage of the machine.
     */
    private fun collectPowerTotal(result: ObservableDoubleMeasurement) {
        val now = clock.millis()
        val duration = now - _lastPowerCallback

        _totalPower += duration / 1000.0 * machine.powerDraw
        result.observe(_totalPower)

        _lastPowerCallback = now
    }

    private var _lastReport = clock.millis()

    /**
     * Helper function to track the uptime of a machine.
     */
    private fun collectTime(result: ObservableLongMeasurement? = null) {
        val now = clock.millis()
        val duration = now - _lastReport

        try {
            collectTime(duration, result)
        } finally {
            _lastReport = now
        }
    }

    private var _uptime = 0L
    private var _downtime = 0L
    private val _upState = Attributes.of(STATE_KEY, "up")
    private val _downState = Attributes.of(STATE_KEY, "down")

    /**
     * Helper function to track the uptime of a machine.
     */
    private fun collectTime(duration: Long, result: ObservableLongMeasurement? = null) {
        if (state == HostState.UP) {
            _uptime += duration
        } else if (state == HostState.DOWN && scope.isActive) {
            // Only increment downtime if the machine is in a failure state
            _downtime += duration
        }

        result?.observe(_uptime, _upState)
        result?.observe(_downtime, _downState)

        for (guest in guests.values) {
            guest.collectUptime(duration, result)
        }
    }

    private var _bootTime = Long.MIN_VALUE

    /**
     * Helper function to track the boot time of a machine.
     */
    private fun collectBootTime(result: ObservableLongMeasurement? = null) {
        if (_bootTime != Long.MIN_VALUE) {
            result?.observe(_bootTime)
        }

        for (guest in guests.values) {
            guest.collectBootTime(result)
        }
    }
}
