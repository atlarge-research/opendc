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

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.telemetry.GuestCpuStats
import org.opendc.compute.service.driver.telemetry.GuestSystemStats
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.simulator.SimWorkloadMapper
import org.opendc.simulator.compute.kernel.SimHypervisor
import org.opendc.simulator.compute.kernel.SimVirtualMachine
import org.opendc.simulator.compute.runWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * A virtual machine instance that is managed by a [SimHost].
 */
internal class Guest(
    context: CoroutineContext,
    private val clock: Clock,
    val host: SimHost,
    private val hypervisor: SimHypervisor,
    private val mapper: SimWorkloadMapper,
    private val listener: GuestListener,
    val server: Server,
    val machine: SimVirtualMachine
) {
    /**
     * The [CoroutineScope] of the guest.
     */
    private val scope: CoroutineScope = CoroutineScope(context + Job())

    /**
     * The logger instance of this guest.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The state of the [Guest].
     *
     * [ServerState.PROVISIONING] is an invalid value for a guest, since it applies before the host is selected for
     * a server.
     */
    var state: ServerState = ServerState.TERMINATED

    /**
     * Start the guest.
     */
    suspend fun start() {
        when (state) {
            ServerState.TERMINATED, ServerState.ERROR -> {
                logger.info { "User requested to start server ${server.uid}" }
                doStart()
            }
            ServerState.RUNNING -> return
            ServerState.DELETED -> {
                logger.warn { "User tried to start deleted server" }
                throw IllegalArgumentException("Server is deleted")
            }
            else -> assert(false) { "Invalid state transition" }
        }
    }

    /**
     * Stop the guest.
     */
    suspend fun stop() {
        when (state) {
            ServerState.RUNNING -> doStop(ServerState.TERMINATED)
            ServerState.ERROR -> doRecover()
            ServerState.TERMINATED, ServerState.DELETED -> return
            else -> assert(false) { "Invalid state transition" }
        }
    }

    /**
     * Delete the guest.
     *
     * This operation will stop the guest if it is running on the host and remove all resources associated with the
     * guest.
     */
    suspend fun delete() {
        stop()

        state = ServerState.DELETED
        hypervisor.removeMachine(machine)
        scope.cancel()
    }

    /**
     * Fail the guest if it is active.
     *
     * This operation forcibly stops the guest and puts the server into an error state.
     */
    suspend fun fail() {
        if (state != ServerState.RUNNING) {
            return
        }

        doStop(ServerState.ERROR)
    }

    /**
     * Recover the guest if it is in an error state.
     */
    suspend fun recover() {
        if (state != ServerState.ERROR) {
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
            Duration.ofMillis(_uptime),
            Duration.ofMillis(_downtime),
            _bootTime
        )
    }

    /**
     * Obtain the CPU statistics of this guest.
     */
    fun getCpuStats(): GuestCpuStats {
        val counters = machine.counters
        counters.flush()

        return GuestCpuStats(
            counters.cpuActiveTime / 1000L,
            counters.cpuIdleTime / 1000L,
            counters.cpuStealTime / 1000L,
            counters.cpuLostTime / 1000L,
            machine.cpuCapacity,
            machine.cpuUsage,
            machine.cpuUsage / _cpuLimit
        )
    }

    /**
     * The [Job] representing the current active virtual machine instance or `null` if no virtual machine is active.
     */
    private var job: Job? = null

    /**
     * Launch the guest on the simulated
     */
    private suspend fun doStart() {
        assert(job == null) { "Concurrent job running" }
        val workload = mapper.createWorkload(server)

        val job = scope.launch { runMachine(workload) }
        this.job = job

        state = ServerState.RUNNING
        onStart()

        job.invokeOnCompletion { cause ->
            this.job = null
            onStop(if (cause != null && cause !is CancellationException) ServerState.ERROR else ServerState.TERMINATED)
        }
    }

    /**
     * Attempt to stop the server and put it into [target] state.
     */
    private suspend fun doStop(target: ServerState) {
        assert(job != null) { "Invalid job state" }
        val job = job ?: return
        job.cancel()
        job.join()

        state = target
    }

    /**
     * Attempt to recover from an error state.
     */
    private fun doRecover() {
        state = ServerState.TERMINATED
    }

    /**
     * Converge the process that models the virtual machine lifecycle as a coroutine.
     */
    private suspend fun runMachine(workload: SimWorkload) {
        delay(1) // TODO Introduce model for boot time
        machine.runWorkload(workload, mapOf("driver" to host, "server" to server))
    }

    /**
     * This method is invoked when the guest was started on the host and has booted into a running state.
     */
    private fun onStart() {
        _bootTime = clock.instant()
        state = ServerState.RUNNING
        listener.onStart(this)
    }

    /**
     * This method is invoked when the guest stopped.
     */
    private fun onStop(target: ServerState) {
        updateUptime()

        state = target
        listener.onStop(this)
    }

    private var _uptime = 0L
    private var _downtime = 0L
    private var _lastReport = clock.millis()
    private var _bootTime: Instant? = null
    private val _cpuLimit = machine.model.cpus.sumOf { it.frequency }

    /**
     * Helper function to track the uptime and downtime of the guest.
     */
    fun updateUptime() {
        val now = clock.millis()
        val duration = now - _lastReport
        _lastReport = now

        if (state == ServerState.RUNNING) {
            _uptime += duration
        } else if (state == ServerState.ERROR) {
            _downtime += duration
        }
    }
}
