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

package org.opendc.serverless.simulator

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.opendc.serverless.service.FunctionObject
import org.opendc.serverless.service.deployer.FunctionDeployer
import org.opendc.serverless.service.deployer.FunctionInstance
import org.opendc.serverless.service.deployer.FunctionInstanceState
import org.opendc.serverless.simulator.workload.SimServerlessWorkloadMapper
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.cpufreq.PerformanceScalingGovernor
import org.opendc.simulator.compute.cpufreq.SimpleScalingDriver
import org.opendc.simulator.compute.power.ConstantPowerModel
import java.time.Clock
import java.util.ArrayDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A [FunctionDeployer] that uses that simulates the [FunctionInstance]s.
 */
public class SimFunctionDeployer(
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val model: SimMachineModel,
    private val mapper: SimServerlessWorkloadMapper
) : FunctionDeployer {

    override fun deploy(function: FunctionObject): Instance {
        val instance = Instance(function)
        instance.start()
        return instance
    }

    /**
     * A simulated [FunctionInstance].
     */
    public inner class Instance(override val function: FunctionObject) : FunctionInstance {
        /**
         * The workload associated with this instance.
         */
        private val workload = mapper.createWorkload(function)

        /**
         * The machine that will execute the workloads.
         */
        public val machine: SimMachine = SimBareMetalMachine(scope.coroutineContext, clock, model, PerformanceScalingGovernor(), SimpleScalingDriver(ConstantPowerModel(0.0)))

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

        override suspend fun invoke() {
            check(state != FunctionInstanceState.Deleted) { "Function instance has been released" }
            return suspendCancellableCoroutine { cont ->
                queue.add(InvocationRequest(cont))
                chan.offer(Unit)
            }
        }

        override fun close() {
            state = FunctionInstanceState.Deleted
            stop()
            machine.close()
        }

        override fun toString(): String = "FunctionInstance[state=$state]"

        /**
         * Start the function instance.
         */
        @OptIn(InternalCoroutinesApi::class)
        internal fun start() {
            check(state == FunctionInstanceState.Provisioning) { "Invalid state of function instance" }
            job = scope.launch {
                launch {
                    try {
                        machine.run(workload)
                    } finally {
                        state = FunctionInstanceState.Terminated
                    }
                }

                while (isActive) {
                    chan.receive()

                    if (queue.isNotEmpty()) {
                        state = FunctionInstanceState.Active
                    }

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

    /**
     * A function invocation request.
     */
    private data class InvocationRequest(val cont: Continuation<Unit>)
}
