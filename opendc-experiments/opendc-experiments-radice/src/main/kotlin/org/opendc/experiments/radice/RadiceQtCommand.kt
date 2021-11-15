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

package org.opendc.experiments.radice

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricProducer
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import kotlinx.coroutines.*
import me.tongfei.progressbar.ProgressBarBuilder
import org.apache.commons.math3.distribution.ExponentialDistribution
import org.apache.commons.math3.random.Well19937c
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.api.ServerWatcher
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.SimHost
import org.opendc.experiments.radice.util.ci
import org.opendc.simulator.compute.kernel.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimRuntimeWorkload
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.FlowEngine
import org.opendc.telemetry.compute.*
import org.opendc.telemetry.compute.table.HostTableReader
import org.opendc.telemetry.sdk.toOtelClock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.math.roundToLong

/**
 * A [CliktCommand] for simulating a queuing theory model.
 */
internal class RadiceQtCommand : CliktCommand(name = "qt") {
    /**
     * The seed for seeding the random instances.
     */
    private val seed by option("-s", "--seed", help = "initial seed for randomness")
        .long()
        .default(0)

    /**
     * The arrival rate according to a Poisson process.
     */
    private val lambda by option("--arrival-rate", help = "arrival rate of VMs (per hour)")
        .double()
        .required()

    /**
     * The service rate of VMs with an exponential distribution.
     */
    private val mu by option("--service-rate", help = "service rate of VMs (per hours)")
        .double()
        .required()

    /**
     * The number of servers in the system.
     */
    private val servers by option("--servers", help = "number of servers in the system")
        .int()
        .required()

    /**
     * The number of VMs to submit.
     */
    private val submissions by option("-n", "--submissions", help = "number of VMs to submit")
        .int()
        .default(100_000)

    /**
     * Sample interval for computing metrics.
     */
    private val sampleInterval by option("--sample-interval", help = "sample interval (in hours) for computing metrics")
        .double()
        .default(1.0)

