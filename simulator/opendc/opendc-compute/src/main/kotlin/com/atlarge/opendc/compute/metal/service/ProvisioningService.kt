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

import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.core.services.AbstractServiceKey
import java.util.UUID

/**
 * A cloud platform service for provisioning bare-metal compute nodes on the platform.
 */
public interface ProvisioningService {
    /**
     * Create a new bare-metal compute node.
     */
    public suspend fun create(driver: BareMetalDriver): Node

    /**
     * Obtain the available nodes.
     */
    public suspend fun nodes(): Set<Node>

    /**
     * Refresh the state of a compute node.
     */
    public suspend fun refresh(node: Node): Node

    /**
     * Deploy the specified [Image] on a compute node.
     */
    public suspend fun deploy(node: Node, image: Image): Node

    /**
     * Stop the specified [Node] .
     */
    public suspend fun stop(node: Node): Node

    /**
     * The service key of this service.
     */
    companion object Key : AbstractServiceKey<ProvisioningService>(UUID.randomUUID(), "provisioner")
}
