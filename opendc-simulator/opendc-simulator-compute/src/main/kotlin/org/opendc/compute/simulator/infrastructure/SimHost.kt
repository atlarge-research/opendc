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
import org.opendc.compute.simulator.service.SimTask
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
 * A simulated physical host that runs [SimTask]s on a [SimMachine].
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
    // Identity & configuration, state, tasks, scheduler bookkeeping, and telemetry bookkeeping.
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
     * The machine that runs the tasks of this host.
     */
    public val simMachine: SimMachine

    /**
     * The tasks placed on this host, in the order they were spawned.
     */
    private val tasks = LinkedHashSet<SimTask>()

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

        // Fail the tasks and delete them. Stopping a task can already remove it from this host (through the
        // listeners), so take the first remaining task each time instead of iterating over the set.
        while (tasks.isNotEmpty()) {
            val task = tasks.first()
            task.stopRun(TaskState.FAILED)
            this.delete(task)
        }
    }

    public fun recover() {
        updateUptime()

        launch()
    }

    public fun pauseAllTasks() {
        while (tasks.isNotEmpty()) {
            val task = tasks.first()
            task.stopRun(TaskState.PAUSED)
            this.delete(task)
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
    // Checking whether tasks fit, and spawning and deleting them.
    // ==================================================================================

    public fun canFit(task: SimTask): Boolean {
        val sufficientMemory = (this.availableMemory) >= task.memorySize
        val enoughCpus = model.coreCount >= task.cpuCoreCount
        val canFit = simMachine.canFit(task.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    /**
     * Place [task] on this host and start running it on the machine.
     */
    public fun spawn(task: SimTask) {
        require(canFit(task)) { "Task does not fit" }

        tasks.add(task)

        // Reserve before starting, so a task that stops immediately is released in [delete]
        reserve(task)

        task.startRun(this)
    }

    public fun delete(task: SimTask) {
        if (!tasks.remove(task)) {
            return
        }

        task.host = null
        // Detach the machine, so a callback from it that arrives later is ignored by the task
        task.virtualMachine = null

        release(task)
    }

    public fun isEmpty(): Boolean {
        return tasks.isEmpty()
    }

    /**
     * The tasks placed on this host, in the order they were spawned.
     */
    public fun getInstances(): Set<SimTask> {
        return tasks
    }

    // ==================================================================================
    // Listeners
    // Forwarding host and task events to the registered HostListeners.
    // ==================================================================================

    public fun addListener(listener: HostListener) {
        hostListeners.add(listener)
    }

    public fun removeListener(listener: HostListener) {
        hostListeners.remove(listener)
    }

    /**
     * Notify the listeners that the state of [task], which runs on this host, has changed.
     */
    internal fun onTaskStateChanged(task: SimTask) {
        hostListeners.forEach { it.onStateChanged(this, task, task.state) }
    }

    // ==================================================================================
    // Telemetry
    // Statistics of the host and of the tasks running on it, for the metric exporters.
    // ==================================================================================

    public fun getSystemStats(): HostSystemStats {
        val now = clock.millis()
        val duration = now - lastReport
        updateUptime()
        simMachine.psu.updateCounters()

        var running = 0
        var failed = 0
        var invalid = 0

        for (task in tasks) {
            when (task.state) {
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
            // Terminated tasks are deleted from the host, so they are never counted here
            0,
            running,
            failed,
            invalid,
        )
    }

    public fun getSystemStats(task: SimTask): GuestSystemStats? {
        if (task !in tasks) {
            return null
        }

        // A task runs from the moment it is placed on this host, which is when the ComputeService sets scheduledAt.
        // Tasks that fail or pause are removed from the host right away, so a task on this host has no downtime.
        return GuestSystemStats(Duration.ofMillis(clock.millis() - task.scheduledAt), Duration.ZERO)
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

    public fun getCpuStats(task: SimTask): GuestCpuStats? {
        if (task !in tasks) {
            return null
        }

        val virtualMachine = task.virtualMachine!!
        virtualMachine.updateCounters(clock.millis())
        val counters = virtualMachine.cpuPerformanceCounters

        return GuestCpuStats(
            counters.activeTime / 1000L,
            counters.idleTime / 1000L,
            counters.stealTime / 1000L,
            counters.lostTime / 1000L,
            counters.capacity,
            counters.supply,
            counters.demand,
            counters.supply / simMachine.cpu.cpuModel.totalCapacity,
        )
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

    public fun getGpuStats(task: SimTask): GuestGpuStats? {
        if (task !in tasks) {
            return null
        }

        val virtualMachine = task.virtualMachine!!
        virtualMachine.updateCounters(clock.millis())
        val counters = virtualMachine.gpuPerformanceCounters ?: return null
        val gpuLimit = simMachine.gpus?.firstOrNull()?.gpuModel?.totalCoreCapacity ?: 0.0

        return GuestGpuStats(
            counters.activeTime / 1000L,
            counters.idleTime / 1000L,
            counters.stealTime / 1000L,
            counters.lostTime / 1000L,
            counters.capacity,
            counters.supply,
            counters.demand,
            counters.supply / gpuLimit,
        )
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
    public fun reserve(task: SimTask) {
        instanceCount++
        provisionedCpuCores += task.cpuCoreCount
        availableCpuCores -= task.cpuCoreCount
        availableMemory -= task.memorySize
        provisionedGpuCores += task.gpuCoreCount
    }

    /**
     * Release the capacity previously reserved for the given task.
     */
    public fun release(task: SimTask) {
        instanceCount--
        provisionedCpuCores -= task.cpuCoreCount
        availableCpuCores += task.cpuCoreCount
        availableMemory += task.memorySize
        provisionedGpuCores -= task.gpuCoreCount
    }

    /**
     * Convert flavor to machine model.
     */
    private fun SimTask.toMachineModel(): MachineModel {
        return MachineModel(
            simMachine.machineModel.cpuModel,
            MemoryUnit("Generic", "Generic", 3200.0, this.memorySize.toLong()),
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
    }
}