    override fun run() = runBlockingSimulation {
        val pb = ProgressBarBuilder()
            .setInitialMax(submissions.toLong())
            .setTaskName("Simulating...")
            .build()

        val (service, _) = createService(coroutineContext, clock)
        val engine = FlowEngine(coroutineContext, clock)
        val hosts = mutableListOf<SimHost>()
        val hostMetricProducers = mutableListOf<MetricProducer>()

        repeat(servers) {
            val (host, metricProducer) = createHost(coroutineContext, clock, engine, UUID(0, it.toLong()))
            hosts.add(host)
            hostMetricProducers.add(metricProducer)
            service.addHost(host)
        }

        val client = service.newClient()

        // Create new image for the virtual machine
        val image = client.newImage("vm-image")
        val flavor = client.newFlavor("vm-flavor", 1, 1)

        val rng = Well19937c(seed)
        val iat = ExponentialDistribution(rng, 1 / lambda)
        val st = ExponentialDistribution(rng, 1 / mu)

        val waitTime = SummaryStatistics()
        val responseTime = SummaryStatistics()

        val HOURS_TO_MS = 60 * 60 * 1000

        val vmsInQueue = SummaryStatistics()
        val vmsInSystem = SummaryStatistics()
        var serversPending = 0
        var serversActive = 0

        // Helper process for sampling the average number of VMs in the system/queue
        val sampler = launch {
            while (true) {
                vmsInQueue.addValue(serversPending.toDouble())
                vmsInSystem.addValue((serversPending + serversActive).toDouble())
                delay((sampleInterval * HOURS_TO_MS).roundToLong())
            }
        }

        try {
            coroutineScope {
                repeat(submissions) { id ->
                    delay((iat.sample() * HOURS_TO_MS).roundToLong()) // minutes to ms

                    launch {
                        val submitTime = clock.millis()

                        val duration = (st.sample() * HOURS_TO_MS).roundToLong()
                        val workload = SimRuntimeWorkload(duration, utilization = 1.0)

                        serversPending++

                        val server = client.newServer(
                            id.toString(),
                            image,
                            flavor,
                            meta = mapOf("workload" to workload)
                        )

                        suspendCancellableCoroutine<Unit> { cont ->
                            server.watch(object : ServerWatcher {
                                override fun onStateChanged(server: Server, newState: ServerState) {
                                    when (newState) {
                                        ServerState.RUNNING -> {
                                            val startTime = clock.millis()
                                            waitTime.addValue((startTime - submitTime) / HOURS_TO_MS.toDouble())

                                            serversPending--
                                            serversActive++
                                        }
                                        ServerState.TERMINATED -> {
                                            val endTime = clock.millis()
                                            responseTime.addValue((endTime - submitTime) / HOURS_TO_MS.toDouble())

                                            serversActive--

                                            pb.step()
                                            pb.extraMessage = "Response time: %.2f ± %.2f".format(responseTime.mean, responseTime.ci(0.95))

                                            cont.resume(Unit)
                                        }
                                        else -> {}
                                    }
                                }
                            })
                        }

                        server.delete()
                    }
                }
            }

            yield()
        } finally {
            client.close()
            sampler.cancel()
            pb.close()
        }

        val utilization = DescriptiveStatistics()
        val agg = ComputeMetricAggregator()
        for (producer in hostMetricProducers) {
            agg.process(producer.collectAllMetrics())
        }
        agg.collect(object : ComputeMonitor {
            override fun record(reader: HostTableReader) {
                val active = reader.cpuActiveTime
                val total = active + reader.cpuIdleTime
                utilization.addValue(if (total > 0.0) active / total.toDouble() else 0.0)
            }
        })

        val totalDuration = Duration.between(Instant.ofEpochMilli(0), clock.instant())

        echo("Total duration [h]: %.3f".format(totalDuration.toMillis() / HOURS_TO_MS.toDouble()))
        echo("Wait time [h]: %.3f ± %.3f".format(waitTime.mean, waitTime.ci(0.95)))
        echo("Response time [h]: %.3f ± %.3f".format(responseTime.mean, responseTime.ci(0.95)))
        echo("Server utilization: %.3f ± %.3f".format(utilization.mean, utilization.ci(0.95)))
        echo("VMs in System: %.3f ± %.3f".format(vmsInSystem.mean, vmsInSystem.ci(0.95)))
        echo("VMs in Queue: %.3f ± %.3f".format(vmsInQueue.mean, vmsInQueue.ci(0.95)))
    }

    /**
     * Construct a [ComputeService] instance.
     */
    private fun createService(context: CoroutineContext, clock: Clock): Pair<ComputeService, SdkMeterProvider> {
        val scheduler = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(1.0)),
            weighers = emptyList(),
            random = Random(seed)
        )

        val resource = Resource.builder()
            .put(ResourceAttributes.SERVICE_NAME, "opendc-compute")
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setClock(clock.toOtelClock())
            .setResource(resource)
            .build()

        val service = ComputeService(context, clock, meterProvider, scheduler, Duration.ofMillis(1))
        return service to meterProvider
    }

    /**
     * Create a host for this simulation.
     *
     * @return The [SimHost] that has been constructed by the runner.
     */
    private fun createHost(context: CoroutineContext, clock: Clock, engine: FlowEngine, uid: UUID): Pair<SimHost, SdkMeterProvider> {
        val resource = Resource.builder()
            .put(HOST_ID, uid.toString())
            .put(HOST_NAME, "host")
            .put(HOST_ARCH, ResourceAttributes.HostArchValues.AMD64)
            .put(HOST_NCPUS, 1)
            .put(HOST_MEM_CAPACITY, 1)
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setClock(clock.toOtelClock())
            .setResource(resource)
            .build()

        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 1)
        val machineModel = MachineModel(
            cpus = listOf(ProcessingUnit(cpuNode, 0, 1.0)),
            memory = listOf(MemoryUnit("Crucial", "Memory", 1.0, 1))
        )
        val host = SimHost(
            uid,
            "host",
            machineModel,
            emptyMap(),
            context,
            engine,
            meterProvider,
            SimFairShareHypervisorProvider(),
        )

        return host to meterProvider
    }
}
