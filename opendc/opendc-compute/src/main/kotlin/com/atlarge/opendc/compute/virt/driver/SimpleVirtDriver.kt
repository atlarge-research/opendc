package com.atlarge.opendc.compute.virt.driver

import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerFlavor
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.image.VM_SCHEDULING_SLICE_DURATION
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.virt.RunRequest
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.min

class SimpleVirtDriver(
    private val ctx: ProcessContext,
    private val hostContext: ServerContext,
    private val hypervisorMonitor: HypervisorMonitor
) : VirtDriver {
    /**
     * The contexts of all VMs running on this hypervisor.
     */
    private val serverContexts: MutableSet<VmServerContext> = mutableSetOf()

    init {
        ctx.launch {
            while (isActive) {
                val serverFlavor = hostContext.server.flavor

                val requests = serverContexts.map { ctx.async { it.channel.receive() } }.awaitAll()
                require(requests.all { it.reqDuration == VM_SCHEDULING_SLICE_DURATION })

                if (requests.isEmpty()) {
                    hostContext.run(LongArray(serverFlavor.cpus[0].cores) { 0 }, 5 * 60 * 1000)
                } else {
                    val totalRequested = requests.map { it.req.sum() }.sum()
                    val capacity = (serverFlavor.cpus[0].cores * serverFlavor.cpus[0].clockRate * 1_000_000L).toLong()

                    hypervisorMonitor.onSliceFinish(
                        processContext.clock.millis(),
                        totalRequested,
                        capacity,
                        serverContexts.size,
                        hostContext.server
                    )

                    val satisfiedCapacity = min(capacity, totalRequested)
                    requests.forEach { request ->
                        val individualAssignedCapacity = (
                            satisfiedCapacity * (request.req.sum().toDouble() / totalRequested) /
                                request.req.size).toLong()

                        request.continuation.resume(
                            hostContext.run(
                                LongArray(request.req.size) { individualAssignedCapacity },
                                VM_SCHEDULING_SLICE_DURATION
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun spawn(image: Image, monitor: ServerMonitor, flavor: ServerFlavor): Server {
        val server = Server(UUID.randomUUID(), "<unnamed>", flavor, image, ServerState.BUILD)
        val context = VmServerContext(server, monitor, flavor, hostContext, Channel(Channel.CONFLATED))
        serverContexts.add(context)
        context.init()
        processContext.launch { image(context) }
        return server
    }

    override suspend fun getNumberOfSpawnedImages(): Int {
        return serverContexts.size
    }

    class VmServerContext(
        override var server: Server,
        val monitor: ServerMonitor,
        val flavor: ServerFlavor,
        val hostContext: ServerContext,
        val channel: Channel<RunRequest>
    ) :
        ServerManagementContext {
        private var initialized: Boolean = false

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
            monitor.onUpdate(server, previousState)
            initialized = false
        }

        override suspend fun run(req: LongArray, reqDuration: Long): LongArray {
            return suspendCancellableCoroutine { cont ->
                channel.offer(RunRequest(req, reqDuration, cont))
            }
        }
    }
}
