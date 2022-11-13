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

package org.opendc.experiments.workflow

import org.opendc.compute.service.ComputeService
import org.opendc.experiments.provisioner.ProvisioningContext
import org.opendc.experiments.provisioner.ProvisioningStep
import org.opendc.workflow.service.WorkflowService
import java.time.Duration

/**
 * A [ProvisioningStep] that provisions a [WorkflowService].
 *
 * @param serviceDomain The domain name under which to register the workflow service.
 * @param computeService The domain name where the underlying compute service is located.
 * @param scheduler The configuration of the scheduler of the workflow engine.
 * @param schedulingQuantum The scheduling quantum of the compute scheduler.
 */
public class WorkflowServiceProvisioningStep internal constructor(
    private val serviceDomain: String,
    private val computeService: String,
    private val scheduler: WorkflowSchedulerSpec,
    private val schedulingQuantum: Duration
) : ProvisioningStep {
    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val computeService = requireNotNull(ctx.registry.resolve(computeService, ComputeService::class.java)) { "Compute service $computeService does not exist" }

        val client = computeService.newClient()
        val service = WorkflowService(
            ctx.dispatcher,
            client,
            scheduler.schedulingQuantum,
            jobAdmissionPolicy = scheduler.jobAdmissionPolicy,
            jobOrderPolicy = scheduler.jobOrderPolicy,
            taskEligibilityPolicy = scheduler.taskEligibilityPolicy,
            taskOrderPolicy = scheduler.taskOrderPolicy
        )
        ctx.registry.register(serviceDomain, WorkflowService::class.java, service)

        return AutoCloseable {
            service.close()
            client.close()
        }
    }
}
