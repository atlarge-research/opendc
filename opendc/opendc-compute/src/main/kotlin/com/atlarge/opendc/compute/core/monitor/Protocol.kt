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

package com.atlarge.opendc.compute.core.monitor

import com.atlarge.odcsim.SendPort
import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Events emitted by a [Server] instance.
 */
public sealed class ServerEvent {
    /**
     * The server that emitted this event.
     */
    public abstract val server: Server

    /**
     * A response sent when the bare metal driver has been initialized.
     */
    public data class StateChanged(public override val server: Server, public val previousState: ServerState) : ServerEvent()
}

/**
 * Serialize the specified [ServerMonitor] instance in order to safely send this object across logical processes.
 */
public suspend fun ServerMonitor.serialize(): ServerMonitor {
    val ctx = processContext
    val input = ctx.open<ServerEvent>()

    ctx.launch {
        val inlet = processContext.listen(input.receive)

        while (isActive) {
            when (val msg = inlet.receive()) {
                is ServerEvent.StateChanged -> onUpdate(msg.server, msg.previousState)
            }
        }
    }

    return object : ServerMonitor {
        private var outlet: SendPort<ServerEvent>? = null

        override suspend fun onUpdate(server: Server, previousState: ServerState) {
            if (outlet == null) {
                outlet = processContext.connect(input.send)
            }

            outlet!!.send(ServerEvent.StateChanged(server, previousState))
        }
    }
}
