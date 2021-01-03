/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.sc20.experiment

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.ServerEvent
import org.opendc.compute.core.metal.NODE_CLUSTER
import org.opendc.compute.core.metal.driver.BareMetalDriver
import org.opendc.compute.core.metal.service.ProvisioningService
import org.opendc.compute.core.virt.HypervisorEvent
import org.opendc.compute.core.virt.service.VirtProvisioningEvent
import org.opendc.compute.core.workload.VmWorkload
import org.opendc.compute.simulator.SimBareMetalDriver
import org.opendc.compute.simulator.SimVirtDriver
import org.opendc.compute.simulator.SimVirtProvisioningService
import org.opendc.compute.simulator.allocation.AllocationPolicy
import org.opendc.experiments.sc20.experiment.monitor.ExperimentMonitor
import org.opendc.experiments.sc20.trace.Sc20StreamingParquetTraceReader
import org.opendc.format.environment.EnvironmentReader
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.failures.CorrelatedFaultInjector
import org.opendc.simulator.failures.FailureDomain
import org.opendc.simulator.failures.FaultInjector
import org.opendc.trace.core.EventTracer
import java.io.File
import java.time.Clock
import kotlin.math.ln
import kotlin.math.max
import kotlin.random.Random

/**
 * The logger for this experiment.
 */
private val logger = KotlinLogging.logger {}

/**
 * Construct the failure domain for the experiments.
 */
public suspend fun createFailureDomain(
    coroutineScope: CoroutineScope,
    clock: Clock,
    seed: Int,
    failureInterval: Double,
    bareMetalProvisioner: ProvisioningService,
    chan: Channel<Unit>
): CoroutineScope {
    val job = coroutineScope.launch {
        chan.receive()
        val random = Random(seed)
        val injectors = mutableMapOf<String, FaultInjector>()
        for (node in bareMetalProvisioner.nodes()) {
            val cluster = node.metadata[NODE_CLUSTER] as String
            val injector =
                injectors.getOrPut(cluster) {
                    createFaultInjector(
                        this,
                        clock,
                        random,
                        failureInterval
                    )
                }
            injector.enqueue(node.metadata["driver"] as FailureDomain)
        }
    }
    return CoroutineScope(coroutineScope.coroutineContext + job)
}

/**
 * Obtain the [FaultInjector] to use for the experiments.
 */
public fun createFaultInjector(
    coroutineScope: CoroutineScope,
    clock: Clock,
    random: Random,
    failureInterval: Double
): FaultInjector {
    // Parameters from A. Iosup, A Framework for the Study of Grid Inter-Operation Mechanisms, 2009
    // GRID'5000
    return CorrelatedFaultInjector(
        coroutineScope,
        clock,
        iatScale = ln(failureInterval), iatShape = 1.03, // Hours
        sizeScale = ln(2.0), sizeShape = ln(1.0), // Expect 2 machines, with variation of 1
        dScale = ln(60.0), dShape = ln(60.0 * 8), // Minutes
        random = random
    )
}

/**
 * Create the trace reader from which the VM workloads are read.
 */
public fun createTraceReader(
    path: File,
    performanceInterferenceModel: PerformanceInterferenceModel,
    vms: List<String>,
    seed: Int
): Sc20StreamingParquetTraceReader {
    return Sc20StreamingParquetTraceReader(
        path,
        performanceInterferenceModel,
        vms,
        Random(seed)
    )
}

/**
 * Construct the environment for a VM provisioner and return the provisioner instance.
 */
public suspend fun createProvisioner(
    coroutineScope: CoroutineScope,
    clock: Clock,
    environmentReader: EnvironmentReader,
    allocationPolicy: AllocationPolicy,
    eventTracer: EventTracer
): Pair<ProvisioningService, SimVirtProvisioningService> {
    val environment = environmentReader.use { it.construct(coroutineScope, clock) }
    val bareMetalProvisioner = environment.platforms[0].zones[0].services[ProvisioningService]

    // Wait for the bare metal nodes to be spawned
    delay(10)

    val scheduler = SimVirtProvisioningService(coroutineScope, clock, bareMetalProvisioner, allocationPolicy, eventTracer)

    // Wait for the hypervisors to be spawned
    delay(10)

    return bareMetalProvisioner to scheduler
}

/**
 * Attach the specified monitor to the VM provisioner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public suspend fun attachMonitor(
    coroutineScope: CoroutineScope,
    clock: Clock,
    scheduler: SimVirtProvisioningService,
    monitor: ExperimentMonitor
) {

    val hypervisors = scheduler.drivers()

    // Monitor hypervisor events
    for (hypervisor in hypervisors) {
        // TODO Do not expose VirtDriver directly but use Hypervisor class.
        val server = (hypervisor as SimVirtDriver).server
        monitor.reportHostStateChange(clock.millis(), hypervisor, server)
        server.events
            .onEach { event ->
                val time = clock.millis()
                when (event) {
                    is ServerEvent.StateChanged -> {
                        monitor.reportHostStateChange(time, hypervisor, event.server)
                    }
                }
            }
            .launchIn(coroutineScope)
        hypervisor.events
            .onEach { event ->
                when (event) {
                    is HypervisorEvent.SliceFinished -> monitor.reportHostSlice(
                        clock.millis(),
                        event.requestedBurst,
                        event.grantedBurst,
                        event.overcommissionedBurst,
                        event.interferedBurst,
                        event.cpuUsage,
                        event.cpuDemand,
                        event.numberOfDeployedImages,
                        event.hostServer
                    )
                }
            }
            .launchIn(coroutineScope)

        val driver = hypervisor.server.services[BareMetalDriver.Key] as SimBareMetalDriver
        driver.powerDraw
            .onEach { monitor.reportPowerConsumption(hypervisor.server, it) }
            .launchIn(coroutineScope)
    }

    scheduler.events
        .onEach { event ->
            when (event) {
                is VirtProvisioningEvent.MetricsAvailable ->
                    monitor.reportProvisionerMetrics(clock.millis(), event)
            }
        }
        .launchIn(coroutineScope)
}

/**
 * Process the trace.
 */
public suspend fun processTrace(
    coroutineScope: CoroutineScope,
    clock: Clock,
    reader: TraceReader<VmWorkload>,
    scheduler: SimVirtProvisioningService,
    chan: Channel<Unit>,
    monitor: ExperimentMonitor
) {
    try {
        var submitted = 0

        while (reader.hasNext()) {
            val (time, workload) = reader.next()

            submitted++
            delay(max(0, time - clock.millis()))
            coroutineScope.launch {
                chan.send(Unit)
                val server = scheduler.deploy(
                    workload.image.name,
                    workload.image,
                    Flavor(
                        workload.image.tags["cores"] as Int,
                        workload.image.tags["required-memory"] as Long
                    )
                )
                // Monitor server events
                server.events
                    .onEach {
                        if (it is ServerEvent.StateChanged) {
                            monitor.reportVmStateChange(clock.millis(), it.server)
                        }
                    }
                    .collect()
            }
        }

        scheduler.events
            .takeWhile {
                when (it) {
                    is VirtProvisioningEvent.MetricsAvailable ->
                        it.inactiveVmCount + it.failedVmCount != submitted
                }
            }
            .collect()
        delay(1)
    } finally {
        reader.close()
    }
}
