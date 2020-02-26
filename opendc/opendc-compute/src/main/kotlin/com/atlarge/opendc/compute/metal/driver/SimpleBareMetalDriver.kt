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

import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerFlavor
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ProcessorContext
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.image.EmptyImage
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.PowerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A basic implementation of the [BareMetalDriver] that simulates an [Image] running on a bare-metal machine.
 *
 * @param uid The unique identifier of the machine.
 * @param name An optional name of the machine.
 * @param flavor The hardware configuration of the machine.
 */
public class SimpleBareMetalDriver(
    uid: UUID,
    name: String,
    private val flavor: ServerFlavor
) : BareMetalDriver {
    /**
     * The monitor to use.
     */
    private lateinit var monitor: ServerMonitor

    /**
     * The machine state.
     */
    private var node: Node = Node(uid, name, PowerState.POWER_OFF, EmptyImage, null)

    override suspend fun init(monitor: ServerMonitor): Node {
        this.monitor = monitor
        return node
    }

    override suspend fun setPower(powerState: PowerState): Node {
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

        return node
    }

    override suspend fun setImage(image: Image): Node {
        node = node.copy(image = image)
        return node
    }

    override suspend fun refresh(): Node = node

    /**
     * Launch the server image on the machine.
     */
    private suspend fun launch() {
        val serverCtx = this.serverCtx

        processContext.spawn {
            serverCtx.init()
            try {
                node.server!!.image(serverCtx)
                serverCtx.exit()
            } catch (cause: Throwable) {
                serverCtx.exit(cause)
            }
        }
    }

    private data class ProcessorContextImpl(override val info: ProcessingUnit) : ProcessorContext {
        override suspend fun run(burst: Long, maxUsage: Double, deadline: Long): Long {
            val start = processContext.clock.millis()
            val usage = min(maxUsage, info.clockRate)

            try {
                val duration = min(max(0, deadline - start), ceil(burst / usage).toLong())
                delay(duration)
            } catch (_: CancellationException) {
                // On cancellation, we compute and return the remaining burst
            }
            val end = processContext.clock.millis()
            val granted = ceil((end - start) * usage * 1_000_000).toLong()
            return max(0, burst - granted)
        }
    }

    private val serverCtx = object : ServerManagementContext {
        private var initialized: Boolean = false
        private lateinit var ctx: ProcessContext

        override val cpus: List<ProcessorContextImpl> = flavor.cpus
            .asSequence()
            .flatMap { cpu ->
                generateSequence { ProcessorContextImpl(cpu) }.take(cpu.cores)
            }
            .toList()

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
            ctx = processContext
            initialized = true
        }

        override suspend fun exit(cause: Throwable?) {
            val previousState = server.state
            val state = if (cause == null) ServerState.SHUTOFF else ServerState.ERROR
            server = server.copy(state = state)
            monitor.onUpdate(server, previousState)
            initialized = false
        }
    }
}