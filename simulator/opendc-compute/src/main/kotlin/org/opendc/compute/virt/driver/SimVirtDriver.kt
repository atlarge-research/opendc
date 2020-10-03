/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.compute.virt.driver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.opendc.compute.core.*
import org.opendc.compute.core.execution.ComputeSimExecutionContext
import org.opendc.compute.core.execution.ShutdownException
import org.opendc.compute.core.image.Image
import org.opendc.compute.core.image.SimWorkloadImage
import org.opendc.compute.virt.HypervisorEvent
import org.opendc.core.services.ServiceRegistry
import org.opendc.simulator.compute.SimExecutionContext
import org.opendc.simulator.compute.SimHypervisor
import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.utils.flow.EventFlow
import java.time.Clock
import java.util.*

/**
 * A [VirtDriver] that is simulates virtual machines on a physical machine using [SimHypervisor].
 */
public class SimVirtDriver(
    private val coroutineScope: CoroutineScope,
    clock: Clock,
    private val ctx: SimExecutionContext
) : VirtDriver {

    /**
     * The [EventFlow] to emit the events.
     */
    internal val eventFlow = EventFlow<HypervisorEvent>()

    override val events: Flow<HypervisorEvent> = eventFlow

    /**
     * Current total memory use of the images on this hypervisor.
     */
    private var availableMemory: Long = ctx.machine.memory.map { it.size }.sum()

    /**
     * The hypervisor to run multiple workloads.
     */
    private val hypervisor = SimHypervisor(
        coroutineScope,
        clock,
        object : SimHypervisor.Listener {
            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                requestedBurst: Long,
                grantedBurst: Long,
                overcommissionedBurst: Long,
                interferedBurst: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                eventFlow.emit(
                    HypervisorEvent.SliceFinished(
                        this@SimVirtDriver,
                        requestedBurst,
                        grantedBurst,
                        overcommissionedBurst,
                        interferedBurst,
                        cpuUsage,
                        cpuDemand,
                        vms.size,
                        (ctx as ComputeSimExecutionContext).server
                    )
                )
            }
        }
    )

    /**
     * The virtual machines running on the hypervisor.
     */
    private val vms = HashSet<VirtualMachine>()

    override suspend fun spawn(name: String, image: Image, flavor: Flavor): Server {
        val requiredMemory = flavor.memorySize
        if (availableMemory - requiredMemory < 0) {
            throw InsufficientMemoryOnServerException()
        }
        require(flavor.cpuCount <= ctx.machine.cpus.size) { "Machine does not fit" }

        val events = EventFlow<ServerEvent>()
        val server = Server(
            UUID.randomUUID(),
            name,
            emptyMap(),
            flavor,
            image,
            ServerState.BUILD,
            ServiceRegistry(),
            events
        )
        availableMemory -= requiredMemory
        vms.add(VirtualMachine(server, events, hypervisor.createMachine(ctx.machine)))
        eventFlow.emit(HypervisorEvent.VmsUpdated(this, vms.size, availableMemory))
        return server
    }

    /**
     * A virtual machine instance that the driver manages.
     */
    private inner class VirtualMachine(server: Server, val events: EventFlow<ServerEvent>, machine: SimMachine) {
        val job = coroutineScope.launch {
            val workload = object : SimWorkload {
                override suspend fun run(ctx: SimExecutionContext) {
                    val wrappedCtx = object : ComputeSimExecutionContext, SimExecutionContext by ctx {
                        override val server: Server
                            get() = this@VirtualMachine.server
                    }
                    (server.image as SimWorkloadImage).workload.run(wrappedCtx)
                }
            }

            delay(1) // TODO Introduce boot time
            init()
            try {
                machine.run(workload)
                exit(null)
            } catch (cause: Throwable) {
                exit(cause)
            }
        }

        var server: Server = server
            set(value) {
                if (field.state != value.state) {
                    events.emit(ServerEvent.StateChanged(value, field.state))
                }

                field = value
            }

        private fun init() {
            server = server.copy(state = ServerState.ACTIVE)
        }

        private fun exit(cause: Throwable?) {
            val serverState =
                if (cause == null || (cause is ShutdownException && cause.cause == null))
                    ServerState.SHUTOFF
                else
                    ServerState.ERROR
            server = server.copy(state = serverState)
            availableMemory += server.flavor.memorySize
            vms.remove(this)
            eventFlow.emit(HypervisorEvent.VmsUpdated(this@SimVirtDriver, vms.size, availableMemory))
            events.close()
        }
    }

    public suspend fun run() {
        hypervisor.run(ctx)
    }
}
