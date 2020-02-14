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

package com.atlarge.opendc.compute.metal.service

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.PowerState
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver

/**
 * A very basic implementation of the [ProvisioningService].
 */
public class SimpleProvisioningService : ProvisioningService, ServerMonitor {
    /**
     * The active nodes in this service.
     */
    private val nodes: MutableMap<Node, BareMetalDriver> = mutableMapOf()

    /**
     * The installed monitors.
     */
    private val monitors: MutableMap<Server, ServerMonitor> = mutableMapOf()

    override suspend fun create(driver: BareMetalDriver): Node {
        val node = driver.init(this)
        nodes[node] = driver
        return node
    }

    override suspend fun nodes(): Set<Node> = nodes.keys

    override suspend fun refresh(node: Node): Node {
        return nodes[node]!!.refresh()
    }

    override suspend fun deploy(node: Node, image: Image, monitor: ServerMonitor): Node {
        val driver = nodes[node]!!

        driver.setImage(image)
        val newNode = driver.setPower(PowerState.POWER_ON)
        monitors[newNode.server!!] = monitor
        return newNode
    }

    override suspend fun onUpdate(server: Server, previousState: ServerState) {
        monitors[server]?.onUpdate(server, previousState)
    }
}
