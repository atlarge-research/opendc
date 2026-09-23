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

package org.opendc.compute.simulator.infrastructure

import org.opendc.common.ResourceType
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.internal.Guest
import org.opendc.compute.simulator.internal.GuestListener
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.GuestCpuStats
import org.opendc.compute.simulator.telemetry.GuestGpuStats
import org.opendc.compute.simulator.telemetry.GuestSystemStats
import org.opendc.compute.simulator.telemetry.HostCpuStats
import org.opendc.compute.simulator.telemetry.HostGpuStats
import org.opendc.compute.simulator.telemetry.HostSystemStats
import org.opendc.simulator.compute.machine.SimMachine
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

/**
 * A simulated physical host that runs [ServiceTask]s as [Guest]s on a [SimMachine].
 *
 * Besides simulating the machine, the host keeps the bookkeeping used by the
 * [org.opendc.compute.simulator.service.ComputeService] and its schedulers to place tasks.
 *
 * @param name The (unique) name of the host.
 * @param clusterName The name of the cluster the host belongs to.
 * @param clock The (virtual) clock used to track time.
 * @param engine The flow engine the machine of this host runs on.
 * @param machineModel The static model of the host.
 * @param cpuPowerModel The power model of the CPU.
 * @param gpuPowerModel The power model of the GPUs, if any.
 * @param embodiedCarbon The embodied carbon of the host.
 * @param expectedLifetime The expected lifetime of the host in years.
 * @param powerDistributor The power distributor to which the host is connected.
 * @param type The type of the host, used by the schedulers to group hosts.
 */
