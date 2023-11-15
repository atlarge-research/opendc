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

@file:JvmName("ComputeSteps")

package org.opendc.experiments.compute

import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.experiments.compute.telemetry.ComputeMonitor
import org.opendc.experiments.compute.telemetry.ComputeMonitorProvisioningStep
import org.opendc.experiments.compute.topology.HostSpec
import org.opendc.experiments.provisioner.ProvisioningContext
import org.opendc.experiments.provisioner.ProvisioningStep
import java.time.Duration

/**
 * Return a [ProvisioningStep] that provisions a [ComputeService] without any hosts.
 *
 * @param serviceDomain The domain name under which to register the compute service.
 * @param scheduler A function to construct the compute scheduler.
 * @param schedulingQuantum The scheduling quantum of the compute scheduler.
 */
public fun setupComputeService(
    serviceDomain: String,
    scheduler: (ProvisioningContext) -> ComputeScheduler,
    schedulingQuantum: Duration = Duration.ofMinutes(5)
): ProvisioningStep {
    return ComputeServiceProvisioningStep(serviceDomain, scheduler, schedulingQuantum)
}

/**
 * Return a [ProvisioningStep] that installs a [ComputeMetricReader] to periodically collect the metrics of a
 * [ComputeService] and report them to a [ComputeMonitor].
 *
 * @param serviceDomain The service domain at which the [ComputeService] is located.
 * @param monitor The [ComputeMonitor] to install.
 * @param exportInterval The interval between which to collect the metrics.
 */
public fun registerComputeMonitor(
    serviceDomain: String,
    monitor: ComputeMonitor,
    exportInterval: Duration = Duration.ofMinutes(5)
): ProvisioningStep {
    return ComputeMonitorProvisioningStep(serviceDomain, monitor, exportInterval)
}

/**
 * Return a [ProvisioningStep] that sets up the specified list of hosts (based on [specs]) for the specified compute
 * service.
 *
 * @param serviceDomain The domain name under which the compute service is registered.
 * @param specs A list of [HostSpec] objects describing the simulated hosts to provision.
 * @param optimize A flag to indicate that the CPU resources of the host should be merged into a single CPU resource.
 */
public fun setupHosts(serviceDomain: String, specs: List<HostSpec>, optimize: Boolean = false): ProvisioningStep {
    return HostsProvisioningStep(serviceDomain, specs, optimize)
}
