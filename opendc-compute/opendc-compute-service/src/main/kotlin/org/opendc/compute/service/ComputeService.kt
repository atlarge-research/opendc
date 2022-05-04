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

package org.opendc.compute.service

import org.opendc.compute.api.ComputeClient
import org.opendc.compute.api.Server
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.internal.ComputeServiceImpl
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.service.telemetry.SchedulerStats
import java.time.Clock
import java.time.Duration
import kotlin.coroutines.CoroutineContext

/**
 * The [ComputeService] hosts the API implementation of the OpenDC Compute service.
 */
public interface ComputeService : AutoCloseable {
    /**
     * The hosts that are registered with the "compute" service.
     */
    public val hosts: Set<Host>

    /**
     * Create a new [ComputeClient] to control the compute service.
     */
    public fun newClient(): ComputeClient

    /**
     * Add a [host] to the scheduling pool of the compute service.
     */
    public fun addHost(host: Host)

    /**
     * Remove a [host] from the scheduling pool of the compute service.
     */
    public fun removeHost(host: Host)

    /**
     * Terminate the lifecycle of the compute service, stopping all running instances.
     */
    public override fun close()

    /**
     * Lookup the [Host] that currently hosts the specified [server].
     */
    public fun lookupHost(server: Server): Host?

    /**
     * Collect the statistics about the scheduler component of this service.
     */
    public fun getSchedulerStats(): SchedulerStats

    public companion object {
        /**
         * Construct a new [ComputeService] implementation.
         *
         * @param context The [CoroutineContext] to use in the service.
         * @param clock The clock instance to use.
         * @param scheduler The scheduler implementation to use.
         * @param schedulingQuantum The interval between scheduling cycles.
         */
        public operator fun invoke(
            context: CoroutineContext,
            clock: Clock,
            scheduler: ComputeScheduler,
            schedulingQuantum: Duration = Duration.ofMinutes(5),
        ): ComputeService {
            return ComputeServiceImpl(context, clock, scheduler, schedulingQuantum)
        }
    }
}
