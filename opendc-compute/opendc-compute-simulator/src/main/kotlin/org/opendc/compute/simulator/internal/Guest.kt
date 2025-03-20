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
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.GuestCpuStats
import org.opendc.compute.simulator.telemetry.GuestSystemStats
import org.opendc.simulator.compute.machine.SimMachine
import org.opendc.simulator.compute.workload.ChainWorkload
import org.opendc.simulator.compute.workload.VirtualMachine
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

/**
 * A virtual machine instance that is managed by a [SimHost].
 */
public class Guest(
    private val clock: InstantSource,
    public val host: SimHost,
    private val listener: GuestListener,
    public val task: ServiceTask,
    public val simMachine: SimMachine,
) {
    /**
     * The state of the [Guest].
     *
     * [TaskState.PROVISIONING] is an invalid value for a guest, since it applies before the host is selected for
     * a task.
     */
    public var state: TaskState = TaskState.CREATED
        private set

    /**
     * The [VirtualMachine] on which the task is currently running
     */
    public var virtualMachine: VirtualMachine? = null

    private var uptime = 0L
    private var downtime = 0L
    private var lastReport = clock.millis()
    private var bootTime: Instant? = null
    private val cpuLimit = simMachine.cpu.cpuModel.totalCapacity

    /**
     * Start the guest.
     */
    public fun start() {
        when (state) {
            TaskState.CREATED, TaskState.FAILED -> {
                LOGGER.info { "User requested to start task ${task.uid}" }
                doStart()
            }
            TaskState.RUNNING -> return
            TaskState.COMPLETED, TaskState.TERMINATED -> {
                LOGGER.warn { "User tried to start a finished task" }
                throw IllegalArgumentException("Task is already finished")
            }
            else -> assert(false) { "Invalid state transition" }
        }
    }

    /**
     * Launch the guest on the simulated Virtual machine
     */
    private fun doStart() {
        assert(virtualMachine == null) { "Concurrent job is already running" }

        onStart()

        // TODO: This is not being used at the moment
//        val bootworkload =
//            TraceWorkload(s
//                ArrayList(
//                    listOf(
//                        TraceFragment(
//                            1000000L,
//                            100000.0,
//                            1,
//                        ),
//                    ),
//                ),
//                0,
//                0,
//                0.0,
//                NoDelayScaling(),
//            )

        if (task.workload is ChainWorkload) {
            virtualMachine =
                simMachine.startWorkload(task.workload as ChainWorkload) { cause ->
                    onStop(if (cause != null) TaskState.FAILED else TaskState.COMPLETED)
                }
        } else {
            val newChainWorkload =
                ChainWorkload(
                    ArrayList(listOf(task.workload)),
                    task.workload.checkpointInterval(),
                    task.workload.checkpointDuration(),
                    task.workload.checkpointIntervalScaling(),
                )

            virtualMachine =
                simMachine.startWorkload(newChainWorkload) { cause ->
                    onStop(if (cause != null) TaskState.FAILED else TaskState.COMPLETED)
                }
        }
    }

    /**
     * This method is invoked when the guest was started on the host and has booted into a running state.
     */
    private fun onStart() {
        bootTime = clock.instant()
        state = TaskState.RUNNING
        listener.onStart(this)
    }

    /**
     * Stop the guest.
     */
    public fun stop() {
        when (state) {
            TaskState.RUNNING -> doStop(TaskState.COMPLETED)
            TaskState.FAILED -> state = TaskState.TERMINATED
            TaskState.COMPLETED, TaskState.TERMINATED -> return
            else -> assert(false) { "Invalid state transition" }
        }
    }

    /**
     * Attempt to stop the task and put it into [target] state.
     */
    private fun doStop(target: TaskState) {
        assert(virtualMachine != null) { "Invalid job state" }
        val virtualMachine = this.virtualMachine ?: return
        if (target == TaskState.FAILED) {
            virtualMachine.stopWorkload(Exception("Task has failed"))
        } else {
            virtualMachine.stopWorkload()
        }

        this.virtualMachine = null

        this.state = target
    }

    /**
     * This method is invoked when the guest stopped.
     */
    private fun onStop(target: TaskState) {
        updateUptime()

        state = target
        listener.onStop(this)
    }

    /**
     * Delete the guest.
     *
     * This operation will stop the guest if it is running on the host and remove all resources associated with the
     * guest.
     */
    public fun delete() {
        stop()

        state = TaskState.FAILED
    }

    /**
     * Fail the guest if it is active.
     *
     * This operation forcibly stops the guest and puts the task into an error state.
     */
    public fun fail() {
        if (state != TaskState.RUNNING) {
            return
        }

        doStop(TaskState.FAILED)
    }

    /**
     * Recover the guest if it is in an error state.
     */
    public fun recover() {
        if (state != TaskState.FAILED) {
            return
        }

        doStart()
    }

    /**
     * Obtain the system statistics of this guest.
     */
    public fun getSystemStats(): GuestSystemStats {
        updateUptime()

        return GuestSystemStats(
            Duration.ofMillis(uptime),
            Duration.ofMillis(downtime),
            bootTime,
        )
    }

    /**
     * Obtain the CPU statistics of this guest.
     */
    public fun getCpuStats(): GuestCpuStats {
        virtualMachine!!.updateCounters(this.clock.millis())
        val counters = virtualMachine!!.performanceCounters

        return GuestCpuStats(
            counters.cpuActiveTime / 1000L,
            counters.cpuIdleTime / 1000L,
            counters.cpuStealTime / 1000L,
            counters.cpuLostTime / 1000L,
            counters.cpuCapacity,
            counters.cpuSupply,
            counters.cpuDemand,
            counters.cpuSupply / cpuLimit,
        )
    }

    /**
     * Helper function to track the uptime and downtime of the guest.
     */
    public fun updateUptime() {
        val now = clock.millis()
        val duration = now - lastReport
        lastReport = now

        if (state == TaskState.RUNNING) {
            uptime += duration
        } else if (state == TaskState.FAILED) {
            downtime += duration
        }
    }

    private companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }
}
