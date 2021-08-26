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

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.opendc.compute.api.*
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostListener
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.ReplayScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.service.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.service.scheduler.weights.RamWeigher
import org.opendc.compute.service.scheduler.weights.VCpuWeigher
import org.opendc.compute.simulator.SimHost
import org.opendc.experiments.capelin.env.EnvironmentReader
import org.opendc.experiments.capelin.monitor.ExperimentMetricExporter
import org.opendc.experiments.capelin.monitor.ExperimentMonitor
import org.opendc.experiments.capelin.trace.TraceReader
import org.opendc.simulator.compute.kernel.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.failures.CorrelatedFaultInjector
import org.opendc.simulator.failures.FaultInjector
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.telemetry.sdk.metrics.export.CoroutineMetricReader
import org.opendc.telemetry.sdk.toOtelClock
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
fun createFailureDomain(
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
fun createFaultInjector(
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
 * Construct the environment for a simulated compute service..
 */
suspend fun withComputeService(
    clock: Clock,
    meterProvider: MeterProvider,
    environmentReader: EnvironmentReader,
    scheduler: ComputeScheduler,
    interferenceModel: VmInterferenceModel? = null,
    block: suspend CoroutineScope.(ComputeService) -> Unit
): Unit = coroutineScope {
    val interpreter = SimResourceInterpreter(coroutineContext, clock)
    val hosts = environmentReader
        .use { it.read() }
        .map { def ->
            SimHost(
                def.uid,
                def.name,
                def.model,
                def.meta,
                coroutineContext,
                interpreter,
                meterProvider.get("opendc-compute-simulator"),
                SimFairShareHypervisorProvider(),
                powerDriver = SimplePowerDriver(def.powerModel),
                interferenceDomain = interferenceModel?.newDomain()
            )
        }

    val serviceMeter = meterProvider.get("opendc-compute")
    val service =
        ComputeService(coroutineContext, clock, serviceMeter, scheduler)

    for (host in hosts) {
        service.addHost(host)
    }

    try {
        block(this, service)
    } finally {
        service.close()
        hosts.forEach(SimHost::close)
    }
}

/**
 * Attach the specified monitor to the VM provisioner.
 */
suspend fun withMonitor(
    monitor: ExperimentMonitor,
    clock: Clock,
    metricProducer: MetricProducer,
    scheduler: ComputeService,
    block: suspend CoroutineScope.() -> Unit
): Unit = coroutineScope {
    // Monitor host events
    for (host in scheduler.hosts) {
        monitor.reportHostStateChange(clock.millis(), host, HostState.UP)
        host.addListener(object : HostListener {
            override fun onStateChanged(host: Host, newState: HostState) {
                monitor.reportHostStateChange(clock.millis(), host, newState)
            }
        })
    }

    val reader = CoroutineMetricReader(
        this,
        listOf(metricProducer),
        ExperimentMetricExporter(monitor, clock, scheduler.hosts.associateBy { it.uid.toString() }),
        exportInterval = 5L * 60 * 1000 /* Every 5 min (which is the granularity of the workload trace) */
    )

    try {
        block(this)
    } finally {
        reader.close()
        monitor.close()
    }
}

class ComputeMetrics {
    var submittedVms: Int = 0
    var queuedVms: Int = 0
    var runningVms: Int = 0
    var unscheduledVms: Int = 0
    var finishedVms: Int = 0
    var hosts: Int = 0
    var availableHosts = 0
}

/**
 * Collect the metrics of the compute service.
 */
fun collectMetrics(metricProducer: MetricProducer): ComputeMetrics {
    return extractComputeMetrics(metricProducer.collectAllMetrics())
}

/**
 * Extract an [ComputeMetrics] object from the specified list of metric data.
 */
internal fun extractComputeMetrics(metrics: Collection<MetricData>): ComputeMetrics {
    val res = ComputeMetrics()
    for (metric in metrics) {
        val points = metric.longSumData.points

        if (points.isEmpty()) {
            continue
        }

        val value = points.first().value.toInt()
        when (metric.name) {
            "servers.submitted" -> res.submittedVms = value
            "servers.waiting" -> res.queuedVms = value
            "servers.unscheduled" -> res.unscheduledVms = value
            "servers.active" -> res.runningVms = value
            "servers.finished" -> res.finishedVms = value
            "hosts.total" -> res.hosts = value
            "hosts.available" -> res.availableHosts = value
        }
    }

    return res
}

/**
 * Process the trace.
 */
suspend fun processTrace(
    clock: Clock,
    reader: TraceReader<SimWorkload>,
    scheduler: ComputeService,
    chan: Channel<Unit>,
    monitor: ExperimentMonitor? = null,
) {
    val client = scheduler.newClient()
    val image = client.newImage("vm-image")
    var offset = Long.MIN_VALUE
    try {
        coroutineScope {
            while (reader.hasNext()) {
                val entry = reader.next()

                if (offset < 0) {
                    offset = entry.start - clock.millis()
                }

                // Make sure the trace entries are ordered by submission time
                assert(entry.start - offset >= 0) { "Invalid trace order" }
                delay(max(0, (entry.start - offset) - clock.millis()))
                launch {
                    chan.send(Unit)
                    val workload = SimTraceWorkload((entry.meta["workload"] as SimTraceWorkload).trace, offset = -offset + 300001)
                    val server = client.newServer(
                        entry.name,
                        image,
                        client.newFlavor(
                            entry.name,
                            entry.meta["cores"] as Int,
                            entry.meta["required-memory"] as Long
                        ),
                        meta = entry.meta + mapOf("workload" to workload)
                    )

                    suspendCancellableCoroutine { cont ->
                        server.watch(object : ServerWatcher {
                            override fun onStateChanged(server: Server, newState: ServerState) {
                                monitor?.reportVmStateChange(clock.millis(), server, newState)

                                if (newState == ServerState.TERMINATED) {
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

/**
 * Create a [MeterProvider] instance for the experiment.
 */
fun createMeterProvider(clock: Clock): MeterProvider {
    return SdkMeterProvider
        .builder()
        .setClock(clock.toOtelClock())
        .build()
}

/**
 * Create a [ComputeScheduler] for the experiment.
 */
fun createComputeScheduler(allocationPolicy: String, seeder: Random, vmPlacements: Map<String, String> = emptyMap()): ComputeScheduler {
    val cpuAllocationRatio = 16.0
    val ramAllocationRatio = 1.5
    return when (allocationPolicy) {
        "mem" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(RamWeigher(multiplier = 1.0))
        )
        "mem-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(RamWeigher(multiplier = -1.0))
        )
        "core-mem" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        "core-mem-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(CoreRamWeigher(multiplier = -1.0))
        )
        "active-servers" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(InstanceCountWeigher(multiplier = -1.0))
        )
        "active-servers-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(InstanceCountWeigher(multiplier = 1.0))
        )
        "provisioned-cores" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = 1.0))
        )
        "provisioned-cores-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = -1.0))
        )
        "random" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = emptyList(),
            subsetSize = Int.MAX_VALUE,
            random = java.util.Random(seeder.nextLong())
        )
        "replay" -> ReplayScheduler(vmPlacements)
        else -> throw IllegalArgumentException("Unknown policy $allocationPolicy")
    }
}
