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

package org.opendc.compute.simulator

import kotlinx.coroutines.*
import org.opendc.compute.api.Image
import org.opendc.compute.service.driver.Host
import org.opendc.metal.Node
import org.opendc.metal.service.ProvisioningService
import org.opendc.simulator.compute.SimHypervisorProvider
import kotlin.coroutines.CoroutineContext

/**
 * A helper class to provision [SimHost]s on top of bare-metal machines using the [ProvisioningService].
 *
 * @param context The [CoroutineContext] to use.
 * @param metal The [ProvisioningService] to use.
 * @param hypervisor The type of hypervisor to use.
 */
public class SimHostProvisioner(
    private val context: CoroutineContext,
    private val metal: ProvisioningService,
    private val hypervisor: SimHypervisorProvider
) : AutoCloseable {
    /**
     * The [CoroutineScope] of the service bounded by the lifecycle of the service.
     */
    private val scope = CoroutineScope(context)

    /**
     * Provision all machines with a host.
     */
    public suspend fun provisionAll(): List<Host> = coroutineScope {
        metal.nodes().map { node -> async { provision(node) } }.awaitAll()
    }

    /**
     * Provision the specified [Node].
     */
    public suspend fun provision(node: Node): Host = coroutineScope {
        val host = SimHost(node, scope, hypervisor)
        metal.deploy(node, Image(node.uid, node.name, mapOf("workload" to host)))
        host
    }

    override fun close() {
        scope.cancel()
    }
}
