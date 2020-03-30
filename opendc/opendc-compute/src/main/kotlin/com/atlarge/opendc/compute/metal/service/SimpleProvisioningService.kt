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

import com.atlarge.odcsim.Domain
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * A very basic implementation of the [ProvisioningService].
 */
public class SimpleProvisioningService(val domain: Domain) : ProvisioningService {
    /**
     * The active nodes in this service.
     */
    private val nodes: MutableMap<Node, BareMetalDriver> = mutableMapOf()

    override suspend fun create(driver: BareMetalDriver): Node = withContext(domain.coroutineContext) {
        val node = driver.init()
        nodes[node] = driver
        return@withContext node
    }

    override suspend fun nodes(): Set<Node> = withContext(domain.coroutineContext) { nodes.keys }

    override suspend fun refresh(node: Node): Node = withContext(domain.coroutineContext) {
        return@withContext nodes[node]!!.refresh()
    }

    override suspend fun deploy(node: Node, image: Image): Node = withContext(domain.coroutineContext) {
        val driver = nodes[node]!!
        driver.setImage(image)
        val newNode = driver.reboot()
        return@withContext newNode
    }

    override suspend fun stop(node: Node): Node = withContext(domain.coroutineContext) {
        val driver = nodes[node]!!
        try {
            driver.stop()
        } catch (e: CancellationException) {
            node
        }
    }
}
