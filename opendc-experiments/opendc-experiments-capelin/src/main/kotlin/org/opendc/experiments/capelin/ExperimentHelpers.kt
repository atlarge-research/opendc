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
import io.opentelemetry.sdk.metrics.aggregator.AggregatorFactory
import io.opentelemetry.sdk.metrics.common.InstrumentType
import io.opentelemetry.sdk.metrics.export.MetricProducer
import io.opentelemetry.sdk.metrics.view.InstrumentSelector
import io.opentelemetry.sdk.metrics.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.opendc.compute.api.*
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostListener
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.simulator.SimHost
import org.opendc.experiments.capelin.monitor.ExperimentMetricExporter
import org.opendc.experiments.capelin.monitor.ExperimentMonitor
import org.opendc.experiments.capelin.trace.Sc20StreamingParquetTraceReader
import org.opendc.format.environment.EnvironmentReader
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.kernel.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.failures.CorrelatedFaultInjector
import org.opendc.simulator.failures.FaultInjector
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.telemetry.sdk.metrics.export.CoroutineMetricReader
import org.opendc.telemetry.sdk.toOtelClock
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
public suspend fun withComputeService(
    clock: Clock,
    meterProvider: MeterProvider,
    environmentReader: EnvironmentReader,
    scheduler: ComputeScheduler,
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
                def.powerModel
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
@OptIn(ExperimentalCoroutinesApi::class)
public suspend fun withMonitor(
    monitor: ExperimentMonitor,
    clock: Clock,
    metricProducer: MetricProducer,
    scheduler: ComputeService,
    block: suspend CoroutineScope.() -> Unit
): Unit = coroutineScope {
    val monitorJobs = mutableSetOf<Job>()

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
        exportInterval = 5 * 60 * 1000 /* Every 5 min (which is the granularity of the workload trace) */
    )

    try {
        block(this)
    } finally {
        monitorJobs.forEach(Job::cancel)
        reader.close()
        monitor.close()
    }
}

public class ComputeMetrics {
    public var submittedVms: Int = 0
    public var queuedVms: Int = 0
    public var runningVms: Int = 0
    public var unscheduledVms: Int = 0
    public var finishedVms: Int = 0
}

/**
 * Collect the metrics of the compute service.
 */
public fun collectMetrics(metricProducer: MetricProducer): ComputeMetrics {
    val metrics = metricProducer.collectAllMetrics().associateBy { it.name }
    val res = ComputeMetrics()
    try {
        // Hack to extract metrics from OpenTelemetry SDK
        res.submittedVms = metrics["servers.submitted"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        res.queuedVms = metrics["servers.waiting"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        res.unscheduledVms = metrics["servers.unscheduled"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        res.runningVms = metrics["servers.active"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        res.finishedVms = metrics["servers.finished"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
    } catch (cause: Throwable) {
        logger.warn(cause) { "Failed to collect metrics" }
    }
    return res
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
    var offset = Long.MIN_VALUE
    try {
        coroutineScope {
            while (reader.hasNext()) {
                val entry = reader.next()

                if (offset < 0) {
                    offset = entry.start - clock.millis()
                }

                delay(max(0, (entry.start - offset) - clock.millis()))
                launch {
                    chan.send(Unit)
                    val workload = SimTraceWorkload((entry.meta["workload"] as SimTraceWorkload).trace)
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

/**
 * Create a [MeterProvider] instance for the experiment.
 */
public fun createMeterProvider(clock: Clock): MeterProvider {
    val powerSelector = InstrumentSelector.builder()
        .setInstrumentNameRegex("power\\.usage")
        .setInstrumentType(InstrumentType.VALUE_RECORDER)
        .build()
    val powerView = View.builder()
        .setAggregatorFactory(AggregatorFactory.lastValue())
        .build()

    return SdkMeterProvider
        .builder()
        .setClock(clock.toOtelClock())
        .registerView(powerSelector, powerView)
        .build()
}
