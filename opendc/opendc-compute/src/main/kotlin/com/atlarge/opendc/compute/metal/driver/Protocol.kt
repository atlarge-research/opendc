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

import com.atlarge.odcsim.ReceivePort
import com.atlarge.odcsim.SendPort
import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.PowerState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Messages that may be sent to the management interface of a bare-metal compute [Node], similar to the
 * [BareMetalDriver] interface.
 */
public sealed class NodeRequest {
    /**
     * Initialize the compute node.
     */
    public data class Initialize(public val monitor: ServerMonitor) : NodeRequest()

    /**
     * Update the power state of the compute node.
     */
    public data class SetPowerState(public val state: PowerState) : NodeRequest()

    /**
     * Update the boot disk image of the compute node.
     */
    public data class SetImage(public val image: Image) : NodeRequest()

    /**
     * Obtain the state of the compute node.
     */
    public object Refresh : NodeRequest()
}

/**
 * Responses emitted by a bare-metal compute [Node].
 */
public sealed class NodeResponse {
    /**
     * The node that sent this response.
     */
    public abstract val node: Node

    /**
     * A response sent when the bare metal driver has been initialized.
     */
    public data class Initialized(public override val node: Node) : NodeResponse()

    /**
     * A response sent to indicate the power state of the node changed.
     */
    public data class PowerStateChanged(public override val node: Node) : NodeResponse()

    /**
     * A response sent to indicate the image of a node was changed.
     */
    public data class ImageChanged(public override val node: Node) : NodeResponse()

    /**
     * A response sent for obtaining the refreshed [Node] instance.
     */
    public data class Refreshed(public override val node: Node) : NodeResponse()
}

/**
 * Serialize the specified [BareMetalDriver] instance in order to safely send this object across logical processes.
 */
public suspend fun BareMetalDriver.serialize(): BareMetalDriver {
    val ctx = processContext
    val input = ctx.open<NodeRequest>()
    val output = ctx.open<NodeResponse>()

    ctx.launch {
        val outlet = processContext.connect(output.send)
        val inlet = processContext.listen(input.receive)

        while (isActive) {
            when (val msg = inlet.receive()) {
                is NodeRequest.Initialize ->
                    outlet.send(NodeResponse.Initialized(init(msg.monitor)))
                is NodeRequest.SetPowerState ->
                    outlet.send(NodeResponse.PowerStateChanged(setPower(msg.state)))
                is NodeRequest.SetImage ->
                    outlet.send(NodeResponse.ImageChanged(setImage(msg.image)))
                is NodeRequest.Refresh ->
                    outlet.send(NodeResponse.Refreshed(refresh()))
            }
        }
    }

    return object : BareMetalDriver {
        private lateinit var inlet: ReceivePort<NodeResponse>
        private lateinit var outlet: SendPort<NodeRequest>

        override suspend fun init(monitor: ServerMonitor): Node {
            outlet = processContext.connect(input.send)
            inlet = processContext.listen(output.receive)

            outlet.send(NodeRequest.Initialize(monitor))
            return (inlet.receive() as NodeResponse.Initialized).node
        }

        override suspend fun setPower(powerState: PowerState): Node {
            outlet.send(NodeRequest.SetPowerState(powerState))
            return (inlet.receive() as NodeResponse.PowerStateChanged).node
        }

        override suspend fun setImage(image: Image): Node {
            outlet.send(NodeRequest.SetImage(image))
            return (inlet.receive() as NodeResponse.ImageChanged).node
        }

        override suspend fun refresh(): Node {
            outlet.send(NodeRequest.Refresh)
            return (inlet.receive() as NodeResponse.Refreshed).node
        }
    }
}
