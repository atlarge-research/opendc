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

package org.opendc.faas.simulator

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.opendc.common.Dispatcher
import org.opendc.common.asCoroutineDispatcher
import org.opendc.faas.service.FunctionObject
import org.opendc.faas.service.deployer.FunctionDeployer
import org.opendc.faas.service.deployer.FunctionInstance
import org.opendc.faas.service.deployer.FunctionInstanceListener
import org.opendc.faas.service.deployer.FunctionInstanceState
import org.opendc.faas.simulator.delay.DelayInjector
import org.opendc.faas.simulator.workload.SimFaaSWorkloadMapper
import org.opendc.faas.simulator.workload.SimMetaFaaSWorkloadMapper
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.runWorkload
import org.opendc.simulator.flow2.FlowEngine
import java.util.ArrayDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A [FunctionDeployer] that uses that simulates the [FunctionInstance]s.
 */
public class SimFunctionDeployer(
    private val dispatcher: Dispatcher,
    private val model: MachineModel,
    private val delayInjector: DelayInjector,
    private val mapper: SimFaaSWorkloadMapper = SimMetaFaaSWorkloadMapper()
) : FunctionDeployer, AutoCloseable {
    /**
     * The [CoroutineScope] of this deployer.
     */
    private val scope = CoroutineScope(dispatcher.asCoroutineDispatcher() + Job())

    override fun deploy(function: FunctionObject, listener: FunctionInstanceListener): Instance {
        val instance = Instance(function, listener)
        instance.start()
        return instance
    }

    /**
     * A simulated [FunctionInstance].
     */
    public inner class Instance(override val function: FunctionObject, private val listener: FunctionInstanceListener) :
        FunctionInstance {
        /**
         * The workload associated with this instance.
         */
        private val workload = mapper.createWorkload(function)

        /**
         * The machine that will execute the workloads.
         */
        public val machine: SimMachine = SimBareMetalMachine.create(
            FlowEngine.create(dispatcher).newGraph(),
            model
        )

        /**
         * The job associated with the lifecycle of the instance.
         */
        private var job: Job? = null

        /**
         * The invocation request queue.
         */
        private val queue = ArrayDeque<InvocationRequest>()

        /**
         * A channel used to signal that new invocations have been enqueued.
         */
        private val chan = Channel<Unit>(Channel.RENDEZVOUS)

        override var state: FunctionInstanceState = FunctionInstanceState.Provisioning
            set(value) {
                if (field != value) {
                    listener.onStateChanged(this, value)
                }

                field = value
            }

        override suspend fun invoke() {
            check(state != FunctionInstanceState.Deleted) { "Function instance has been released" }
            return suspendCancellableCoroutine { cont ->
                queue.add(InvocationRequest(cont))
                chan.trySend(Unit)
            }
        }

        override fun close() {
            state = FunctionInstanceState.Deleted
            stop()
            machine.cancel()
        }

        override fun toString(): String = "FunctionInstance[state=$state]"

        /**
         * Start the function instance.
         */
        internal fun start() {
            check(state == FunctionInstanceState.Provisioning) { "Invalid state of function instance" }
            job = scope.launch {
                delay(delayInjector.getColdStartDelay(this@Instance))

                launch {
                    try {
                        machine.runWorkload(workload)
                    } finally {
                        state = FunctionInstanceState.Deleted
                    }
                }

                while (isActive) {
                    if (queue.isEmpty()) {
                        chan.receive()
                    }

                    state = FunctionInstanceState.Active
                    while (queue.isNotEmpty()) {
                        val request = queue.poll()
                        try {
                            workload.invoke()
                            request.cont.resume(Unit)
                        } catch (cause: CancellationException) {
                            request.cont.resumeWithException(cause)
                            throw cause
                        } catch (cause: Throwable) {
                            request.cont.resumeWithException(cause)
                        }
                    }
                    state = FunctionInstanceState.Idle
                }
            }
        }

        /**
         * Stop the function instance.
         */
        private fun stop() {
            val job = job

            if (job != null) {
                this.job = null
                job.cancel()
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    /**
     * A function invocation request.
     */
    private data class InvocationRequest(val cont: Continuation<Unit>)
}
