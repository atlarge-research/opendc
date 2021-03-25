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

package org.opendc.experiments.capelin

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import org.opendc.compute.api.*
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.ComputeServiceEvent
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostEvent
import org.opendc.compute.service.driver.HostListener
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.scheduler.AllocationPolicy
import org.opendc.compute.simulator.SimHost
import org.opendc.experiments.capelin.monitor.ExperimentMonitor
import org.opendc.experiments.capelin.trace.Sc20StreamingParquetTraceReader
import org.opendc.format.environment.EnvironmentReader
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.failures.CorrelatedFaultInjector
import org.opendc.simulator.failures.FaultInjector
import java.io.File
import java.time.Clock
import kotlin.coroutines.resume
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
public fun createFailureDomain(
    coroutineScope: CoroutineScope,
    clock: Clock,
    seed: Int,
    failureInterval: Double,
    service: ComputeService,
    chan: Channel<Unit>
): CoroutineScope {
    val job = coroutineScope.launch {
        chan.receive()
        val random = Random(seed)
        val injectors = mutableMapOf<String, FaultInjector>()
        for (host in service.hosts) {
            val cluster = host.meta["cluster"] as String
            val injector =
                injectors.getOrPut(cluster) {
                    createFaultInjector(
                        this,
                        clock,
                        random,
                        failureInterval
                    )
                }
            injector.enqueue(host as SimHost)
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
 * Construct the environment for a simulated compute service..
 */
public fun createComputeService(
    coroutineScope: CoroutineScope,
    clock: Clock,
    environmentReader: EnvironmentReader,
    allocationPolicy: AllocationPolicy
): ComputeService {
    val hosts = environmentReader
        .use { it.read() }
        .map { def ->
            SimHost(
                def.uid,
                def.name,
                def.model,
                def.meta,
                coroutineScope.coroutineContext,
                clock,
                SimFairShareHypervisorProvider(),
                def.powerModel
            )
        }

    val scheduler =
        ComputeService(coroutineScope.coroutineContext, clock, allocationPolicy)

    for (host in hosts) {
        scheduler.addHost(host)
    }

    return scheduler
}

/**
 * Attach the specified monitor to the VM provisioner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun attachMonitor(
    coroutineScope: CoroutineScope,
    clock: Clock,
    scheduler: ComputeService,
    monitor: ExperimentMonitor
): MonitorResults {
    val results = MonitorResults()
    // Monitor host events
    for (host in scheduler.hosts) {
        monitor.reportHostStateChange(clock.millis(), host, HostState.UP)
        host.addListener(object : HostListener {
            override fun onStateChanged(host: Host, newState: HostState) {
                monitor.reportHostStateChange(clock.millis(), host, newState)
            }
        })

        host.events
            .onEach { event ->
                when (event) {
                    is HostEvent.SliceFinished -> monitor.reportHostSlice(
                        clock.millis(),
                        event.requestedBurst,
                        event.grantedBurst,
                        event.overcommissionedBurst,
                        event.interferedBurst,
                        event.cpuUsage,
                        event.cpuDemand,
                        event.numberOfDeployedImages,
                        event.driver
                    )
                }
            }
            .launchIn(coroutineScope)

        (host as SimHost).machine.powerDraw
            .onEach { monitor.reportPowerConsumption(host, it) }
            .launchIn(coroutineScope)
    }

    scheduler.events
        .onEach { event ->
            when (event) {
                is ComputeServiceEvent.MetricsAvailable -> {
                    results.submittedVms = event.totalVmCount
                    results.queuedVms = event.waitingVmCount
                    results.runningVms = event.activeVmCount
                    results.finishedVms = event.inactiveVmCount
                    results.unscheduledVms = event.failedVmCount
                    monitor.reportProvisionerMetrics(clock.millis(), event)
                }
            }
        }
        .launchIn(coroutineScope)

    return results
}

public class MonitorResults {
    public var submittedVms: Int = 0
    public var queuedVms: Int = 0
    public var runningVms: Int = 0
    public var finishedVms: Int = 0
    public var unscheduledVms: Int = 0
}

/**
 * Process the trace.
 */
public suspend fun processTrace(
    clock: Clock,
    reader: TraceReader<SimWorkload>,
    scheduler: ComputeService,
    chan: Channel<Unit>,
    monitor: ExperimentMonitor
) {
    val client = scheduler.newClient()
    val image = client.newImage("vm-image")
    try {
        coroutineScope {
            while (reader.hasNext()) {
                val entry = reader.next()

                delay(max(0, entry.start - clock.millis()))
                launch {
                    chan.send(Unit)
                    val server = client.newServer(
                        entry.name,
                        image,
                        client.newFlavor(
                            entry.name,
                            entry.meta["cores"] as Int,
                            entry.meta["required-memory"] as Long
                        ),
                        meta = entry.meta
                    )

                    suspendCancellableCoroutine { cont ->
                        server.watch(object : ServerWatcher {
                            override fun onStateChanged(server: Server, newState: ServerState) {
                                monitor.reportVmStateChange(clock.millis(), server, newState)

                                if (newState == ServerState.TERMINATED || newState == ServerState.ERROR) {
                                    cont.resume(Unit)
                                }
                            }
                        })
                    }
                }
            }
        }

        yield()
    } finally {
        reader.close()
        client.close()
    }
}
