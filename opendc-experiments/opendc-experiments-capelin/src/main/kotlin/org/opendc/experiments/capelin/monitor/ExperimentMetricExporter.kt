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

package org.opendc.experiments.capelin.monitor

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.opendc.compute.service.driver.Host
import org.opendc.experiments.capelin.extractComputeMetrics
import java.time.Clock

/**
 * A [MetricExporter] that exports the metrics to the [ExperimentMonitor].
 */
public class ExperimentMetricExporter(
    private val monitor: ExperimentMonitor,
    private val clock: Clock,
    private val hosts: Map<String, Host>
) : MetricExporter {

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        return try {
            reportHostMetrics(metrics)
            reportProvisionerMetrics(metrics)
            CompletableResultCode.ofSuccess()
        } catch (e: Throwable) {
            CompletableResultCode.ofFailure()
        }
    }

    private var lastHostMetrics: Map<String, HostMetrics> = emptyMap()
    private val hostMetricsSingleton = HostMetrics()

    private fun reportHostMetrics(metrics: Collection<MetricData>) {
        val hostMetrics = mutableMapOf<String, HostMetrics>()
        hosts.mapValuesTo(hostMetrics) { HostMetrics() }

        for (metric in metrics) {
            when (metric.name) {
                "cpu.demand" -> mapDoubleSummary(metric, hostMetrics) { m, v -> m.cpuDemand = v }
                "cpu.usage" -> mapDoubleSummary(metric, hostMetrics) { m, v -> m.cpuUsage = v }
                "power.usage" -> mapDoubleGauge(metric, hostMetrics) { m, v -> m.powerDraw = v }
                "cpu.work.total" -> mapDoubleSum(metric, hostMetrics) { m, v -> m.totalWork = v }
                "cpu.work.granted" -> mapDoubleSum(metric, hostMetrics) { m, v -> m.grantedWork = v }
                "cpu.work.overcommit" -> mapDoubleSum(metric, hostMetrics) { m, v -> m.overcommittedWork = v }
                "cpu.work.interference" -> mapDoubleSum(metric, hostMetrics) { m, v -> m.interferedWork = v }
                "guests.active" -> mapLongSum(metric, hostMetrics) { m, v -> m.instanceCount = v.toInt() }
            }
        }

        for ((id, hostMetric) in hostMetrics) {
            val lastHostMetric = lastHostMetrics.getOrDefault(id, hostMetricsSingleton)
            val host = hosts.getValue(id)
            monitor.reportHostData(
                clock.millis(),
                hostMetric.totalWork - lastHostMetric.totalWork,
                hostMetric.grantedWork - lastHostMetric.grantedWork,
                hostMetric.overcommittedWork - lastHostMetric.overcommittedWork,
                hostMetric.interferedWork - lastHostMetric.interferedWork,
                hostMetric.cpuUsage,
                hostMetric.cpuDemand,
                hostMetric.powerDraw,
                hostMetric.instanceCount,
                host
            )
        }

        lastHostMetrics = hostMetrics
    }

    private fun mapDoubleSummary(data: MetricData, hostMetrics: MutableMap<String, HostMetrics>, block: (HostMetrics, Double) -> Unit) {
        val points = data.doubleSummaryData?.points ?: emptyList()
        for (point in points) {
            val uid = point.attributes[ResourceAttributes.HOST_ID]
            val hostMetric = hostMetrics[uid]

            if (hostMetric != null) {
                // Take the average of the summary
                val avg = (point.percentileValues[0].value + point.percentileValues[1].value) / 2
                block(hostMetric, avg)
            }
        }
    }

    private fun mapDoubleGauge(data: MetricData?, hostMetrics: MutableMap<String, HostMetrics>, block: (HostMetrics, Double) -> Unit) {
        val points = data?.doubleGaugeData?.points ?: emptyList()
        for (point in points) {
            val uid = point.attributes[ResourceAttributes.HOST_ID]
            val hostMetric = hostMetrics[uid]

            if (hostMetric != null) {
                block(hostMetric, point.value)
            }
        }
    }

    private fun mapLongSum(data: MetricData?, hostMetrics: MutableMap<String, HostMetrics>, block: (HostMetrics, Long) -> Unit) {
        val points = data?.longSumData?.points ?: emptyList()
        for (point in points) {
            val uid = point.attributes[ResourceAttributes.HOST_ID]
            val hostMetric = hostMetrics[uid]

            if (hostMetric != null) {
                block(hostMetric, point.value)
            }
        }
    }

    private fun mapDoubleSum(data: MetricData?, hostMetrics: MutableMap<String, HostMetrics>, block: (HostMetrics, Double) -> Unit) {
        val points = data?.doubleSumData?.points ?: emptyList()
        for (point in points) {
            val uid = point.attributes[ResourceAttributes.HOST_ID]
            val hostMetric = hostMetrics[uid]

            if (hostMetric != null) {
                block(hostMetric, point.value)
            }
        }
    }

    private fun reportProvisionerMetrics(metrics: Collection<MetricData>) {
        val res = extractComputeMetrics(metrics)

        monitor.reportServiceData(
            clock.millis(),
            res.hosts,
            res.availableHosts,
            res.submittedVms,
            res.runningVms,
            res.finishedVms,
            res.queuedVms,
            res.unscheduledVms
        )
    }

    private class HostMetrics {
        var totalWork: Double = 0.0
        var grantedWork: Double = 0.0
        var overcommittedWork: Double = 0.0
        var interferedWork: Double = 0.0
        var cpuUsage: Double = 0.0
        var cpuDemand: Double = 0.0
        var instanceCount: Int = 0
        var powerDraw: Double = 0.0
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
