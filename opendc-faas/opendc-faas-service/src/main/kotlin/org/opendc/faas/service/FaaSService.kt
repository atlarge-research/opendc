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

import io.opentelemetry.api.metrics.Meter
import org.opendc.faas.api.FaaSClient
import org.opendc.faas.service.autoscaler.FunctionTerminationPolicy
import org.opendc.faas.service.deployer.FunctionDeployer
import org.opendc.faas.service.internal.FaaSServiceImpl
import org.opendc.faas.service.router.RoutingPolicy
import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * The [FaaSService] hosts the service implementation of the OpenDC FaaS platform.
 */
public interface FaaSService : AutoCloseable {
    /**
     * Create a new [FaaSClient] to control the compute service.
     */
    public fun newClient(): FaaSClient

    /**
     * Terminate the lifecycle of the FaaS service, stopping all running function instances.
     */
    public override fun close()

    public companion object {
        /**
         * Construct a new [FaaSService] implementation.
         *
         * @param context The [CoroutineContext] to use in the service.
         * @param clock The clock instance to use.
         * @param meter The meter to report metrics to.
         * @param deployer the [FunctionDeployer] to use for deploying function instances.
         * @param routingPolicy The policy to route function invocations.
         * @param terminationPolicy The policy for terminating function instances.
         */
        public operator fun invoke(
            context: CoroutineContext,
            clock: Clock,
            meter: Meter,
            deployer: FunctionDeployer,
            routingPolicy: RoutingPolicy,
            terminationPolicy: FunctionTerminationPolicy,
        ): FaaSService {
            return FaaSServiceImpl(context, clock, meter, deployer, routingPolicy, terminationPolicy)
        }
    }
}
