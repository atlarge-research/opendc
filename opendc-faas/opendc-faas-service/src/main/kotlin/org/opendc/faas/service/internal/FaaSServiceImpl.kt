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

package org.opendc.faas.service.internal

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import org.opendc.common.Dispatcher
import org.opendc.common.util.Pacer
import org.opendc.faas.api.FaaSClient
import org.opendc.faas.api.FaaSFunction
import org.opendc.faas.service.FaaSService
import org.opendc.faas.service.FunctionObject
import org.opendc.faas.service.autoscaler.FunctionTerminationPolicy
import org.opendc.faas.service.deployer.FunctionDeployer
import org.opendc.faas.service.deployer.FunctionInstance
import org.opendc.faas.service.deployer.FunctionInstanceListener
import org.opendc.faas.service.deployer.FunctionInstanceState
import org.opendc.faas.service.router.RoutingPolicy
import org.opendc.faas.service.telemetry.FunctionStats
import org.opendc.faas.service.telemetry.SchedulerStats
import java.lang.IllegalStateException
import java.time.Duration
import java.time.InstantSource
import java.util.ArrayDeque
import java.util.Random
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

/**
 * Implementation of the [FaaSService] interface.
 *
 * This component acts as the function router from the SPEC RG Reference Architecture for FaaS and is responsible
 * for routing incoming requests or events to the correct [FunctionInstance]. If no [FunctionInstance] is available,
 * this component queues the events to await the deployment of new instances.
 */
internal class FaaSServiceImpl(
    dispatcher: Dispatcher,
    private val deployer: FunctionDeployer,
    private val routingPolicy: RoutingPolicy,
    private val terminationPolicy: FunctionTerminationPolicy,
    quantum: Duration
) : FaaSService, FunctionInstanceListener {
    /**
     * The logger instance of this server.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The [Pacer] to use for scheduling the scheduler cycles.
     */
    private val pacer = Pacer(dispatcher, quantum.toMillis()) { doSchedule() }

    /**
     * The [InstantSource] instance representing the clock.
     */
    private val clock = dispatcher.timeSource

    /**
     * The [Random] instance used to generate unique identifiers for the objects.
     */
    private val random = Random(0)

    /**
     * The registered functions for this service.
     */
    private val functions = mutableMapOf<UUID, FunctionObject>()
    private val functionsByName = mutableMapOf<String, FunctionObject>()

    /**
     * The queue of invocation requests.
     */
    private val queue = ArrayDeque<InvocationRequest>()

    /**
     * Metrics tracked by the service.
     */
    private var totalInvocations = 0L
    private var timelyInvocations = 0L
    private var delayedInvocations = 0L

    override fun newClient(): FaaSClient {
        return object : FaaSClient {
            private var isClosed: Boolean = false

            /**
             * Exposes a [FunctionObject] to a client-exposed [FaaSFunction] instance.
             */
            private fun FunctionObject.asClientFunction(): FaaSFunction {
                return FaaSFunctionImpl(this@FaaSServiceImpl, this)
            }

            override suspend fun queryFunctions(): List<FaaSFunction> {
                check(!isClosed) { "Client is already closed" }

                return functions.values.map { it.asClientFunction() }
            }

            override suspend fun findFunction(id: UUID): FaaSFunction? {
                check(!isClosed) { "Client is already closed" }

                return functions[id]?.asClientFunction()
            }

            override suspend fun findFunction(name: String): FaaSFunction? {
                check(!isClosed) { "Client is already closed" }

                return functionsByName[name]?.asClientFunction()
            }

            override suspend fun newFunction(
                name: String,
                memorySize: Long,
                labels: Map<String, String>,
                meta: Map<String, Any>
            ): FaaSFunction {
                check(!isClosed) { "Client is already closed" }
                require(name !in functionsByName) { "Function with same name exists" }

                val uid = UUID(clock.millis(), random.nextLong())
                val function = FunctionObject(
                    uid,
                    name,
                    memorySize,
                    labels,
                    meta
                )

                functionsByName[name] = function
                functions[uid] = function

                return function.asClientFunction()
            }

            override suspend fun invoke(name: String) {
                check(!isClosed) { "Client is already closed" }

                val func = requireNotNull(functionsByName[name]) { "Unknown function" }
                this@FaaSServiceImpl.invoke(func)
            }

            override fun close() {
                isClosed = true
            }
        }
    }

    override fun getSchedulerStats(): SchedulerStats {
        return SchedulerStats(totalInvocations, timelyInvocations, delayedInvocations)
    }

    override fun getFunctionStats(function: FaaSFunction): FunctionStats {
        val func = requireNotNull(functions[function.uid]) { "Unknown function" }
        return func.getStats()
    }

    /**
     * Indicate that a new scheduling cycle is needed due to a change to the service's state.
     */
    private fun schedule() {
        // Bail out in case the queue is empty.
        if (queue.isEmpty()) {
            return
        }

        pacer.enqueue()
    }

    /**
     * Run a single scheduling iteration.
     */
    @OptIn(InternalCoroutinesApi::class)
    private fun doSchedule() {
        try {
            while (queue.isNotEmpty()) {
                val (submitTime, function, cont) = queue.poll()

                val instances = function.instances

                // Check if there exists an instance of the function
                val activeInstance = if (instances.isNotEmpty()) {
                    routingPolicy.select(instances, function)
                } else {
                    null
                }

                val instance = if (activeInstance != null) {
                    timelyInvocations++
                    function.reportDeployment(isDelayed = false)

                    activeInstance
                } else {
                    val instance = deployer.deploy(function, this)
                    instances.add(instance)
                    terminationPolicy.enqueue(instance)

                    delayedInvocations++
                    function.reportDeployment(isDelayed = true)

                    instance
                }

                suspend {
                    val start = clock.millis()
                    function.reportStart(start, submitTime)
                    try {
                        instance.invoke()
                    } catch (e: Throwable) {
                        logger.debug(e) { "Function invocation failed" }
                        function.reportFailure()
                    } finally {
                        val end = clock.millis()
                        function.reportEnd(end - start)
                    }
                }.startCoroutineCancellable(cont)
            }
        } catch (cause: Throwable) {
            logger.error(cause) { "Exception occurred during scheduling cycle" }
        }
    }

    suspend fun invoke(function: FunctionObject) {
        check(function.uid in functions) { "Function does not exist (anymore)" }

        totalInvocations++
        function.reportSubmission()

        return suspendCancellableCoroutine { cont ->
            if (!queue.add(InvocationRequest(clock.millis(), function, cont))) {
                cont.resumeWithException(IllegalStateException("Failed to enqueue request"))
            } else {
                schedule()
            }
        }
    }

    fun delete(function: FunctionObject) {
        functions.remove(function.uid)
        functionsByName.remove(function.name)
    }

    override fun close() {
        // Stop all function instances
        for ((_, function) in functions) {
            function.close()
        }
    }

    override fun onStateChanged(instance: FunctionInstance, newState: FunctionInstanceState) {
        terminationPolicy.onStateChanged(instance, newState)

        if (newState == FunctionInstanceState.Deleted) {
            val function = instance.function
            function.instances.remove(instance)
        }
    }

    /**
     * A request to invoke a function.
     */
    private data class InvocationRequest(val timestamp: Long, val function: FunctionObject, val cont: Continuation<Unit>)
}
