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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.Server
import org.opendc.compute.core.ServerEvent
import org.opendc.compute.core.ServerState
import org.opendc.compute.core.image.EmptyImage
import org.opendc.compute.core.image.Image
import org.opendc.compute.core.metal.Node
import org.opendc.compute.core.metal.NodeEvent
import org.opendc.compute.core.metal.NodeState
import org.opendc.compute.core.metal.driver.BareMetalDriver
import org.opendc.compute.simulator.power.api.CpuPowerModel
import org.opendc.compute.simulator.power.api.Powerable
import org.opendc.compute.simulator.power.models.ConstantPowerModel
import org.opendc.core.services.ServiceRegistry
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimExecutionContext
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.workload.SimResourceCommand
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.failures.FailureDomain
import org.opendc.utils.flow.EventFlow
import org.opendc.utils.flow.StateFlow
import java.time.Clock
import java.util.UUID
import kotlin.random.Random

/**
 * A basic implementation of the [BareMetalDriver] that simulates an [Image] running on a bare-metal machine.
 *
 * @param coroutineScope The [CoroutineScope] the driver runs in.
 * @param clock The virtual clock to keep track of time.
 * @param uid The unique identifier of the machine.
 * @param name An optional name of the machine.
 * @param metadata The initial metadata of the node.
 * @param machine The machine model to simulate.
 * @param cpuPowerModel The CPU power model of this machine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class SimBareMetalDriver(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
    uid: UUID,
    name: String,
    metadata: Map<String, Any>,
    machine: SimMachineModel,
    cpuPowerModel: CpuPowerModel = ConstantPowerModel(0.0),
) : BareMetalDriver, FailureDomain, Powerable {
    /**
     * The flavor that corresponds to this machine.
     */
    private val flavor = Flavor(
        machine.cpus.size,
        machine.memory.map { it.size }.sum()
    )

    /**
     * The events of the machine.
     */
    private val events = EventFlow<NodeEvent>()

    /**
     * The machine state.
     */
    private val nodeState =
        StateFlow(Node(uid, name, metadata + ("driver" to this), NodeState.SHUTOFF, EmptyImage, null, events))

    /**
     * The [SimBareMetalMachine] we use to run the workload.
     */
    private val machine = SimBareMetalMachine(coroutineScope, clock, machine)

    override val node: Flow<Node> = nodeState

    override val usage: Flow<Double>
        get() = this.machine.usage

    override val powerDraw: Flow<Double> = cpuPowerModel.getPowerDraw(this)

    /**
     * The internal random instance.
     */
    private val random = Random(uid.leastSignificantBits xor uid.mostSignificantBits)

    /**
     * The [Job] that runs the simulated workload.
     */
    private var job: Job? = null

    /**
     * The event stream to publish to for the server.
     */
    private var serverEvents: EventFlow<ServerEvent>? = null

    override suspend fun init(): Node {
        return nodeState.value
    }

    override suspend fun start(): Node {
        val node = nodeState.value
        if (node.state != NodeState.SHUTOFF) {
            return node
        }

        val events = EventFlow<ServerEvent>()
        serverEvents = events
        val server = Server(
            UUID(random.nextLong(), random.nextLong()),
            node.name,
            emptyMap(),
            flavor,
            node.image,
            ServerState.BUILD,
            ServiceRegistry().put(BareMetalDriver, this@SimBareMetalDriver),
            events
        )

        val delegate = (node.image as SimWorkloadImage).workload
        // Wrap the workload to pass in a ComputeSimExecutionContext
        val workload = object : SimWorkload {
            lateinit var wrappedCtx: ComputeSimExecutionContext

            override fun onStart(ctx: SimExecutionContext) {
                wrappedCtx = object : ComputeSimExecutionContext, SimExecutionContext by ctx {
                    override val server: Server
                        get() = nodeState.value.server!!

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

        job = coroutineScope.launch {
            delay(1) // TODO Introduce boot time
            initMachine()
            try {
                machine.run(workload)
                exitMachine(null)
            } catch (_: CancellationException) {
                // Ignored
            } catch (cause: Throwable) {
                exitMachine(cause)
            }
        }

        setNode(node.copy(state = NodeState.BOOT, server = server))
        return nodeState.value
    }

    private fun initMachine() {
        val server = nodeState.value.server?.copy(state = ServerState.ACTIVE)
        setNode(nodeState.value.copy(state = NodeState.ACTIVE, server = server))
    }

    private fun exitMachine(cause: Throwable?) {
        val newServerState =
            if (cause == null)
                ServerState.SHUTOFF
            else
                ServerState.ERROR
        val newNodeState =
            if (cause == null)
                nodeState.value.state
            else
                NodeState.ERROR
        val server = nodeState.value.server?.copy(state = newServerState)
        setNode(nodeState.value.copy(state = newNodeState, server = server))

        serverEvents?.close()
        serverEvents = null
    }

    override suspend fun stop(): Node {
        val node = nodeState.value
        if (node.state == NodeState.SHUTOFF) {
            return node
        }

        job?.cancelAndJoin()
        setNode(node.copy(state = NodeState.SHUTOFF, server = null))
        return node
    }

    override suspend fun reboot(): Node {
        stop()
        return start()
    }

    override suspend fun setImage(image: Image): Node {
        setNode(nodeState.value.copy(image = image))
        return nodeState.value
    }

    override suspend fun refresh(): Node = nodeState.value

    private fun setNode(value: Node) {
        val field = nodeState.value
        if (field.state != value.state) {
            events.emit(NodeEvent.StateChanged(value, field.state))
        }

        val oldServer = field.server
        val newServer = value.server

        if (oldServer != null && newServer != null && oldServer.state != newServer.state) {
            serverEvents?.emit(ServerEvent.StateChanged(newServer, oldServer.state))
        }

        nodeState.value = value
    }

    override val scope: CoroutineScope
        get() = coroutineScope

    override suspend fun fail() {
        val server = nodeState.value.server?.copy(state = ServerState.ERROR)
        setNode(nodeState.value.copy(state = NodeState.ERROR, server = server))
    }

    override suspend fun recover() {
        val server = nodeState.value.server?.copy(state = ServerState.ACTIVE)
        setNode(nodeState.value.copy(state = NodeState.ACTIVE, server = server))
    }

    override fun toString(): String = "SimBareMetalDriver(node = ${nodeState.value.uid})"
}
