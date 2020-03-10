/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.metal.driver

import com.atlarge.odcsim.Domain
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.MemoryUnit
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.image.EmptyImage
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.PowerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.withContext

/**
 * A basic implementation of the [BareMetalDriver] that simulates an [Image] running on a bare-metal machine.
 *
 * @param uid The unique identifier of the machine.
 * @param name An optional name of the machine.
 * @param cpus The CPUs available to the bare metal machine.
 * @param memoryUnits The memory units in this machine.
 * @param domain The simulation domain the driver runs in.
 */
public class SimpleBareMetalDriver(
    uid: UUID,
    name: String,
    val cpus: List<ProcessingUnit>,
    val memoryUnits: List<MemoryUnit>,
    private val domain: Domain
) : BareMetalDriver {
    /**
     * The monitor to use.
     */
    private lateinit var monitor: ServerMonitor

    /**
     * The machine state.
     */
    private var node: Node = Node(uid, name, PowerState.POWER_OFF, EmptyImage, null)

    /**
     * The flavor that corresponds to this machine.
     */
    private val flavor = Flavor(cpus.size, memoryUnits.map { it.size }.sum())

    /**
     * The job that is running the image.
     */
    private var job: Job? = null

    /**
     * The channel containing the load of the server.
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    private val loadChannel = BroadcastChannel<Double>(Channel.CONFLATED)

    @UseExperimental(FlowPreview::class)
    override val load: Flow<Double> = loadChannel.asFlow()

    init {
        loadChannel.offer(0.0)
    }

    override suspend fun init(monitor: ServerMonitor): Node = withContext(domain.coroutineContext) {
        this@SimpleBareMetalDriver.monitor = monitor
        return@withContext node
    }

    override suspend fun setPower(powerState: PowerState): Node = withContext(domain.coroutineContext) {
        val previousPowerState = node.powerState
        val server = when (node.powerState to powerState) {
            PowerState.POWER_OFF to PowerState.POWER_OFF -> null
            PowerState.POWER_OFF to PowerState.POWER_ON -> Server(
                UUID.randomUUID(),
                node.name,
                emptyMap(),
                flavor,
                node.image,
                ServerState.BUILD
            )
            PowerState.POWER_ON to PowerState.POWER_OFF -> null // TODO Terminate existing image
            PowerState.POWER_ON to PowerState.POWER_ON -> node.server
            else -> throw IllegalStateException()
        }
        node = node.copy(powerState = powerState, server = server)

        if (powerState != previousPowerState && server != null) {
            launch()
        }

        return@withContext node
    }

    override suspend fun setImage(image: Image): Node = withContext(domain.coroutineContext) {
        node = node.copy(image = image)
        return@withContext node
    }

    override suspend fun refresh(): Node = withContext(domain.coroutineContext) { node }

    /**
     * Launch the server image on the machine.
     */
    private suspend fun launch() {
        val serverContext = serverCtx

        job = domain.launch {
            serverContext.init()
            try {
                node.server!!.image(serverContext)
                serverContext.exit()
            } catch (cause: Throwable) {
                serverContext.exit(cause)
            }
        }
    }

    private val serverCtx = object : ServerManagementContext {
        private var initialized: Boolean = false

        override val cpus: List<ProcessingUnit> = this@SimpleBareMetalDriver.cpus

        override var server: Server
            get() = node.server!!
            set(value) {
                node = node.copy(server = value)
            }

        override suspend fun init() {
            if (initialized) {
                throw IllegalStateException()
            }

            val previousState = server.state
            server = server.copy(state = ServerState.ACTIVE)
            monitor.onUpdate(server, previousState)
            initialized = true
        }

        override suspend fun exit(cause: Throwable?) {
            val previousState = server.state
            val state = if (cause == null) ServerState.SHUTOFF else ServerState.ERROR
            server = server.copy(state = state)
            initialized = false
            domain.launch { monitor.onUpdate(server, previousState) }
        }

        private var flush: Job? = null

        override suspend fun run(burst: LongArray, limit: DoubleArray, deadline: Long) {
            require(burst.size == limit.size) { "Array dimensions do not match" }

            // If run is called in at the same timestamp as the previous call, cancel the load flush
            flush?.cancel()
            flush = null

            val start = simulationContext.clock.millis()
            var duration = max(0, deadline - start)
            var load = 0.0

            // Determine the duration of the first CPU to finish
            for (i in 0 until min(cpus.size, burst.size)) {
                val cpu = cpus[i]
                val usage = min(limit[i], cpu.frequency) * 1_000_000 // Usage from MHz to Hz
                val cpuDuration = ceil(burst[i] / usage * 1000).toLong() // Convert from seconds to milliseconds

                load += usage / cpu.frequency

                if (cpuDuration != 0L) { // We only wait for processor cores with a non-zero burst
                    duration = min(duration, cpuDuration)
                }
            }

            loadChannel.offer(load)

            try {
                delay(duration)
            } catch (_: CancellationException) {
                // On cancellation, we compute and return the remaining burst
            }
            val end = simulationContext.clock.millis()

            // Flush the load if the do not receive a new run call for the same timestamp
            flush = domain.launch {
                delay(1)
                loadChannel.offer(0.0)
            }
            flush!!.invokeOnCompletion {
                flush = null
            }

            // Write back the remaining burst time
            for (i in 0 until min(cpus.size, burst.size)) {
                val usage = min(limit[i], cpus[i].frequency) * 1_000_000
                val granted = ceil((end - start) / 1000.0 * usage).toLong()
                burst[i] = max(0, burst[i] - granted)
            }
        }
    }
}
