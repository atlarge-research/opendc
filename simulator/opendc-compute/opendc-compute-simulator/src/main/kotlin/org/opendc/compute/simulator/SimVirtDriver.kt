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

package org.opendc.compute.simulator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.opendc.compute.core.*
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.image.Image
import org.opendc.compute.core.virt.HypervisorEvent
import org.opendc.compute.core.virt.driver.InsufficientMemoryOnServerException
import org.opendc.compute.core.virt.driver.VirtDriver
import org.opendc.core.services.ServiceRegistry
import org.opendc.simulator.compute.*
import org.opendc.simulator.compute.interference.IMAGE_PERF_INTERFERENCE_MODEL
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.workload.SimResourceCommand
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.utils.flow.EventFlow
import java.util.*

/**
 * A [VirtDriver] that is simulates virtual machines on a physical machine using [SimHypervisor].
 */
public class SimVirtDriver(private val coroutineScope: CoroutineScope, hypervisor: SimHypervisorProvider) : VirtDriver, SimWorkload {
    /**
     * The execution context in which the [VirtDriver] runs.
     */
    private lateinit var ctx: ComputeSimExecutionContext

    /**
     * The server hosting this hypervisor.
     */
    public val server: Server
        get() = ctx.server

    /**
     * The [EventFlow] to emit the events.
     */
    internal val eventFlow = EventFlow<HypervisorEvent>()

    override val events: Flow<HypervisorEvent> = eventFlow

    /**
     * Current total memory use of the images on this hypervisor.
     */
    private var availableMemory: Long = 0

    /**
     * The hypervisor to run multiple workloads.
     */
    private val hypervisor = hypervisor.create(
        object : SimHypervisor.Listener {
            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                requestedWork: Long,
                grantedWork: Long,
                overcommittedWork: Long,
                interferedWork: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                eventFlow.emit(
                    HypervisorEvent.SliceFinished(
                        this@SimVirtDriver,
                        requestedWork,
                        grantedWork,
                        overcommittedWork,
                        interferedWork,
                        cpuUsage,
                        cpuDemand,
                        vms.size,
                        ctx.server
                    )
                )
            }
        }
    )

    /**
     * The virtual machines running on the hypervisor.
     */
    private val vms = HashSet<VirtualMachine>()

    override fun canFit(flavor: Flavor): Boolean {
        val sufficientMemory = availableMemory > flavor.memorySize
        val enoughCpus = ctx.machine.cpus.size >= flavor.cpuCount
        val canFit = hypervisor.canFit(flavor.toMachineModel())

        return sufficientMemory && enoughCpus && canFit
    }

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

        val vm = VirtualMachine(server, events, hypervisor.createMachine(flavor.toMachineModel()))
        vms.add(vm)
        vmStarted(vm)
        eventFlow.emit(HypervisorEvent.VmsUpdated(this, vms.size, availableMemory))
        return server
    }

    /**
     * Convert flavor to machine model.
     */
    private fun Flavor.toMachineModel(): SimMachineModel {
        val originalCpu = ctx.machine.cpus[0]
        val processingNode = originalCpu.node.copy(coreCount = cpuCount)
        val processingUnits = (0 until cpuCount).map { originalCpu.copy(id = it, node = processingNode) }
        val memoryUnits = listOf(MemoryUnit("Generic", "Generic", 3200.0, memorySize))

        return SimMachineModel(processingUnits, memoryUnits)
    }

    private fun vmStarted(vm: VirtualMachine) {
        vms.forEach { it ->
            vm.performanceInterferenceModel?.onStart(it.server.image.name)
        }
    }

    private fun vmStopped(vm: VirtualMachine) {
        vms.forEach { it ->
            vm.performanceInterferenceModel?.onStop(it.server.image.name)
        }
    }

    /**
     * A virtual machine instance that the driver manages.
     */
    private inner class VirtualMachine(server: Server, val events: EventFlow<ServerEvent>, val machine: SimMachine) {
        val performanceInterferenceModel: PerformanceInterferenceModel? = server.image.tags[IMAGE_PERF_INTERFERENCE_MODEL] as? PerformanceInterferenceModel?

        val job = coroutineScope.launch {
            val delegate = server.image.tags["workload"] as SimWorkload
            // Wrap the workload to pass in a ComputeSimExecutionContext
            val workload = object : SimWorkload {
                lateinit var wrappedCtx: ComputeSimExecutionContext

                override fun onStart(ctx: SimExecutionContext) {
                    wrappedCtx = object : ComputeSimExecutionContext, SimExecutionContext by ctx {
                        override val server: Server
                            get() = server

                        override fun toString(): String = "WrappedSimExecutionContext"
                    }

                    delegate.onStart(wrappedCtx)
                }

                override fun onStart(ctx: SimExecutionContext, cpu: Int): SimResourceCommand {
                    return delegate.onStart(wrappedCtx, cpu)
                }

                override fun onNext(ctx: SimExecutionContext, cpu: Int, remainingWork: Double): SimResourceCommand {
                    return delegate.onNext(wrappedCtx, cpu, remainingWork)
                }

                override fun toString(): String = "SimWorkloadWrapper(delegate=$delegate)"
            }

            delay(1) // TODO Introduce boot time
            init()
            try {
                machine.run(workload)
                exit(null)
            } catch (cause: Throwable) {
                exit(cause)
            } finally {
                machine.close()
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
                if (cause == null)
                    ServerState.SHUTOFF
                else
                    ServerState.ERROR
            server = server.copy(state = serverState)
            availableMemory += server.flavor.memorySize
            vms.remove(this)
            vmStopped(this)
            eventFlow.emit(HypervisorEvent.VmsUpdated(this@SimVirtDriver, vms.size, availableMemory))
            events.close()
        }
    }

    override fun onStart(ctx: SimExecutionContext) {
        this.ctx = ctx as ComputeSimExecutionContext
        this.availableMemory = ctx.machine.memory.map { it.size }.sum()
        this.hypervisor.onStart(ctx)
    }

    override fun onStart(ctx: SimExecutionContext, cpu: Int): SimResourceCommand {
        return hypervisor.onStart(ctx, cpu)
    }

    override fun onNext(ctx: SimExecutionContext, cpu: Int, remainingWork: Double): SimResourceCommand {
        return hypervisor.onNext(ctx, cpu, remainingWork)
    }
}
