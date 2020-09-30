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

package org.opendc.compute.metal.driver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectInstance
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.MemoryUnit
import org.opendc.compute.core.ProcessingUnit
import org.opendc.compute.core.Server
import org.opendc.compute.core.ServerEvent
import org.opendc.compute.core.ServerState
import org.opendc.compute.core.execution.ServerContext
import org.opendc.compute.core.execution.ServerManagementContext
import org.opendc.compute.core.execution.ShutdownException
import org.opendc.compute.core.image.EmptyImage
import org.opendc.compute.core.image.Image
import org.opendc.compute.metal.Node
import org.opendc.compute.metal.NodeEvent
import org.opendc.compute.metal.NodeState
import org.opendc.compute.metal.power.ConstantPowerModel
import org.opendc.core.power.PowerModel
import org.opendc.core.services.ServiceKey
import org.opendc.core.services.ServiceRegistry
import org.opendc.utils.flow.EventFlow
import org.opendc.utils.flow.StateFlow
import java.lang.Exception
import java.time.Clock
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A basic implementation of the [BareMetalDriver] that simulates an [Image] running on a bare-metal machine.
 *
 * @param coroutineScope The [CoroutineScope] the driver runs in.
 * @param clock The virtual clock to keep track of time.
 * @param uid The unique identifier of the machine.
 * @param name An optional name of the machine.
 * @param metadata The initial metadata of the node.
 * @param cpus The CPUs available to the bare metal machine.
 * @param memoryUnits The memory units in this machine.
 * @param powerModel The power model of this machine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class SimpleBareMetalDriver(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
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
    private val usageState = MutableStateFlow(0.0)

    /**
     * The machine state.
     */
    private val nodeState =
        StateFlow(Node(uid, name, metadata + ("driver" to this), NodeState.SHUTOFF, EmptyImage, null, events))

    override val node: Flow<Node> = nodeState

    @OptIn(FlowPreview::class)
    override val usage: Flow<Double> = usageState

    override val powerDraw: Flow<Double> = powerModel(this)

    /**
     * The internal random instance.
     */
    private val random = Random(uid.leastSignificantBits xor uid.mostSignificantBits)

    override suspend fun init(): Node {
        return nodeState.value
    }

    override suspend fun start(): Node {
        val node = nodeState.value
        if (node.state != NodeState.SHUTOFF) {
            return node
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
        return nodeState.value
    }

    override suspend fun stop(): Node {
        val node = nodeState.value
        if (node.state == NodeState.SHUTOFF) {
            return node
        }

        // We terminate the image running on the machine
        serverContext!!.cancel(fail = false)
        serverContext = null

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

        override val clock: Clock
            get() = this@SimpleBareMetalDriver.clock

        private val job = coroutineScope.launch {
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

        /**
         * A disposable to prevent resetting the usage state for subsequent calls to onRun.
         */
        private var usageFlush: DisposableHandle? = null

        /**
         * Cache the [Delay] instance for timing.
         *
         * XXX We need to cache this before the call to [onRun] since doing this in [onRun] is too heavy.
         * XXX Note however that this is an ugly hack which may break in the future.
         */
        @OptIn(InternalCoroutinesApi::class)
        private val delay = coroutineScope.coroutineContext[ContinuationInterceptor] as Delay

        @OptIn(InternalCoroutinesApi::class)
        override fun onRun(
            batch: Sequence<ServerContext.Slice>,
            triggerMode: ServerContext.TriggerMode,
            merge: (ServerContext.Slice, ServerContext.Slice) -> ServerContext.Slice
        ): SelectClause0 {
            assert(!finalized) { "Server instance is already finalized" }

            return object : SelectClause0 {
                @InternalCoroutinesApi
                override fun <R> registerSelectClause0(select: SelectInstance<R>, block: suspend () -> R) {
                    // Do not reset the usage state: we will set it ourselves
                    usageFlush?.dispose()
                    usageFlush = null

                    val queue = batch.iterator()
                    var start = Long.MIN_VALUE
                    var currentWork: SliceWork? = null
                    var currentDisposable: DisposableHandle? = null

                    fun schedule(slice: ServerContext.Slice) {
                        start = clock.millis()

                        val isLastSlice = !queue.hasNext()
                        val work = SliceWork(slice)
                        val candidateDuration = when (triggerMode) {
                            ServerContext.TriggerMode.FIRST -> work.minExit
                            ServerContext.TriggerMode.LAST -> work.maxExit
                            ServerContext.TriggerMode.DEADLINE -> slice.deadline - start
                        }

                        // Check whether the deadline is exceeded during the run of the slice.
                        val duration = min(candidateDuration, slice.deadline - start)

                        val action = Runnable {
                            currentWork = null

                            // Flush all the work that was performed
                            val hasFinished = work.stop(duration)

                            if (!isLastSlice) {
                                val candidateSlice = queue.next()
                                val nextSlice =
                                    // If our previous slice exceeds its deadline, merge it with the next candidate slice
                                    if (hasFinished)
                                        candidateSlice
                                    else
                                        merge(candidateSlice, slice)
                                schedule(nextSlice)
                            } else if (select.trySelect()) {
                                block.startCoroutineCancellable(select.completion)
                            }
                        }

                        // Schedule the flush after the entire slice has finished
                        currentDisposable = delay.invokeOnTimeout(duration, action)

                        // Start the slice work
                        currentWork = work
                        work.start()
                    }

                    // Schedule the first work
                    if (queue.hasNext()) {
                        schedule(queue.next())

                        // A DisposableHandle to flush the work in case the call is cancelled
                        val disposable = DisposableHandle {
                            val end = clock.millis()
                            val duration = end - start

                            currentWork?.stop(duration)
                            currentDisposable?.dispose()

                            // Schedule reset the usage of the machine since the call is returning
                            usageFlush = delay.invokeOnTimeout(1) {
                                usageState.value = 0.0
                                usageFlush = null
                            }
                        }

                        select.disposeOnSelect(disposable)
                    } else if (select.trySelect()) {
                        // No work has been given: select immediately
                        block.startCoroutineCancellable(select.completion)
                    }
                }
            }
        }

        /**
         * A slice to be processed.
         */
        private inner class SliceWork(val slice: ServerContext.Slice) {
            /**
             * The duration after which the first processor finishes processing this slice.
             */
            public val minExit: Long

            /**
             * The duration after which the last processor finishes processing this slice.
             */
            public val maxExit: Long

            /**
             * A flag to indicate that the slice will exceed the deadline.
             */
            public val exceedsDeadline: Boolean
                get() = slice.deadline < maxExit

            /**
             * The total amount of CPU usage.
             */
            public val totalUsage: Double

            /**
             * A flag to indicate that this slice is empty.
             */
            public val isEmpty: Boolean

            init {
                var totalUsage = 0.0
                var minExit = Long.MAX_VALUE
                var maxExit = 0L
                var nonEmpty = false

                // Determine the duration of the first/last CPU to finish
                for (i in 0 until min(cpus.size, slice.burst.size)) {
                    val cpu = cpus[i]
                    val usage = min(slice.limit[i], cpu.frequency)
                    val cpuDuration =
                        ceil(slice.burst[i] / usage * 1000).toLong() // Convert from seconds to milliseconds

                    totalUsage += usage / cpu.frequency

                    if (cpuDuration != 0L) { // We only wait for processor cores with a non-zero burst
                        minExit = min(minExit, cpuDuration)
                        maxExit = max(maxExit, cpuDuration)
                        nonEmpty = true
                    }
                }

                this.isEmpty = !nonEmpty
                this.totalUsage = totalUsage
                this.minExit = minExit
                this.maxExit = maxExit
            }

            /**
             * Indicate that the work on the slice has started.
             */
            public fun start() {
                usageState.value = totalUsage / cpus.size
            }

            /**
             * Flush the work performed on the slice.
             */
            public fun stop(duration: Long): Boolean {
                var hasFinished = true

                // Only flush the work if the machine is available
                if (!unavailable) {
                    for (i in 0 until min(cpus.size, slice.burst.size)) {
                        val usage = min(slice.limit[i], cpus[i].frequency)
                        val granted = ceil(duration / 1000.0 * usage).toLong()
                        val res = max(0, slice.burst[i] - granted)
                        slice.burst[i] = res

                        if (res != 0L) {
                            hasFinished = false
                        }
                    }
                }

                return hasFinished
            }
        }
    }

    override val scope: CoroutineScope
        get() = coroutineScope

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
