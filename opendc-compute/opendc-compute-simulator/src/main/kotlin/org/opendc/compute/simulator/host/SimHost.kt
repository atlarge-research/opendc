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

package org.opendc.compute.simulator.host

import org.opendc.compute.api.Flavor
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.internal.Guest
import org.opendc.compute.simulator.internal.GuestListener
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.GuestCpuStats
import org.opendc.compute.simulator.telemetry.GuestSystemStats
import org.opendc.compute.simulator.telemetry.HostCpuStats
import org.opendc.compute.simulator.telemetry.HostSystemStats
import org.opendc.simulator.Multiplexer
import org.opendc.simulator.compute.cpu.CpuPowerModel
import org.opendc.simulator.compute.machine.SimMachine
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.engine.FlowGraph
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.util.UUID

/**
 * A [Host] implementation that simulates virtual machines on a physical machine.
 *
 * @param uid The unique identifier of the host.
 * @param name The name of the host.
 * @param meta The metadata of the host.
 * @param clock The (virtual) clock used to track time.
 * @param graph The Flow Graph that the Host is part of
 * @param machineModel The static model of the host
 * @param powerModel The static powerModel of the CPU TODO: can this be combined with machinemodel?
 * @constructor Create empty Sim host
 */
public class SimHost(
    private val uid: UUID,
    private val name: String,
    private val meta: Map<String, Any>,
    private val clock: InstantSource,
    private val graph: FlowGraph,
    private val machineModel: MachineModel,
    private val powerModel: CpuPowerModel,
    private val powerMux: Multiplexer,
) : AutoCloseable {
    /**
     * The event listeners registered with this host.
     */
    private val hostListeners = mutableListOf<HostListener>()

    /**
     * The virtual machines running on the hypervisor.
     */
    private val taskToGuestMap = HashMap<ServiceTask, Guest>()
    private val guests = mutableListOf<Guest>()

    private var hostState: HostState = HostState.DOWN
        set(value) {
            if (value != field) {
                hostListeners.forEach { it.onStateChanged(this, value) }
            }
            field = value
        }

    private val model: HostModel =
        HostModel(
            machineModel.cpu.totalCapacity,
            machineModel.cpu.coreCount,
            machineModel.memory.size,
        )

    private var simMachine: SimMachine? = null

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

    private var lastReport = clock.millis()
    private var totalUptime = 0L
    private var totalDowntime = 0L
    private var bootTime: Instant? = null
    private val cpuLimit = machineModel.cpu.totalCapacity

    init {
        launch()
    }

    /**
     * Launch the hypervisor.
     */
    private fun launch() {
        bootTime = this.clock.instant()
        hostState = HostState.UP

        if (this.simMachine != null) {
            return
        }

        this.simMachine =
            SimMachine(
                this.graph,
                this.machineModel,
                this.powerModel,
                this.powerMux,
            ) { cause ->
                hostState = if (cause != null) HostState.ERROR else HostState.DOWN
            }
    }

    override fun close() {
        reset(HostState.DOWN)
    }

    public fun fail() {
        reset(HostState.ERROR)

        // Fail the guest and delete them
        // This weird loop is the only way I have been able to make it work.
        while (guests.size > 0) {
            val guest = guests[0]
            guest.fail()
            this.delete(guest.task)
        }
    }

    public fun recover() {
        updateUptime()

        launch()
    }

    /**
     * Reset the machine.
     */
    private fun reset(state: HostState) {
        updateUptime()

        // Stop the hypervisor
        hostState = state
    }

    public fun getUid(): UUID {
        return uid
    }

    public fun getName(): String {
        return name
    }

    public fun getModel(): HostModel {
        return model
    }

    public fun getMeta(): Map<String, *> {
        return meta
    }

    public fun getState(): HostState {
        return hostState
    }

    public fun getInstances(): Set<ServiceTask> {
        return taskToGuestMap.keys
    }

    public fun getGuests(): List<Guest> {
        return this.guests
    }

    public fun canFit(task: ServiceTask): Boolean {
        val sufficientMemory = model.memoryCapacity >= task.flavor.memorySize
        val enoughCpus = model.coreCount >= task.flavor.coreCount
        val canFit = simMachine!!.canFit(task.flavor.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    /**
     * Spawn A Virtual machine that run the Task and put this Task as a Guest on it
     *
     * @param task
     */
    public fun spawn(task: ServiceTask) {
        assert(simMachine != null) { "Tried start task $task while no SimMachine is active" }

        require(canFit(task)) { "Task does not fit" }

        val newGuest =
            Guest(
                clock,
                this,
                guestListener,
                task,
                simMachine!!,
            )

        guests.add(newGuest)
        newGuest.start()

        taskToGuestMap.computeIfAbsent(task) { newGuest }
    }

    public fun contains(task: ServiceTask): Boolean {
        return task in taskToGuestMap
    }

    public fun start(task: ServiceTask) {
        val guest = requireNotNull(taskToGuestMap[task]) { "Unknown task ${task.uid} at host $uid" }
        guest.start()
    }

    public fun stop(task: ServiceTask) {
        val guest = requireNotNull(taskToGuestMap[task]) { "Unknown task ${task.uid} at host $uid" }
        guest.stop()
    }

    public fun delete(task: ServiceTask) {
        val guest = taskToGuestMap[task] ?: return
        guest.delete()

        taskToGuestMap.remove(task)
        guests.remove(guest)
        task.host = null
    }

    public fun removeTask(task: ServiceTask) {
        val guest = taskToGuestMap[task] ?: return
        guest.delete()

        taskToGuestMap.remove(task)
        guests.remove(guest)
    }

    public fun addListener(listener: HostListener) {
        hostListeners.add(listener)
    }

    public fun removeListener(listener: HostListener) {
        hostListeners.remove(listener)
    }

    public fun getSystemStats(): HostSystemStats {
        updateUptime()
        this.simMachine!!.psu.updateCounters()

        var terminated = 0
        var running = 0
        var failed = 0
        var invalid = 0
        var completed = 0

        val guests = guests.listIterator()
        for (guest in guests) {
            when (guest.state) {
                TaskState.RUNNING -> running++
                TaskState.COMPLETED, TaskState.FAILED, TaskState.TERMINATED -> {
                    failed++
                    // Remove guests that have been deleted
                    this.taskToGuestMap.remove(guest.task)
                    guests.remove()
                }
                else -> invalid++
            }
        }

        return HostSystemStats(
            Duration.ofMillis(totalUptime),
            Duration.ofMillis(totalDowntime),
            bootTime,
            simMachine!!.psu.powerDraw,
            simMachine!!.psu.energyUsage,
            terminated,
            running,
            failed,
            invalid,
        )
    }

    public fun getSystemStats(task: ServiceTask): GuestSystemStats {
        val guest = requireNotNull(taskToGuestMap[task]) { "Unknown task ${task.uid} at host $uid" }
        return guest.getSystemStats()
    }

    public fun getCpuStats(): HostCpuStats {
        simMachine!!.cpu.updateCounters(this.clock.millis())

        val counters = simMachine!!.performanceCounters

        return HostCpuStats(
            counters.cpuActiveTime,
            counters.cpuIdleTime,
            counters.cpuStealTime,
            counters.cpuLostTime,
            counters.cpuCapacity,
            counters.cpuDemand,
            counters.cpuSupply,
            counters.cpuSupply / cpuLimit,
        )
    }

    public fun getCpuStats(task: ServiceTask): GuestCpuStats {
        val guest = requireNotNull(taskToGuestMap[task]) { "Unknown task ${task.uid} at host $uid" }
        return guest.getCpuStats()
    }

    override fun hashCode(): Int = uid.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is SimHost && uid == other.uid
    }

    override fun toString(): String = "SimHost[uid=$uid,name=$name,model=$model]"

    /**
     * Convert flavor to machine model.
     */
    private fun Flavor.toMachineModel(): MachineModel {
        return MachineModel(simMachine!!.machineModel.cpu, MemoryUnit("Generic", "Generic", 3200.0, memorySize))
    }

    /**
     * Helper function to track the uptime of a machine.
     */
    private fun updateUptime() {
        val now = clock.millis()
        val duration = now - lastReport
        lastReport = now

        if (hostState == HostState.UP) {
            totalUptime += duration
        } else if (hostState == HostState.ERROR) {
            // Only increment downtime if the machine is in a failure state
            totalDowntime += duration
        }

        val guests = guests
        for (i in guests.indices) {
            guests[i].updateUptime()
        }
    }
}
