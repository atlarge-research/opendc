/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.compute.simulator.internal

import mu.KotlinLogging
import org.opendc.compute.api.Task
import org.opendc.compute.api.TaskState
import org.opendc.compute.service.driver.telemetry.GuestCpuStats
import org.opendc.compute.service.driver.telemetry.GuestSystemStats
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.simulator.SimWorkloadMapper
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.kernel.SimHypervisor
import org.opendc.simulator.compute.workload.SimWorkload
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

/**
 * A virtual machine instance that is managed by a [SimHost].
 */
internal class Guest(
    private val clock: InstantSource,
    val host: SimHost,
    private val hypervisor: SimHypervisor,
    private val mapper: SimWorkloadMapper,
    private val listener: GuestListener,
    val task: Task,
    val machine: SimHypervisor.SimVirtualMachine,
) {
    /**
     * The state of the [Guest].
     *
     * [TaskState.PROVISIONING] is an invalid value for a guest, since it applies before the host is selected for
     * a task.
     */
    var state: TaskState = TaskState.TERMINATED
        private set

    /**
     * Start the guest.
     */
    fun start() {
        when (state) {
            TaskState.TERMINATED, TaskState.ERROR -> {
                LOGGER.info { "User requested to start task ${task.uid}" }
                doStart()
            }
            TaskState.RUNNING -> return
            TaskState.DELETED -> {
                LOGGER.warn { "User tried to start deleted task" }
                throw IllegalArgumentException("Task is deleted")
            }
            else -> assert(false) { "Invalid state transition" }
        }
    }

    /**
     * Stop the guest.
     */
    fun stop() {
        when (state) {
            TaskState.RUNNING -> doStop(TaskState.TERMINATED)
            TaskState.ERROR -> doRecover()
            TaskState.TERMINATED, TaskState.DELETED -> return
            else -> assert(false) { "Invalid state transition" }
        }
    }

    /**
     * Delete the guest.
     *
     * This operation will stop the guest if it is running on the host and remove all resources associated with the
     * guest.
     */
    fun delete() {
        stop()

        state = TaskState.DELETED
        hypervisor.removeMachine(machine)
    }

    /**
     * Fail the guest if it is active.
     *
     * This operation forcibly stops the guest and puts the task into an error state.
     */
    fun fail() {
        if (state != TaskState.RUNNING) {
            return
        }

        doStop(TaskState.ERROR)
    }

    /**
     * Recover the guest if it is in an error state.
     */
    fun recover() {
        if (state != TaskState.ERROR) {
            return
        }

        doStart()
    }

    /**
     * Obtain the system statistics of this guest.
     */
    fun getSystemStats(): GuestSystemStats {
        updateUptime()

        return GuestSystemStats(
            Duration.ofMillis(localUptime),
            Duration.ofMillis(localDowntime),
            localBootTime,
        )
    }

    /**
     * Obtain the CPU statistics of this guest.
     */
    fun getCpuStats(): GuestCpuStats {
        val counters = machine.counters
        counters.sync()

        return GuestCpuStats(
            counters.cpuActiveTime / 1000L,
            counters.cpuIdleTime / 1000L,
            counters.cpuStealTime / 1000L,
            counters.cpuLostTime / 1000L,
            machine.cpuCapacity,
            machine.cpuUsage,
            machine.cpuUsage / localCpuLimit,
        )
    }

    /**
     * The [SimMachineContext] representing the current active virtual machine instance or `null` if no virtual machine
     * is active.
     */
    private var ctx: SimMachineContext? = null

    /**
     * Launch the guest on the simulated
     */
    private fun doStart() {
        assert(ctx == null) { "Concurrent job running" }

        onStart()

        val workload: SimWorkload = mapper.createWorkload(task)
        workload.setOffset(clock.millis())
        val meta = mapOf("driver" to host, "task" to task) + task.meta
        ctx =
            machine.startWorkload(workload, meta) { cause ->
                onStop(if (cause != null) TaskState.ERROR else TaskState.TERMINATED)
                ctx = null
            }
    }

    /**
     * Attempt to stop the task and put it into [target] state.
     */
    private fun doStop(target: TaskState) {
        assert(ctx != null) { "Invalid job state" }
        val ctx = ctx ?: return
        if (target == TaskState.ERROR) {
            ctx.shutdown(Exception("Stopped because of ERROR"))
        } else {
            ctx.shutdown()
        }

        state = target
    }

    /**
     * Attempt to recover from an error state.
     */
    private fun doRecover() {
        state = TaskState.TERMINATED
    }

    /**
     * This method is invoked when the guest was started on the host and has booted into a running state.
     */
    private fun onStart() {
        localBootTime = clock.instant()
        state = TaskState.RUNNING
        listener.onStart(this)
    }

    /**
     * This method is invoked when the guest stopped.
     */
    private fun onStop(target: TaskState) {
        updateUptime()

        state = target
        listener.onStop(this)
    }

    private var localUptime = 0L
    private var localDowntime = 0L
    private var localLastReport = clock.millis()
    private var localBootTime: Instant? = null
    private val localCpuLimit = machine.model.cpu.totalCapacity

    /**
     * Helper function to track the uptime and downtime of the guest.
     */
    fun updateUptime() {
        val now = clock.millis()
        val duration = now - localLastReport
        localLastReport = now

        if (state == TaskState.RUNNING) {
            localUptime += duration
        } else if (state == TaskState.ERROR) {
            localDowntime += duration
        }
    }

    private companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }
}
