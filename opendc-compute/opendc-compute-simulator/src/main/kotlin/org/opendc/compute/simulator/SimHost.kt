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

import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Task
import org.opendc.compute.api.TaskState
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostListener
import org.opendc.compute.service.driver.HostModel
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.driver.telemetry.GuestCpuStats
import org.opendc.compute.service.driver.telemetry.GuestSystemStats
import org.opendc.compute.service.driver.telemetry.HostCpuStats
import org.opendc.compute.service.driver.telemetry.HostSystemStats
import org.opendc.compute.simulator.internal.DefaultWorkloadMapper
import org.opendc.compute.simulator.internal.Guest
import org.opendc.compute.simulator.internal.GuestListener
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.kernel.SimHypervisor
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.compute.workload.SimWorkloads
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.util.UUID
import java.util.function.Supplier

/**
 * A [Host] implementation that simulates virtual machines on a physical machine using [SimHypervisor].
 *
 * @param uid The unique identifier of the host.
 * @param name The name of the host.
 * @param meta The metadata of the host.
 * @param clock The (virtual) clock used to track time.
 * @param machine The [SimBareMetalMachine] on which the host runs.
 * @param hypervisor The [SimHypervisor] to run on top of the machine.
 * @param mapper A [SimWorkloadMapper] to map a [Task] to a [SimWorkload].
 * @param bootModel A [Supplier] providing the [SimWorkload] to execute during the boot procedure of the hypervisor.
 * @param optimize A flag to indicate to optimize the machine models of the virtual machines.
 */
