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

package org.opendc.serverless.service.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import mu.KotlinLogging
import org.opendc.serverless.api.ServerlessClient
import org.opendc.serverless.api.ServerlessFunction
import org.opendc.serverless.service.ServerlessService
import org.opendc.serverless.service.deployer.FunctionDeployer
import org.opendc.serverless.service.deployer.FunctionInstance
import org.opendc.serverless.service.router.RoutingPolicy
import org.opendc.utils.TimerScheduler
import java.lang.IllegalStateException
import java.time.Clock
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

/**
 * Implementation of the [ServerlessService] interface.
 *
 * This component acts as the function router from the SPEC RG Reference Architecture for FaaS and is responsible
 * for routing incoming requests or events to the correct [FunctionInstance]. If no [FunctionInstance] is available,
 * this component queues the events to await the deployment of new instances.
 */
internal class ServerlessServiceImpl(
    context: CoroutineContext,
    private val clock: Clock,
    private val deployer: FunctionDeployer,
    private val routingPolicy: RoutingPolicy
) : ServerlessService {
    /**
     * The [CoroutineScope] of the service bounded by the lifecycle of the service.
     */
    private val scope = CoroutineScope(context + Job())

    /**
     * The logger instance of this server.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The [TimerScheduler] to use for scheduling the scheduler cycles.
     */
    private val scheduler: TimerScheduler<Unit> = TimerScheduler(scope.coroutineContext, clock)

    /**
     * The [Random] instance used to generate unique identifiers for the objects.
     */
    private val random = Random(0)

    /**
     * The registered functions for this service.
     */
    private val functions = mutableMapOf<UUID, InternalFunction>()
    private val functionsByName = mutableMapOf<String, InternalFunction>()

    /**
     * The queue of invocation requests.
     */
    private val queue = ArrayDeque<InvocationRequest>()

    /**
     * The active function instances.
     */
    private val instancesByFunction = mutableMapOf<InternalFunction, MutableList<FunctionInstance>>()

    override fun newClient(): ServerlessClient {
        return object : ServerlessClient {
            private var isClosed: Boolean = false

            override suspend fun queryFunctions(): List<ServerlessFunction> {
                check(!isClosed) { "Client is already closed" }

                return functions.values.map { ClientFunction(it) }
            }

            override suspend fun findFunction(id: UUID): ServerlessFunction? {
                check(!isClosed) { "Client is already closed" }

                return functions[id]?.let { ClientFunction(it) }
            }

            override suspend fun findFunction(name: String): ServerlessFunction? {
                check(!isClosed) { "Client is already closed" }

                return functionsByName[name]?.let { ClientFunction(it) }
            }

            override suspend fun newFunction(
                name: String,
                labels: Map<String, String>,
                meta: Map<String, Any>
            ): ServerlessFunction {
                check(!isClosed) { "Client is already closed" }
                require(name !in functionsByName) { "Function with same name exists" }

                val uid = UUID(clock.millis(), random.nextLong())
                val function = InternalFunction(
                    this@ServerlessServiceImpl,
                    uid,
                    name,
                    labels,
                    meta
                )

                functionsByName[name] = function
                functions[uid] = function

                return ClientFunction(function)
            }

            override suspend fun invoke(name: String) {
                check(!isClosed) { "Client is already closed" }

                requireNotNull(functionsByName[name]) { "Unknown function" }.invoke()
            }

            override fun close() {
                isClosed = true
            }
        }
    }

    /**
     * Indicate that a new scheduling cycle is needed due to a change to the service's state.
     */
    private fun schedule() {
        // Bail out in case we have already requested a new cycle or the queue is empty.
        if (scheduler.isTimerActive(Unit) || queue.isEmpty()) {
            return
        }

        val quantum = 1000

        // We assume that the provisioner runs at a fixed slot every time quantum (e.g t=0, t=60, t=120).
        // This is important because the slices of the VMs need to be aligned.
        // We calculate here the delay until the next scheduling slot.
        val delay = quantum - (clock.millis() % quantum)

        scheduler.startSingleTimer(Unit, delay, ::doSchedule)
    }

    /**
     * Run a single scheduling iteration.
     */
    @OptIn(InternalCoroutinesApi::class)
    private fun doSchedule() {
        try {
            while (queue.isNotEmpty()) {
                val (function, cont) = queue.poll()

                val instances = instancesByFunction[function]

                // Check if there exists an instance of the function
                val activeInstance = if (instances != null && instances.isNotEmpty()) {
                    routingPolicy.select(instances, function)
                } else {
                    null
                }

                val instance = if (activeInstance != null) {
                    activeInstance
                } else {
                    val instance = deployer.deploy(function)
                    instancesByFunction.compute(function) { _, v ->
                        if (v != null) {
                            v.add(instance)
                            v
                        } else {
                            mutableListOf(instance)
                        }
                    }

                    instance
                }

                // Invoke the function instance
                suspend { instance.invoke() }.startCoroutineCancellable(cont)
            }
        } catch (cause: Throwable) {
            logger.error(cause) { "Exception occurred during scheduling cycle" }
        }
    }

    internal suspend fun invoke(function: InternalFunction) {
        check(function.uid in functions) { "Function does not exist (anymore)" }

        return suspendCancellableCoroutine { cont ->
            if (!queue.add(InvocationRequest(function, cont))) {
                cont.resumeWithException(IllegalStateException("Failed to enqueue request"))
            } else {
                schedule()
            }
        }
    }

    internal fun delete(function: InternalFunction) {
        functions.remove(function.uid)
        functionsByName.remove(function.name)
    }

    override fun close() {
        scope.cancel()

        // Stop all function instances
        for ((_, instances) in instancesByFunction) {
            instances.forEach(FunctionInstance::close)
        }
        instancesByFunction.clear()
    }

    /**
     * A request to invoke a function.
     */
    private data class InvocationRequest(val function: InternalFunction, val cont: Continuation<Unit>)
}