public class SimHost(
    public val name: String,
    public val clusterName: String,
    private val clock: InstantSource,
    private val engine: FlowEngine,
    private val machineModel: MachineModel,
    private val cpuPowerModel: PowerModel,
    private val gpuPowerModel: PowerModel?,
    private val embodiedCarbon: Double,
    private val expectedLifetime: Double,
    private val powerDistributor: FlowDistributor,
    public val type: String = "Unknown",
) : AutoCloseable {
    // ==================================================================================
    // Fields
    // Identity & configuration, state, guests, scheduler bookkeeping, and telemetry bookkeeping.
    // ==================================================================================

    private val gpuHostModels: List<GpuHostModel>? =
        machineModel.gpuModels?.map { gpumodel ->
            return@map GpuHostModel(
                gpumodel.totalCoreCapacity,
                gpumodel.coreCount,
                gpumodel.memorySize,
                gpumodel.memoryBandwidth,
            )
        }

    /**
     * The model of the host as seen by the schedulers.
     */
    public val model: HostModel =
        HostModel(
            machineModel.cpuModel.totalCapacity,
            machineModel.cpuModel.coreCount,
            machineModel.memory.size,
            gpuHostModels,
        )

    /**
     * The event listeners registered with this host.
     */
    private val hostListeners = mutableListOf<HostListener>()

    /**
     * The state of the host. Registered [HostListener]s are notified of every change.
     */
    public var state: HostState = HostState.DOWN
        private set(value) {
            if (value != field) {
                hostListeners.forEach { it.onStateChanged(this, value) }
            }
            field = value
        }

    /**
     * The machine that runs the guests of this host.
     */
    public val simMachine: SimMachine

    /**
     * The virtual machines running on the hypervisor.
     */
    private val taskToGuestMap = HashMap<ServiceTask, Guest>()
    private val guests = mutableSetOf<Guest>()

    /**
     * Capacity reserved by the tasks spawned on this host, used by the schedulers.
     * Updated in [spawn] and [delete].
     */
    public var instanceCount: Int = 0
        private set
    public var availableMemory: Long = model.memoryCapacity
        private set
    public var provisionedCpuCores: Int = 0
        private set
    public var availableCpuCores: Int = model.coreCount
        private set
    public var provisionedGpuCores: Int = 0
        private set

    /**
     * Scheduler bookkeeping
     * Use by schedulers which use a priority queue data structure
     * to keep track of the order of hosts to scheduler tasks on.
     * [org.opendc.compute.simulator.scheduler.MemorizingScheduler] for example.
     * MemorizingScheduler has an array of lists
     * The 0th index of the array has a list of hosts with 0 tasks,
     * 1st index of the array has hosts with 1 task, and so on.
     * The priorityIndex points to the index of this the list this host
     * belongs to in the array.
     * The listIndex is the position of this host in the list.
     */
    public var priorityIndex: Int = 0
    public var listIndex: Int = 0

    private var lastReport = clock.millis()
    private var totalUptime = 0L
    private var totalDowntime = 0L
    private var bootTime: Instant? = null
    private val embodiedCarbonRate: Double =
        (embodiedCarbon * 1000) / (expectedLifetime * 365.0 * 24.0 * 60.0 * 60.0 * 1000.0)

    // ==================================================================================
    // Construction
    // ==================================================================================

    init {
        launch()

        simMachine =
            SimMachine(
                engine,
                machineModel,
                powerDistributor,
                cpuPowerModel,
                gpuPowerModel,
            ) { cause ->
                state = if (cause != null) HostState.ERROR else HostState.DOWN
            }
    }

    // ==================================================================================
    // Lifecycle
    // Booting, failing, recovering, and shutting down the host.
    // ==================================================================================

    /**
     * Launch the hypervisor.
     */
    private fun launch() {
        bootTime = clock.instant()
        state = HostState.UP
    }

    override fun close() {
        reset(HostState.DOWN)
    }

    public fun fail() {
        reset(HostState.ERROR)

        // Fail the guest and delete them
        // This weird loop is the only way I have been able to make it work.
        while (guests.isNotEmpty()) {
            val guest = guests.first()
            guest.fail()
            this.delete(guest.task)
        }
    }

    public fun recover() {
        updateUptime()

        launch()
    }

    public fun pauseAllTasks() {
        while (guests.isNotEmpty()) {
            val guest = guests.first()
            guest.pause()
            this.delete(guest.task)
        }
    }

    /**
     * Reset the machine.
     */
    private fun reset(state: HostState) {
        updateUptime()

        // Stop the hypervisor
        this.state = state
    }

    // ==================================================================================
    // Task placement
    // Checking whether tasks fit, and spawning and deleting the guests that run them.
    // ==================================================================================

    public fun canFit(task: ServiceTask): Boolean {
        val sufficientMemory = (model.memoryCapacity - this.availableMemory) >= task.memorySize
        val enoughCpus = model.coreCount >= task.cpuCoreCount
        val canFit = simMachine.canFit(task.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    /**
     * Spawn A Virtual machine that run the Task and put this Task as a Guest on it
     *
     * @param task
     */
    public fun spawn(task: ServiceTask) {
        require(canFit(task)) { "Task does not fit" }

        val newGuest =
            Guest(
                clock,
                this,
                guestListener,
                task,
                simMachine,
            )

        guests.add(newGuest)
        taskToGuestMap.computeIfAbsent(task) { newGuest }

        // Reserve before starting, so a guest that stops immediately is released in [delete]
        reserve(task)

        newGuest.start()
    }

    public fun delete(task: ServiceTask) {
        val guest = taskToGuestMap[task] ?: return

        taskToGuestMap.remove(task)
        guests.remove(guest)
        task.host = null

        release(task)
    }

    public fun isEmpty(): Boolean {
        return guests.isEmpty()
    }

    public fun getInstances(): Set<ServiceTask> {
        return taskToGuestMap.keys
    }

    public fun getGuests(): List<Guest> {
        return this.guests.toList()
    }

    // ==================================================================================
    // Listeners
    // Forwarding host and guest events to the registered HostListeners.
    // ==================================================================================

    public fun addListener(listener: HostListener) {
        hostListeners.add(listener)
    }

    public fun removeListener(listener: HostListener) {
        hostListeners.remove(listener)
    }

    /**
     * The [GuestListener] that listens for guest events.
     */
    private val guestListener =
        object : GuestListener {
            override fun onStart(guest: Guest) {
                hostListeners.forEach { it.onStateChanged(this@SimHost, guest.task, guest.state) }
            }

            override fun onStop(guest: Guest) {
                hostListeners.forEach { it.onStateChanged(this@SimHost, guest.task, guest.state) }
            }
        }

    // ==================================================================================
    // Telemetry
    // Statistics of the host and of the guests running on it, for the metric exporters.
    // ==================================================================================

    public fun getSystemStats(): HostSystemStats {
        val now = clock.millis()
        val duration = now - lastReport
        updateUptime()
        simMachine.psu.updateCounters()

        var running = 0
        var failed = 0
        var invalid = 0

        for (guest in guests) {
            when (guest.state) {
                TaskState.RUNNING -> running++
                TaskState.FAILED, TaskState.TERMINATED -> failed++
                TaskState.COMPLETED, TaskState.PAUSED -> {}
                else -> invalid++
            }
        }

        return HostSystemStats(
            Duration.ofMillis(totalUptime),
            Duration.ofMillis(totalDowntime),
            bootTime,
            simMachine.psu.powerDraw,
            simMachine.psu.energyUsage,
            simMachine.psu.carbonIntensity,
            simMachine.psu.carbonEmission,
            embodiedCarbonRate * duration,
            // Terminated guests are deleted from the host, so they are never counted here
            0,
            running,
            failed,
            invalid,
        )
    }

    public fun getSystemStats(task: ServiceTask): GuestSystemStats? {
        val guest = taskToGuestMap[task] ?: return null
        return guest.getSystemStats()
    }

    public fun getCpuStats(): HostCpuStats {
        simMachine.cpu.updateCounters(this.clock.millis())

        val counters = simMachine.performanceCounters

        return HostCpuStats(
            counters.activeTime,
            counters.idleTime,
            counters.stealTime,
            counters.lostTime,
            counters.capacity,
            counters.demand,
            counters.supply,
            counters.supply / model.cpuCapacity,
        )
    }

    public fun getCpuStats(task: ServiceTask): GuestCpuStats? {
        val guest = taskToGuestMap[task] ?: return null
        return guest.getCpuStats()
    }

    public fun getGpuStats(): List<HostGpuStats> {
        val gpuStats = mutableListOf<HostGpuStats>()
        for (gpu in simMachine.gpus) {
            gpu.updateCounters(this.clock.millis())
            val counters = simMachine.getGpuPerformanceCounters(gpu.id)

            gpuStats.add(
                HostGpuStats(
                    counters.activeTime,
                    counters.idleTime,
                    counters.stealTime,
                    counters.lostTime,
                    counters.capacity,
                    counters.demand,
                    counters.supply,
                    counters.supply / gpu.getCapacity(ResourceType.GPU),
                    counters.powerDraw,
                ),
            )
        }
        return gpuStats
    }

    public fun getGpuStats(task: ServiceTask): GuestGpuStats? {
        val guest = taskToGuestMap[task] ?: return null
        return guest.getGpuStats()
    }

    // ==================================================================================
    // Object overrides
    // ==================================================================================

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is SimHost && name == other.name
    }

    override fun toString(): String = "SimHost[uid=$name,name=$name,model=$model]"

    // ==================================================================================
    // Internals
    // ==================================================================================

    /**
     * Reserve this host's capacity for the given task.
     */
    private fun reserve(task: ServiceTask) {
        instanceCount++
        provisionedCpuCores += task.cpuCoreCount
        availableCpuCores -= task.cpuCoreCount
        availableMemory -= task.memorySize
        provisionedGpuCores += task.gpuCoreCount
    }

    /**
     * Release the capacity previously reserved for the given task.
     */
    private fun release(task: ServiceTask) {
        instanceCount--
        provisionedCpuCores -= task.cpuCoreCount
        availableCpuCores += task.cpuCoreCount
        availableMemory += task.memorySize
        provisionedGpuCores -= task.gpuCoreCount
    }

    /**
     * Convert flavor to machine model.
     */
    private fun ServiceTask.toMachineModel(): MachineModel {
        return MachineModel(
            simMachine.machineModel.cpuModel,
            MemoryUnit("Generic", "Generic", 3200.0, this.memorySize),
            simMachine.machineModel.gpuModels,
            simMachine.machineModel.cpuDistributionStrategy,
            simMachine.machineModel.gpuDistributionStrategy,
        )
    }

    /**
     * Helper function to track the uptime of a machine.
     */
    private fun updateUptime() {
        val now = clock.millis()
        val duration = now - lastReport
        lastReport = now

        if (state == HostState.UP) {
            totalUptime += duration
        } else if (state == HostState.ERROR) {
            // Only increment downtime if the machine is in a failure state
            totalDowntime += duration
        }

        for (guest in guests) {
            guest.updateUptime()
        }
    }
}
