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

package com.atlarge.opendc.compute.virt.driver.hypervisor

import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ProcessorContext
import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * A [VirtDriver] that is backed by a simple hypervisor implementation.
 */
class HypervisorVirtDriver(
    private val hostContext: ServerContext,
    private val scheduler: VmScheduler
) : VirtDriver {
    /**
     * A set for tracking the VM context objects.
     */
    internal val vms: MutableSet<VmServerContext> = mutableSetOf()

    /**
     * Current total memory use of the images on this hypervisor.
     */
    private var memoryAvailable: Long = hostContext.server.flavor.memorySize

    override suspend fun spawn(image: Image, monitor: ServerMonitor, flavor: Flavor): Server {
        val requiredMemory = flavor.memorySize
        if (memoryAvailable - requiredMemory < 0) {
            throw InsufficientMemoryOnServerException()
        }

        val server = Server(UUID.randomUUID(), "<unnamed>", emptyMap(), flavor, image, ServerState.BUILD)
        memoryAvailable -= requiredMemory
        vms.add(VmServerContext(server, monitor, processContext))
        return server
    }

    override suspend fun getNumberOfSpawnedImages(): Int {
        return vms.size
    }

    internal inner class VmServerContext(
        override var server: Server,
        val monitor: ServerMonitor,
        ctx: ProcessContext
    ) : ServerManagementContext {
        private var initialized: Boolean = false

        internal val job: Job = ctx.launch {
            init()
            try {
                server.image(this@VmServerContext)
                exit()
            } catch (cause: Throwable) {
                exit(cause)
            }
        }

        override val cpus: List<ProcessorContext> = scheduler.createVirtualCpus(server.flavor)

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
            memoryAvailable += server.flavor.memorySize
            monitor.onUpdate(server, previousState)
            initialized = false
        }
    }
}
