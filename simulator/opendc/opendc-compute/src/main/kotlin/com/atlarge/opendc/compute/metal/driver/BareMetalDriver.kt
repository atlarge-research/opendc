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

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.core.failure.FailureDomain
import com.atlarge.opendc.core.power.Powerable
import com.atlarge.opendc.core.services.AbstractServiceKey
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * A driver interface for the management interface of a bare-metal compute node.
 */
public interface BareMetalDriver : Powerable, FailureDomain {
    /**
     * The [Node] that is controlled by this driver.
     */
    public val node: Flow<Node>

    /**
     * The amount of work done by the machine in percentage with respect to the total amount of processing power
     * available.
     */
    public val usage: Flow<Double>

    /**
     * Initialize the driver.
     */
    public suspend fun init(): Node

    /**
     * Start the bare metal node with the specified boot disk image.
     */
    public suspend fun start(): Node

    /**
     * Stop the bare metal node if it is running.
     */
    public suspend fun stop(): Node

    /**
     * Reboot the bare metal node.
     */
    public suspend fun reboot(): Node

    /**
     * Update the boot disk image of the compute node.
     *
     * Changing the boot disk image of node does not affect it while the node is running. In order to start the new boot
     * disk image, the compute node must be restarted.
     */
    public suspend fun setImage(image: Image): Node

    /**
     * Obtain the state of the compute node.
     */
    public suspend fun refresh(): Node

    /**
     * A key that allows access to the [BareMetalDriver] instance from a [Server] that runs on the bare-metal machine.
     */
    companion object Key : AbstractServiceKey<BareMetalDriver>(UUID.randomUUID(), "bare-metal:driver")
}
