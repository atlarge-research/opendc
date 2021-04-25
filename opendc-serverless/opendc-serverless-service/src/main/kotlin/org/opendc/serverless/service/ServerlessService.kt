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

package org.opendc.serverless.service

import io.opentelemetry.api.metrics.Meter
import org.opendc.serverless.api.ServerlessClient
import org.opendc.serverless.service.deployer.FunctionDeployer
import org.opendc.serverless.service.internal.ServerlessServiceImpl
import org.opendc.serverless.service.router.RoutingPolicy
import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * The [ServerlessService] hosts the API implementation of the OpenDC Serverless service.
 */
public interface ServerlessService : AutoCloseable {
    /**
     * Create a new [ServerlessClient] to control the compute service.
     */
    public fun newClient(): ServerlessClient

    /**
     * Terminate the lifecycle of the serverless service, stopping all running function instances.
     */
    public override fun close()

    public companion object {
        /**
         * Construct a new [ServerlessService] implementation.
         *
         * @param context The [CoroutineContext] to use in the service.
         * @param clock The clock instance to use.
         * @param meter The meter to report metrics to.
         * @param deployer the [FunctionDeployer] to use for deploying function instances.
         * @param routingPolicy The policy to route function invocations.
         */
        public operator fun invoke(
            context: CoroutineContext,
            clock: Clock,
            meter: Meter,
            deployer: FunctionDeployer,
            routingPolicy: RoutingPolicy,
        ): ServerlessService {
            return ServerlessServiceImpl(context, clock, meter, deployer, routingPolicy)
        }
    }
}