public class SimHost(
    private val uid: UUID,
    private val name: String,
    private val meta: Map<String, Any>,
    private val clock: InstantSource,
    private val machine: SimBareMetalMachine,
    private val hypervisor: SimHypervisor,
    private val mapper: SimWorkloadMapper = DefaultWorkloadMapper,
    private val bootModel: Supplier<SimWorkload?> = Supplier { null },
    private val optimize: Boolean = false,
) : Host, AutoCloseable {
    /**
     * The event listeners registered with this host.
     */
    private val listeners = mutableListOf<HostListener>()

    /**
     * The virtual machines running on the hypervisor.
     */
    private val guests = HashMap<Task, Guest>()
    private val localGuests = mutableListOf<Guest>()

    private var localState: HostState = HostState.DOWN
        set(value) {
            if (value != field) {
                listeners.forEach { it.onStateChanged(this, value) }
            }
            field = value
        }

    private val model: HostModel =
        HostModel(
            machine.model.cpu.totalCapacity,
            machine.model.cpu.coreCount,
            machine.model.memory.size,
        )

    /**
     * The [GuestListener] that listens for guest events.
     */
    private val guestListener =
        object : GuestListener {
            override fun onStart(guest: Guest) {
                listeners.forEach { it.onStateChanged(this@SimHost, guest.task, guest.state) }
            }

            override fun onStop(guest: Guest) {
                listeners.forEach { it.onStateChanged(this@SimHost, guest.task, guest.state) }
            }
        }

    init {
        launch()
    }

    override fun getUid(): UUID {
        return uid
    }

    override fun getName(): String {
        return name
    }

    override fun getModel(): HostModel {
        return model
    }

    override fun getMeta(): Map<String, *> {
        return meta
    }

    override fun getState(): HostState {
        return localState
    }

    override fun getInstances(): Set<Task> {
        return guests.keys
    }

    override fun canFit(task: Task): Boolean {
        val sufficientMemory = model.memoryCapacity >= task.flavor.memorySize
        val enoughCpus = model.coreCount >= task.flavor.coreCount
        val canFit = hypervisor.canFit(task.flavor.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

    override fun spawn(task: Task) {
        guests.computeIfAbsent(task) { key ->
            require(canFit(key)) { "Task does not fit" }

            val machine = hypervisor.newMachine(key.flavor.toMachineModel())
            val newGuest =
                Guest(
                    clock,
                    this,
                    hypervisor,
                    mapper,
                    guestListener,
                    task,
                    machine,
                )

            localGuests.add(newGuest)
            newGuest
        }
    }

    override fun contains(task: Task): Boolean {
        return task in guests
    }

    override fun start(task: Task) {
        val guest = requireNotNull(guests[task]) { "Unknown task ${task.uid} at host $uid" }
        guest.start()
    }

    override fun stop(task: Task) {
        val guest = requireNotNull(guests[task]) { "Unknown task ${task.uid} at host $uid" }
        guest.stop()
    }

    override fun delete(task: Task) {
        val guest = guests[task] ?: return
        guest.delete()

        guests.remove(task)
        localGuests.remove(guest)
    }

    override fun addListener(listener: HostListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: HostListener) {
        listeners.remove(listener)
    }

    override fun close() {
        reset(HostState.DOWN)
        machine.cancel()
    }

    override fun getSystemStats(): HostSystemStats {
        updateUptime()

        var terminated = 0
        var running = 0
        var error = 0
        var invalid = 0

        val guests = localGuests.listIterator()
        for (guest in guests) {
            when (guest.state) {
                TaskState.TERMINATED -> terminated++
                TaskState.RUNNING -> running++
                TaskState.ERROR -> error++
                TaskState.DELETED -> {
                    // Remove guests that have been deleted
                    this.guests.remove(guest.task)
                    guests.remove()
                }
                else -> invalid++
            }
        }

        return HostSystemStats(
            Duration.ofMillis(localUptime),
            Duration.ofMillis(localDowntime),
            localBootTime,
            machine.psu.powerDraw,
            machine.psu.energyUsage,
            terminated,
            running,
            error,
            invalid,
        )
    }

    override fun getSystemStats(task: Task): GuestSystemStats {
        val guest = requireNotNull(guests[task]) { "Unknown task ${task.uid} at host $uid" }
        return guest.getSystemStats()
    }

    override fun getCpuStats(): HostCpuStats {
        val counters = hypervisor.counters
        counters.sync()

        return HostCpuStats(
            counters.cpuActiveTime,
            counters.cpuIdleTime,
            counters.cpuStealTime,
            counters.cpuLostTime,
            hypervisor.cpuCapacity,
            hypervisor.cpuDemand,
            hypervisor.cpuUsage,
            hypervisor.cpuUsage / localCpuLimit,
        )
    }

    override fun getCpuStats(task: Task): GuestCpuStats {
        val guest = requireNotNull(guests[task]) { "Unknown task ${task.uid} at host $uid" }
        return guest.getCpuStats()
    }

    override fun hashCode(): Int = uid.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is SimHost && uid == other.uid
    }

    override fun toString(): String = "SimHost[uid=$uid,name=$name,model=$model]"

    public fun fail() {
        reset(HostState.ERROR)

        for (guest in localGuests) {
            guest.fail()
        }
    }

    public fun recover() {
        updateUptime()

        launch()
    }

    /**
     * The [SimMachineContext] that represents the machine running the hypervisor.
     */
    private var ctx: SimMachineContext? = null

    /**
     * Launch the hypervisor.
     */
    private fun launch() {
        check(ctx == null) { "Concurrent hypervisor running" }

        val bootWorkload = bootModel.get()
        val hypervisor = hypervisor
        val hypervisorWorkload =
            object : SimWorkload by hypervisor {
                override fun onStart(ctx: SimMachineContext) {
                    try {
                        localBootTime = clock.instant()
                        localState = HostState.UP
                        hypervisor.onStart(ctx)

                        // Recover the guests that were running on the hypervisor.
                        for (guest in localGuests) {
                            guest.recover()
                        }
                    } catch (cause: Throwable) {
                        localState = HostState.ERROR
                        throw cause
                    }
                }
            }

        val workload = if (bootWorkload != null) SimWorkloads.chain(bootWorkload, hypervisorWorkload) else hypervisorWorkload

        // Launch hypervisor onto machine
        ctx =
            machine.startWorkload(workload, emptyMap()) { cause ->
                localState = if (cause != null) HostState.ERROR else HostState.DOWN
                ctx = null
            }
    }

    /**
     * Reset the machine.
     */
    private fun reset(state: HostState) {
        updateUptime()

        // Stop the hypervisor
        ctx?.shutdown()
        localState = state
    }

    /**
     * Convert flavor to machine model.
     */
    private fun Flavor.toMachineModel(): MachineModel {
        return MachineModel(machine.model.cpu, MemoryUnit("Generic", "Generic", 3200.0, memorySize))
    }

    private var localLastReport = clock.millis()
    private var localUptime = 0L
    private var localDowntime = 0L
    private var localBootTime: Instant? = null
    private val localCpuLimit = machine.model.cpu.totalCapacity

    /**
     * Helper function to track the uptime of a machine.
     */
    private fun updateUptime() {
        val now = clock.millis()
        val duration = now - localLastReport
        localLastReport = now

        if (localState == HostState.UP) {
            localUptime += duration
        } else if (localState == HostState.ERROR) {
            // Only increment downtime if the machine is in a failure state
            localDowntime += duration
        }

        val guests = localGuests
        for (i in guests.indices) {
            guests[i].updateUptime()
        }
    }
}
