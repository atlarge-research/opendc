package com.atlarge.opendc.compute.virt.driver.hypervisor

import com.atlarge.odcsim.SimulationContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.execution.ProcessorContext
import com.atlarge.opendc.compute.core.execution.ServerManagementContext
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The execution context of an image running as a server.
 *
 * @property server The server instance of this execution context.
 * @property monitor The monitor that should be informed on changes.
 * @property scheduler The local VM scheduler.
 * @property onExit The callback to be invoked on exit of the server.
 * @param ctx The simulation context in which this server is running.
 */
class VmServerContext(
    override var server: Server,
    val monitor: ServerMonitor,
    val scheduler: VmScheduler,
    val onExit: suspend () -> Unit,
    ctx: SimulationContext
) : ServerManagementContext {
    private var initialized: Boolean = false

    internal val job: Job = ctx.domain.launch {
        init()
        try {
            server.image(this@VmServerContext)
            exit()
        } catch (cause: Throwable) {
            exit(cause)
        }
    }

    override val cpus: List<ProcessorContext> = scheduler.createVirtualCpus(server.flavor, this)

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
        onExit()
        monitor.onUpdate(server, previousState)
        initialized = false
    }
}
