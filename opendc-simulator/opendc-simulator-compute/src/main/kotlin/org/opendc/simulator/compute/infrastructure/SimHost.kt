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

package org.opendc.simulator.compute.infrastructure

import org.opendc.common.units.DataSize
import org.opendc.simulator.compute.machine.SimMachine
import org.opendc.simulator.compute.models.GpuHostModel
import org.opendc.simulator.compute.models.HostListener
import org.opendc.simulator.compute.models.HostModel
import org.opendc.simulator.compute.models.HostState
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.compute.task.SimTask
import org.opendc.simulator.compute.task.TaskState
import org.opendc.simulator.compute.telemetry.GuestCpuStats
import org.opendc.simulator.compute.telemetry.GuestGpuStats
import org.opendc.simulator.compute.telemetry.GuestSystemStats
import org.opendc.simulator.compute.telemetry.HostCpuStats
import org.opendc.simulator.compute.telemetry.HostGpuStats
import org.opendc.simulator.compute.telemetry.HostSystemStats
import org.opendc.simulator.core.ResourceType
import org.opendc.simulator.flow.engine.FlowEngine
import org.opendc.simulator.flow.graph.FlowDistributor
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

/**
 * A simulated physical host that runs [SimTask]s on a [SimMachine].
 *
 * Besides simulating the machine, the host keeps the bookkeeping used by the
 * [org.opendc.simulator.compute.service.ComputeService] and its schedulers to place tasks.
 *
 * @param id Identifies the host: hosts are equal when their ids are, so ids must be unique within a simulation.
 * @param name The (unique) name of the host.
 * @param clusterId The id of the cluster the host belongs to.
 * @param clock The (virtual) clock used to track time.
 * @param engine The flow engine the machine of this host runs on.
 * @param machineModel The static model of the host.
 * @param cpuPowerModel The power model of the CPU.
 * @param gpuPowerModel The power model of the GPUs, if any.
 * @param embodiedCarbon The embodied carbon of the host.
 * @param expectedLifetime The expected lifetime of the host in years.
 * @param powerDistributor The power distributor to which the host is connected.
 */
public class SimHost(
    public val id: Int,
    public val name: String,
    public val clusterId: Int,
    private val clock: InstantSource,
    private val engine: FlowEngine,
    private val machineModel: MachineModel,
    private val cpuPowerModel: PowerModel,
    private val gpuPowerModel: PowerModel?,
    private val embodiedCarbon: Double,
    private val expectedLifetime: Double,
    private val powerDistributor: FlowDistributor,
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
     * The number of [tasks] in each [TaskState], indexed by ordinal. It is kept up to date as tasks are spawned, deleted
     * and change state, so that the statistics do not have to visit every task at every sample.
     */
    private val taskStateCounts = IntArray(TaskState.entries.size)

    /**
     * Capacity reserved by the tasks spawned on this host, used by the schedulers.
     * Updated in [spawn] and [delete].
     */
    public var instanceCount: Int = 0
        private set
    public var availableMemory: DataSize = DataSize.ofMiB(model.memoryCapacity)
        private set
    public var provisionedCpuCores: Int = 0
        private set
    public var availableCpuCores: Int = model.coreCount
        private set
    public var provisionedGpuCores: Int = 0
        private set

    /**
     * Identifies the [model] of this host: hosts with an equal model get the same id, so schedulers can group identical
     * hosts by comparing a number instead of the model. Assigned by whoever creates the hosts, before they are added
     * to a scheduler; -1 until then. The model of a host does not change during a simulation.
     */
    public var modelId: Int = -1

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
        val sufficientMemory = availableMemory >= task.memorySize
        val enoughCpus = model.coreCount >= task.cpuCoreCount
        val canFit = simMachine.canFit(task.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    /**
     * Place [task] on this host and start running it on the machine.
     */
    public fun spawn(task: SimTask) {
        require(canFit(task)) { "Task does not fit" }

        if (tasks.add(task)) {
            taskStateCounts[task.state.ordinal]++
        }

        // Reserve before starting, so a task that stops immediately is released in [delete]
        reserve(task)

        task.startRun(this)
    }

    public fun delete(task: SimTask) {
        if (!tasks.remove(task)) {
            return
        }
        taskStateCounts[task.state.ordinal]--

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
     * Count the change of the state of [task] from [oldState], if it is on this host. Called by [SimTask] at every change
     * of its state.
     */
    internal fun onTaskStateCountChanged(
        task: SimTask,
        oldState: TaskState,
    ) {
        if (task in tasks) {
            taskStateCounts[oldState.ordinal]--
            taskStateCounts[task.state.ordinal]++
        }
    }

    /**
     * Count the [tasks] in each state by visiting them, to check [taskStateCounts].
     */
    private fun countTaskStates(): IntArray {
        val counts = IntArray(TaskState.entries.size)
        for (task in tasks) {
            counts[task.state.ordinal]++
        }
        return counts
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

        if (CHECK_TASK_STATE_COUNTS) {
            check(taskStateCounts.contentEquals(countTaskStates())) { "The task state counts of host $name are out of date" }
        }

        val counts = taskStateCounts
        val running = counts[TaskState.RUNNING.ordinal]
        val failed = counts[TaskState.FAILED.ordinal] + counts[TaskState.TERMINATED.ordinal]
        // The tasks in any other state than these are invalid on a host
        val invalid = tasks.size - running - failed - counts[TaskState.COMPLETED.ordinal] - counts[TaskState.PAUSED.ordinal]

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

    /**
     * Whether [task] is running on this host. This is cheaper than looking it up in [tasks], which the exporters do for
     * every task at every sample: a task on this host has it as its host and has a virtual machine, and [delete] clears
     * both.
     */
    private fun hasTask(task: SimTask): Boolean = task.host === this && task.virtualMachine != null

    public fun getSystemStats(task: SimTask): GuestSystemStats? {
        if (!hasTask(task)) {
            return null
        }

        // A task runs from the moment it is placed on this host, which is when the ComputeService sets scheduledAt.
        // Tasks that fail or pause are removed from the host right away, so a task on this host has no downtime.
        val scheduledAt = checkNotNull(task.scheduledAt) { "Task ${task.id} is on host $name without having been scheduled" }
        return GuestSystemStats(Duration.ofMillis(clock.millis() - scheduledAt.toEpochMs().toLong()), Duration.ZERO)
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
        if (!hasTask(task)) {
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
        if (!hasTask(task)) {
            return null
        }

        val virtualMachine = task.virtualMachine!!
        val counters = virtualMachine.gpuPerformanceCounters ?: return null
        virtualMachine.updateCounters(clock.millis())
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

    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean = other is SimHost && id == other.id

    override fun toString(): String = "SimHost[id=$id,name=$name,model=$model]"

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
            MemoryUnit("Generic", "Generic", 3200.0, this.memorySize.toMiB().toLong()),
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

/**
 * Whether the task state counts of the hosts are checked against counting the tasks, at every sample. This happens when
 * assertions are enabled, as in the tests.
 */
private val CHECK_TASK_STATE_COUNTS = SimHost::class.java.desiredAssertionStatus()
