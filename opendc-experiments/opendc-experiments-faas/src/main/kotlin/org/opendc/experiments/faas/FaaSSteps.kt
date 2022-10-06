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

@file:JvmName("FaaSSteps")

package org.opendc.experiments.faas

import org.opendc.experiments.provisioner.ProvisioningContext
import org.opendc.experiments.provisioner.ProvisioningStep
import org.opendc.faas.service.FaaSService
import org.opendc.faas.service.autoscaler.FunctionTerminationPolicy
import org.opendc.faas.service.router.RoutingPolicy
import org.opendc.faas.simulator.delay.ColdStartModel
import org.opendc.simulator.compute.model.MachineModel

/**
 * Return a [ProvisioningStep] that sets up a [FaaSService].
 *
 * @param serviceDomain The domain name under which to register the compute service.
 * @param routingPolicy The routing policy to use.
 * @param terminationPolicy The function termination policy to use.
 * @param machineModel The [MachineModel] that models the physical machine on which the functions run.
 * @param coldStartModel The cold start models to test.
 */
public fun setupFaaSService(
    serviceDomain: String,
    routingPolicy: (ProvisioningContext) -> RoutingPolicy,
    terminationPolicy: (ProvisioningContext) -> FunctionTerminationPolicy,
    machineModel: MachineModel,
    coldStartModel: ColdStartModel? = null
): ProvisioningStep {
    return FaaSServiceProvisioningStep(serviceDomain, routingPolicy, terminationPolicy, machineModel, coldStartModel)
}
