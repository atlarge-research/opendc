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

package org.opendc.experiments.compute

import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.experiments.provisioner.ProvisioningContext
import org.opendc.experiments.provisioner.ProvisioningStep
import java.time.Duration

/**
 * A [ProvisioningStep] that provisions a [ComputeService] without any hosts.
 *
 * @param serviceDomain The domain name under which to register the compute service.
 * @param scheduler A function to construct the compute scheduler.
 * @param schedulingQuantum The scheduling quantum of the compute scheduler.
 */
public class ComputeServiceProvisioningStep internal constructor(
    private val serviceDomain: String,
    private val scheduler: (ProvisioningContext) -> ComputeScheduler,
    private val schedulingQuantum: Duration
) : ProvisioningStep {
    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val service = ComputeService.builder(ctx.dispatcher, scheduler(ctx))
            .withQuantum(schedulingQuantum)
            .build()
        ctx.registry.register(serviceDomain, ComputeService::class.java, service)

        return AutoCloseable { service.close() }
    }
}
