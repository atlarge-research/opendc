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

import com.atlarge.odcsim.Domain
import com.atlarge.odcsim.SimulationContext
import com.atlarge.odcsim.flow.EventFlow
import com.atlarge.odcsim.flow.StateFlow
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.MemoryUnit
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.execution.ShutdownException
import com.atlarge.opendc.compute.core.image.EmptyImage
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.NodeEvent
import com.atlarge.opendc.compute.metal.NodeState
import com.atlarge.opendc.compute.metal.power.ConstantPowerModel
import com.atlarge.opendc.core.power.PowerModel
import com.atlarge.opendc.core.services.ServiceKey
import com.atlarge.opendc.core.services.ServiceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectInstance
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.coroutines.ContinuationInterceptor
import kotlin.random.Random

/**
 * A basic implementation of the [BareMetalDriver] that simulates an [Image] running on a bare-metal machine.
 *
 * @param domain The simulation domain the driver runs in.
 * @param uid The unique identifier of the machine.
 * @param name An optional name of the machine.
 * @param metadata The initial metadata of the node.
 * @param cpus The CPUs available to the bare metal machine.
 * @param memoryUnits The memory units in this machine.
 * @param powerModel The power model of this machine.
 */
public class SimpleBareMetalDriver(
    private val domain: Domain,
    uid: UUID,
    name: String,
    metadata: Map<String, Any>,
    val cpus: List<ProcessingUnit>,
    val memoryUnits: List<MemoryUnit>,
    powerModel: PowerModel<SimpleBareMetalDriver> = ConstantPowerModel(
        0.0
    )
) : BareMetalDriver {
    /**
     * The flavor that corresponds to this machine.
     */
    private val flavor = Flavor(cpus.size, memoryUnits.map { it.size }.sum())

    /**
     * The current active server context.
     */
    private var serverContext: BareMetalServerContext? = null

    /**
     * The events of the machine.
     */
    private val events = EventFlow<NodeEvent>()

    /**
     * The flow containing the load of the server.
     */
    private val usageState = StateFlow(0.0)

    /**
     * The machine state.
     */
    private val nodeState = StateFlow(Node(uid, name, metadata + ("driver" to this), NodeState.SHUTOFF, EmptyImage, null, events))

    override val node: Flow<Node> = nodeState

    override val usage: Flow<Double> = usageState

    override val powerDraw: Flow<Double> = powerModel(this)

    /**
     * The internal random instance.
     */
    private val random = Random(uid.leastSignificantBits xor uid.mostSignificantBits)

    override suspend fun init(): Node = withContext(domain.coroutineContext) {
        nodeState.value
    }

    override suspend fun start(): Node = withContext(domain.coroutineContext) {
        val node = nodeState.value
        if (node.state != NodeState.SHUTOFF) {
            return@withContext node
        }

        val events = EventFlow<ServerEvent>()
        val server = Server(
            UUID(random.nextLong(), random.nextLong()),
            node.name,
            emptyMap(),
            flavor,
            node.image,
            ServerState.BUILD,
            ServiceRegistry().put(BareMetalDriver, this@SimpleBareMetalDriver),
            events
        )

        setNode(node.copy(state = NodeState.BOOT, server = server))
        serverContext = BareMetalServerContext(events)
        return@withContext nodeState.value
    }

    override suspend fun stop(): Node = withContext(domain.coroutineContext) {
        val node = nodeState.value
        if (node.state == NodeState.SHUTOFF) {
            return@withContext node
        }

        // We terminate the image running on the machine
        serverContext!!.cancel(fail = false)
        serverContext = null

        setNode(node.copy(state = NodeState.SHUTOFF, server = null))
        return@withContext node
    }

    override suspend fun reboot(): Node = withContext(domain.coroutineContext) {
        stop()
        start()
    }

    override suspend fun setImage(image: Image): Node = withContext(domain.coroutineContext) {
        setNode(nodeState.value.copy(image = image))
        return@withContext nodeState.value
    }

    override suspend fun refresh(): Node = withContext(domain.coroutineContext) { nodeState.value }

    private fun setNode(value: Node) {
        val field = nodeState.value
        if (field.state != value.state) {
            events.emit(NodeEvent.StateChanged(value, field.state))
        }

        if (field.server != null && value.server != null && field.server.state != value.server.state) {
            serverContext!!.events.emit(ServerEvent.StateChanged(value.server, field.server.state))
        }

        nodeState.value = value
    }

    private inner class BareMetalServerContext(val events: EventFlow<ServerEvent>) : ServerManagementContext {
        private var finalized: Boolean = false

        // A state in which the machine is still available, but does not run any of the work requested by the
        // image
        var unavailable = false

        override val cpus: List<ProcessingUnit> = this@SimpleBareMetalDriver.cpus

        override val server: Server
            get() = nodeState.value.server!!

        private val job = domain.launch {
            delay(1) // TODO Introduce boot time
            init()
            try {
                server.image(this@BareMetalServerContext)
                exit()
            } catch (cause: Throwable) {
                exit(cause)
            }
        }

        /**
         * Cancel the image running on the machine.
         */
        suspend fun cancel(fail: Boolean) {
            if (fail)
                job.cancel(ShutdownException(cause = Exception("Random failure")))
            else
                job.cancel(ShutdownException())
            job.join()
        }

        override suspend fun <T : Any> publishService(key: ServiceKey<T>, service: T) {
            val server = server.copy(services = server.services.put(key, service))
            setNode(nodeState.value.copy(server = server))
            events.emit(ServerEvent.ServicePublished(server, key))
        }

        override suspend fun init() {
            assert(!finalized) { "Machine is already finalized" }

            val server = server.copy(state = ServerState.ACTIVE)
            setNode(nodeState.value.copy(state = NodeState.ACTIVE, server = server))
        }

        override suspend fun exit(cause: Throwable?) {
            finalized = true

            val newServerState =
                if (cause == null || (cause is ShutdownException && cause.cause == null))
                    ServerState.SHUTOFF
                else
                    ServerState.ERROR
            val newNodeState =
                if (cause == null || (cause is ShutdownException && cause.cause != null))
                    nodeState.value.state
                else
                    NodeState.ERROR
            val server = server.copy(state = newServerState)
            setNode(nodeState.value.copy(state = newNodeState, server = server))
        }

        private var flush: DisposableHandle? = null

        @OptIn(InternalCoroutinesApi::class)
        override fun onRun(burst: LongArray, limit: DoubleArray, deadline: Long): SelectClause0 {
            require(burst.size == limit.size) { "Array dimensions do not match" }
            assert(!finalized) { "Server instance is already finalized" }

            return object : SelectClause0 {
                @InternalCoroutinesApi
                override fun <R> registerSelectClause0(select: SelectInstance<R>, block: suspend () -> R) {
                    // If run is called in at the same timestamp as the previous call, cancel the load flush
                    flush?.dispose()
                    flush = null

                    val context = select.completion.context
                    val simulationContext = context[SimulationContext]!!
                    val delay = context[ContinuationInterceptor] as Delay

                    val start = simulationContext.clock.millis()
                    var duration = max(0, deadline - start)
                    var totalUsage = 0.0

                    // Determine the duration of the first CPU to finish
                    for (i in 0 until min(cpus.size, burst.size)) {
                        val cpu = cpus[i]
                        val usage = min(limit[i], cpu.frequency)
                        val cpuDuration = ceil(burst[i] / usage * 1000).toLong() // Convert from seconds to milliseconds

                        totalUsage += usage / cpu.frequency

                        if (cpuDuration != 0L) { // We only wait for processor cores with a non-zero burst
                            duration = min(duration, cpuDuration)
                        }
                    }

                    if (!unavailable) {
                        delay.invokeOnTimeout(1, Runnable {
                            usageState.value = totalUsage / cpus.size
                        })
                    }

                    val action = Runnable {
                        // todo: we could have replaced startCoroutine with startCoroutineUndispatched
                        // But we need a way to know that Delay.invokeOnTimeout had used the right thread
                        if (select.trySelect()) {
                            block.startCoroutineCancellable(select.completion) // shall be cancellable while waits for dispatch
                        }
                    }

                    val disposable = delay.invokeOnTimeout(duration, action)
                    val flush = DisposableHandle {
                        val end = simulationContext.clock.millis()

                        // Flush the load if they do not receive a new run call for the same timestamp
                        flush = delay.invokeOnTimeout(1, Runnable {
                            usageState.value = 0.0
                            flush = null
                        })

                        if (!unavailable) {
                            // Write back the remaining burst time
                            for (i in 0 until min(cpus.size, burst.size)) {
                                val usage = min(limit[i], cpus[i].frequency)
                                val granted = ceil((end - start) / 1000.0 * usage).toLong()
                                burst[i] = max(0, burst[i] - granted)
                            }
                        }

                        disposable.dispose()
                    }

                    select.disposeOnSelect(flush)
                }
            }
        }
    }

    override val scope: CoroutineScope
        get() = domain

    override suspend fun fail() {
        serverContext?.unavailable = true

        val server = nodeState.value.server?.copy(state = ServerState.ERROR)
        setNode(nodeState.value.copy(state = NodeState.ERROR, server = server))
    }

    override suspend fun recover() {
        serverContext?.unavailable = false

        val server = nodeState.value.server?.copy(state = ServerState.ACTIVE)
        setNode(nodeState.value.copy(state = NodeState.ACTIVE, server = server))
    }

    override fun toString(): String = "SimpleBareMetalDriver(node = ${nodeState.value.uid})"
}
