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

package org.opendc.compute.workload

import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricProducer
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.workload.env.MachineDef
import org.opendc.compute.workload.trace.TraceReader
import org.opendc.simulator.compute.kernel.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.kernel.SimHypervisorProvider
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.telemetry.compute.*
import org.opendc.telemetry.sdk.toOtelClock
import java.time.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

/**
 * Helper class to simulated VM-based workloads in OpenDC.
 */
public class ComputeWorkloadRunner(
    private val context: CoroutineContext,
    private val clock: Clock,
    scheduler: ComputeScheduler,
    machines: List<MachineDef>,
    private val failureModel: FailureModel? = null,
    interferenceModel: VmInterferenceModel? = null,
    hypervisorProvider: SimHypervisorProvider = SimFairShareHypervisorProvider()
) : AutoCloseable {
    /**
     * The [ComputeService] that has been configured by the manager.
     */
    public val service: ComputeService

    /**
     * The [MetricProducer] that are used by the [ComputeService] and the simulated hosts.
     */
    public val producers: List<MetricProducer>
        get() = _metricProducers
    private val _metricProducers = mutableListOf<MetricProducer>()

    /**
     * The [SimResourceInterpreter] to simulate the hosts.
     */
    private val interpreter = SimResourceInterpreter(context, clock)

    /**
     * The hosts that belong to this class.
     */
    private val hosts = mutableSetOf<SimHost>()

    init {
        val (service, serviceMeterProvider) = createService(scheduler)
        this._metricProducers.add(serviceMeterProvider)
        this.service = service

        for (def in machines) {
            val (host, hostMeterProvider) = createHost(def, hypervisorProvider, interferenceModel)
            this._metricProducers.add(hostMeterProvider)
            hosts.add(host)
            this.service.addHost(host)
        }
    }

    /**
     * Run a simulation of the [ComputeService] by replaying the workload trace given by [reader].
     */
    public suspend fun run(reader: TraceReader<SimWorkload>) {
        val injector = failureModel?.createInjector(context, clock, service)
        val client = service.newClient()

        // Create new image for the virtual machine
        val image = client.newImage("vm-image")

        try {
            coroutineScope {
                // Start the fault injector
                injector?.start()

                var offset = Long.MIN_VALUE

                while (reader.hasNext()) {
                    val entry = reader.next()

                    if (offset < 0) {
                        offset = entry.start - clock.millis()
                    }

                    // Make sure the trace entries are ordered by submission time
                    assert(entry.start - offset >= 0) { "Invalid trace order" }
                    delay(max(0, (entry.start - offset) - clock.millis()))

                    launch {
                        val workloadOffset = -offset + 300001
                        val workload = SimTraceWorkload((entry.meta["workload"] as SimTraceWorkload).trace, workloadOffset)

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

                        // Wait for the server reach its end time
                        val endTime = entry.meta["end-time"] as Long
                        delay(endTime + workloadOffset - clock.millis() + 1)

                        // Delete the server after reaching the end-time of the virtual machine
                        server.delete()
                    }
                }
            }

            yield()
        } finally {
            injector?.close()
            reader.close()
            client.close()
        }
    }

    override fun close() {
        service.close()

        for (host in hosts) {
            host.close()
        }

        hosts.clear()
    }

    /**
     * Construct a [ComputeService] instance.
     */
    private fun createService(scheduler: ComputeScheduler): Pair<ComputeService, SdkMeterProvider> {
        val resource = Resource.builder()
            .put(ResourceAttributes.SERVICE_NAME, "opendc-compute")
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setClock(clock.toOtelClock())
            .setResource(resource)
            .build()

        val service = ComputeService(context, clock, meterProvider, scheduler)
        return service to meterProvider
    }

    /**
     * Construct a [SimHost] instance for the specified [MachineDef].
     */
    private fun createHost(
        def: MachineDef,
        hypervisorProvider: SimHypervisorProvider,
        interferenceModel: VmInterferenceModel? = null
    ): Pair<SimHost, SdkMeterProvider> {
        val resource = Resource.builder()
            .put(HOST_ID, def.uid.toString())
            .put(HOST_NAME, def.name)
            .put(HOST_ARCH, ResourceAttributes.HostArchValues.AMD64)
            .put(HOST_NCPUS, def.model.cpus.size)
            .put(HOST_MEM_CAPACITY, def.model.memory.sumOf { it.size })
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setClock(clock.toOtelClock())
            .setResource(resource)
            .build()

        val host = SimHost(
            def.uid,
            def.name,
            def.model,
            def.meta,
            context,
            interpreter,
            meterProvider,
            hypervisorProvider,
            powerDriver = SimplePowerDriver(def.powerModel),
            interferenceDomain = interferenceModel?.newDomain()
        )

        return host to meterProvider
    }
}
