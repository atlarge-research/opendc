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

import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.execution.serialize
import com.atlarge.opendc.compute.core.image.EmptyImage
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.PowerState
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.delay

/**
 * A implementation of the [BareMetalDriver] that simulates an [Image] running on a bare-metal machine, but performs
 * not actual computation.
 *
 * @param uid The unique identifier of the machine.
 * @param name An optional name of the machine.
 * @param flavor The hardware configuration of the machine.
 */
public class FakeBareMetalDriver(
    uid: UUID,
    name: String,
    private val flavor: Flavor
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
            PowerState.POWER_OFF to PowerState.POWER_ON -> Server(node.uid, node.name, flavor, node.image, ServerState.BUILD)
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
        val serverCtx = this.serverCtx.serialize()

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

    private val serverCtx = object : ServerManagementContext {
        override var server: Server
            get() = node.server!!
            set(value) {
                node = node.copy(server = value)
            }

        override suspend fun init() {
            val previousState = server.state
            server = server.copy(state = ServerState.ACTIVE)
            monitor.onUpdate(server, previousState)
        }

        override suspend fun exit(cause: Throwable?) {
            val previousState = server.state
            val state = if (cause == null) ServerState.SHUTOFF else ServerState.ERROR
            server = server.copy(state = state)
            monitor.onUpdate(server, previousState)
        }

        override suspend fun run(req: LongArray) {
            // TODO Properly implement this for multiple CPUs
            val time = max(0, req.max() ?: 0) * flavor.cpus[0].clockRate
            delay(time.toLong())
        }
    }
}
