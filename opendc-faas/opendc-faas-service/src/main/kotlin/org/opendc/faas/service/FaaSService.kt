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

package org.opendc.faas.service

import org.opendc.common.Dispatcher
import org.opendc.faas.api.FaaSClient
import org.opendc.faas.api.FaaSFunction
import org.opendc.faas.service.autoscaler.FunctionTerminationPolicy
import org.opendc.faas.service.deployer.FunctionDeployer
import org.opendc.faas.service.internal.FaaSServiceImpl
import org.opendc.faas.service.router.RoutingPolicy
import org.opendc.faas.service.telemetry.FunctionStats
import org.opendc.faas.service.telemetry.SchedulerStats
import java.time.Duration

/**
 * The [FaaSService] hosts the service implementation of the OpenDC FaaS platform.
 */
public interface FaaSService : AutoCloseable {
    /**
     * Create a new [FaaSClient] to control the compute service.
     */
    public fun newClient(): FaaSClient

    /**
     * Collect statistics about the scheduler of the service.
     */
    public fun getSchedulerStats(): SchedulerStats

    /**
     * Collect statistics about the specified [function].
     */
    public fun getFunctionStats(function: FaaSFunction): FunctionStats

    /**
     * Terminate the lifecycle of the FaaS service, stopping all running function instances.
     */
    public override fun close()

    public companion object {
        /**
         * Construct a new [FaaSService] implementation.
         *
         * @param dispatcher The [Dispatcher] used for scheduling events.
         * @param deployer the [FunctionDeployer] to use for deploying function instances.
         * @param routingPolicy The policy to route function invocations.
         * @param terminationPolicy The policy for terminating function instances.
         * @param quantum The scheduling quantum of the service (100 ms default)
         */
        public operator fun invoke(
            dispatcher: Dispatcher,
            deployer: FunctionDeployer,
            routingPolicy: RoutingPolicy,
            terminationPolicy: FunctionTerminationPolicy,
            quantum: Duration = Duration.ofMillis(100)
        ): FaaSService {
            return FaaSServiceImpl(dispatcher, deployer, routingPolicy, terminationPolicy, quantum)
        }
    }
}
