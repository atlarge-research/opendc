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

package org.opendc.compute.workload.telemetry

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricProducer
import io.opentelemetry.sdk.metrics.export.MetricReader
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.workload.topology.HostSpec
import org.opendc.telemetry.compute.*
import org.opendc.telemetry.sdk.toOtelClock
import java.time.Clock

/**
 * A [TelemetryManager] using the OpenTelemetry Java SDK.
 */
public class SdkTelemetryManager(private val clock: Clock) : TelemetryManager, AutoCloseable {
    /**
     * The [SdkMeterProvider]s that belong to the workload runner.
     */
    private val _meterProviders = mutableListOf<SdkMeterProvider>()

    /**
     * The internal [MetricProducer] registered with the runner.
     */
    private val _metricProducers = mutableListOf<MetricProducer>()

    /**
     * The list of [MetricReader]s that have been registered with the runner.
     */
    private val _metricReaders = mutableListOf<MetricReader>()

    /**
     * A [MetricProducer] that combines all the other metric producers.
     */
    public val metricProducer: MetricProducer = object : MetricProducer {
        private val producers = _metricProducers

        override fun collectAllMetrics(): Collection<MetricData> = producers.flatMap(MetricProducer::collectAllMetrics)

        override fun toString(): String = "SdkTelemetryManager.AggregateMetricProducer"
    }

    /**
     * Register a [MetricReader] for this manager.
     *
     * @param factory The factory for the reader to register.
     */
    public fun registerMetricReader(factory: MetricReaderFactory) {
        val reader = factory.apply(metricProducer)
        _metricReaders.add(reader)
    }

    override fun createMeterProvider(scheduler: ComputeScheduler): MeterProvider {
        val resource = Resource.builder()
            .put(ResourceAttributes.SERVICE_NAME, "opendc-compute")
            .build()

        return createMeterProvider(resource)
    }

    override fun createMeterProvider(host: HostSpec): MeterProvider {
        val resource = Resource.builder()
            .put(HOST_ID, host.uid.toString())
            .put(HOST_NAME, host.name)
            .put(HOST_ARCH, ResourceAttributes.HostArchValues.AMD64)
            .put(HOST_NCPUS, host.model.cpus.size)
            .put(HOST_MEM_CAPACITY, host.model.memory.sumOf { it.size })
            .build()

        return createMeterProvider(resource)
    }

    /**
     * Construct a [SdkMeterProvider] for the specified [resource].
     */
    private fun createMeterProvider(resource: Resource): SdkMeterProvider {
        val meterProvider = SdkMeterProvider.builder()
            .setClock(clock.toOtelClock())
            .setResource(resource)
            .registerMetricReader { producer ->
                _metricProducers.add(producer)
                object : MetricReader {
                    override fun getPreferredTemporality(): AggregationTemporality = AggregationTemporality.CUMULATIVE
                    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()
                    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
                }
            }
            .build()
        _meterProviders.add(meterProvider)
        return meterProvider
    }

    override fun close() {
        for (meterProvider in _meterProviders) {
            meterProvider.close()
        }

        _meterProviders.clear()

        for (metricReader in _metricReaders) {
            metricReader.shutdown()
        }

        _metricReaders.clear()
        _metricProducers.clear()
    }
}
