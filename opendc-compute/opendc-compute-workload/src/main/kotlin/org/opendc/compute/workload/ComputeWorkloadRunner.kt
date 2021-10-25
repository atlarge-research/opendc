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
import org.opendc.compute.workload.topology.HostSpec
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.flow.FlowEngine
import org.opendc.telemetry.compute.*
import org.opendc.telemetry.sdk.toOtelClock
import java.time.Clock
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

/**
 * Helper class to simulated VM-based workloads in OpenDC.
 *
 * @param context [CoroutineContext] to run the simulation in.
 * @param clock [Clock] instance tracking simulation time.
 * @param scheduler [ComputeScheduler] implementation to use for the service.
 * @param failureModel A failure model to use for injecting failures.
 * @param interferenceModel The model to use for performance interference.
 */
public class ComputeWorkloadRunner(
    private val context: CoroutineContext,
    private val clock: Clock,
    scheduler: ComputeScheduler,
    private val failureModel: FailureModel? = null,
    private val interferenceModel: VmInterferenceModel? = null,
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
     * The [FlowEngine] to simulate the hosts.
     */
    private val engine = FlowEngine(context, clock)

    /**
     * The hosts that belong to this class.
     */
    private val hosts = mutableSetOf<SimHost>()

    init {
        val (service, serviceMeterProvider) = createService(scheduler)
        this._metricProducers.add(serviceMeterProvider)
        this.service = service
    }

    /**
     * Converge a simulation of the [ComputeService] by replaying the workload trace given by [trace].
     */
    public suspend fun run(trace: List<VirtualMachine>, seed: Long) {
        val random = Random(seed)
        val injector = failureModel?.createInjector(context, clock, service, random)
        val client = service.newClient()

        // Create new image for the virtual machine
        val image = client.newImage("vm-image")

        try {
            coroutineScope {
                // Start the fault injector
                injector?.start()

                var offset = Long.MIN_VALUE

                for (entry in trace.sortedBy { it.startTime }) {
                    val now = clock.millis()
                    val start = entry.startTime.toEpochMilli()

                    if (offset < 0) {
                        offset = start - now
                    }

                    // Make sure the trace entries are ordered by submission time
                    assert(start - offset >= 0) { "Invalid trace order" }
                    delay(max(0, (start - offset) - now))

                    launch {
                        val workloadOffset = -offset + 300001
                        val workload = SimTraceWorkload(entry.trace, workloadOffset)

                        val server = client.newServer(
                            entry.name,
                            image,
                            client.newFlavor(
                                entry.name,
                                entry.cpuCount,
                                entry.memCapacity,
                                meta = if (entry.cpuCapacity > 0.0) mapOf("cpu-capacity" to entry.cpuCapacity) else emptyMap()
                            ),
                            meta = mapOf("workload" to workload)
                        )

                        // Wait for the server reach its end time
                        val endTime = entry.stopTime.toEpochMilli()
                        delay(endTime + workloadOffset - clock.millis() + 5 * 60 * 1000)

                        // Delete the server after reaching the end-time of the virtual machine
                        server.delete()
                    }
                }
            }

            yield()
        } finally {
            injector?.close()
            client.close()
        }
    }

    /**
     * Register a host for this simulation.
     *
     * @param spec The definition of the host.
     * @param optimize Merge the CPU resources of the host into a single CPU resource.
     * @return The [SimHost] that has been constructed by the runner.
     */
    public fun registerHost(spec: HostSpec, optimize: Boolean = false): SimHost {
        val resource = Resource.builder()
            .put(HOST_ID, spec.uid.toString())
            .put(HOST_NAME, spec.name)
            .put(HOST_ARCH, ResourceAttributes.HostArchValues.AMD64)
            .put(HOST_NCPUS, spec.model.cpus.size)
            .put(HOST_MEM_CAPACITY, spec.model.memory.sumOf { it.size })
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setClock(clock.toOtelClock())
            .setResource(resource)
            .build()
        _metricProducers.add(meterProvider)

        val host = SimHost(
            spec.uid,
            spec.name,
            spec.model,
            spec.meta,
            context,
            engine,
            meterProvider,
            spec.hypervisor,
            powerDriver = spec.powerDriver,
            interferenceDomain = interferenceModel?.newDomain(),
            optimize = optimize
        )

        hosts.add(host)
        service.addHost(host)

        return host
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
}
