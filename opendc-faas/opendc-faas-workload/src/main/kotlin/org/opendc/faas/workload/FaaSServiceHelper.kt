/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.faas.workload

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.opendc.faas.api.FaaSFunction
import org.opendc.faas.service.FaaSService
import org.opendc.faas.service.FunctionObject
import org.opendc.faas.service.autoscaler.FunctionTerminationPolicy
import org.opendc.faas.service.deployer.FunctionDeployer
import org.opendc.faas.service.deployer.FunctionInstance
import org.opendc.faas.service.deployer.FunctionInstanceListener
import org.opendc.faas.service.router.RoutingPolicy
import org.opendc.faas.simulator.SimFunctionDeployer
import org.opendc.faas.simulator.delay.ColdStartModel
import org.opendc.faas.simulator.delay.StochasticDelayInjector
import org.opendc.faas.simulator.delay.ZeroDelayInjector
import org.opendc.simulator.compute.model.MachineModel
import java.time.Clock
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

/**
 * Helper class to simulate FaaS-based workloads in OpenDC.
 *
 * @param context A [CoroutineContext] to run the simulation in.
 * @param clock A [Clock] instance tracking simulation time.
 * @param machineModel The [MachineModel] that models the physical machine on which the functions run.
 * @param routingPolicy The routing policy to use.
 * @param terminationPolicy The function termination policy to use.
 * @param coldStartModel The cold start models to test.
 * @param seed The seed of the simulation.
 */
public class FaaSServiceHelper(
    private val context: CoroutineContext,
    private val clock: Clock,
    private val machineModel: MachineModel,
    private val routingPolicy: RoutingPolicy,
    private val terminationPolicy: FunctionTerminationPolicy,
    private val coldStartModel: ColdStartModel? = null,
) : AutoCloseable {
    /**
     * The scope of this helper.
     */
    private val scope = CoroutineScope(context + Job())

    /**
     * The logger for this class.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The simulated function deployer.
     */
    private val deployer = object : FunctionDeployer {
        override fun deploy(function: FunctionObject, listener: FunctionInstanceListener): FunctionInstance {
            val deployer = checkNotNull(_deployer)
            return deployer.deploy(function, listener)
        }
    }
    private var _deployer: SimFunctionDeployer? = null

    /**
     * The [FaaSService] created by the helper.
     */
    public val service: FaaSService = FaaSService(
        context,
        clock,
        deployer,
        routingPolicy,
        terminationPolicy
    )

    /**
     * Run a simulation of the [FaaSService] by replaying the workload trace given by [trace].
     *
     * @param trace The trace to simulate.
     * @param seed The seed for the simulation.
     * @param functions The functions that have been created by the runner.
     */
    public suspend fun run(trace: List<FunctionTrace>, seed: Long = 0, functions: MutableList<FaaSFunction>? = null) {
        // Set up the simulated deployer
        val delayInjector = if (coldStartModel != null)
            StochasticDelayInjector(coldStartModel, Random(seed))
        else
            ZeroDelayInjector
        val traceById = trace.associateBy { it.id }
        _deployer = SimFunctionDeployer(clock, scope, machineModel, delayInjector) {
            FunctionTraceWorkload(traceById.getValue(it.name))
        }

        val client = service.newClient()
        try {
            coroutineScope {
                for (entry in trace) {
                    launch {
                        val function = client.newFunction(entry.id, entry.maxMemory.toLong())
                        functions?.add(function)

                        var offset = Long.MIN_VALUE

                        for (sample in entry.samples) {
                            if (sample.invocations == 0) {
                                continue
                            }

                            if (offset < 0) {
                                offset = sample.timestamp - clock.millis()
                            }

                            delay(max(0, (sample.timestamp - offset) - clock.millis()))

                            logger.info { "Invoking function ${entry.id} ${sample.invocations} times [${sample.timestamp}]" }

                            repeat(sample.invocations) {
                                function.invoke()
                            }
                        }
                    }
                }
            }
        } finally {
            client.close()
        }
    }

    override fun close() {
        service.close()
        scope.cancel()
    }
}
