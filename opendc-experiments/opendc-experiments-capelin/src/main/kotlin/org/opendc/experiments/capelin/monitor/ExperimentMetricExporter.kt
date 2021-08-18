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

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import org.opendc.compute.service.driver.Host
import java.time.Clock

/**
 * A [MetricExporter] that exports the metrics to the [ExperimentMonitor].
 */
public class ExperimentMetricExporter(
    private val monitor: ExperimentMonitor,
    private val clock: Clock,
    private val hosts: Map<String, Host>
) : MetricExporter {
    private val hostKey = AttributeKey.stringKey("host")

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        val metricsByName = metrics.associateBy { it.name }
        reportHostMetrics(metricsByName)
        reportProvisionerMetrics(metricsByName)
        return CompletableResultCode.ofSuccess()
    }

    private fun reportHostMetrics(metrics: Map<String, MetricData>) {
        val hostMetrics = mutableMapOf<String, HostMetrics>()
        hosts.mapValuesTo(hostMetrics) { HostMetrics() }

        mapDoubleSummary(metrics["cpu.demand"], hostMetrics) { m, v ->
            m.cpuDemand = v
        }

        mapDoubleSummary(metrics["cpu.usage"], hostMetrics) { m, v ->
            m.cpuUsage = v
        }

        mapDoubleGauge(metrics["power.usage"], hostMetrics) { m, v ->
            m.powerDraw = v
        }

        mapDoubleSummary(metrics["cpu.work.total"], hostMetrics) { m, v ->
            m.requestedBurst = v.toLong()
        }

        mapDoubleSummary(metrics["cpu.work.granted"], hostMetrics) { m, v ->
            m.grantedBurst = v.toLong()
        }

        mapDoubleSummary(metrics["cpu.work.overcommit"], hostMetrics) { m, v ->
            m.overcommissionedBurst = v.toLong()
        }

        mapDoubleSummary(metrics["cpu.work.interference"], hostMetrics) { m, v ->
            m.interferedBurst = v.toLong()
        }

        mapLongSum(metrics["guests.active"], hostMetrics) { m, v ->
            m.numberOfDeployedImages = v.toInt()
        }

        for ((id, hostMetric) in hostMetrics) {
            val host = hosts.getValue(id)
            monitor.reportHostSlice(
                clock.millis(),
                hostMetric.requestedBurst,
                hostMetric.grantedBurst,
                hostMetric.overcommissionedBurst,
                hostMetric.interferedBurst,
                hostMetric.cpuUsage,
                hostMetric.cpuDemand,
                hostMetric.powerDraw,
                hostMetric.numberOfDeployedImages,
                host
            )
        }
    }

    private fun mapDoubleSummary(data: MetricData?, hostMetrics: MutableMap<String, HostMetrics>, block: (HostMetrics, Double) -> Unit) {
        val points = data?.doubleSummaryData?.points ?: emptyList()
        for (point in points) {
            val uid = point.attributes[hostKey]
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
            val uid = point.attributes[hostKey]
            val hostMetric = hostMetrics[uid]

            if (hostMetric != null) {
                block(hostMetric, point.value)
            }
        }
    }

    private fun mapLongSum(data: MetricData?, hostMetrics: MutableMap<String, HostMetrics>, block: (HostMetrics, Long) -> Unit) {
        val points = data?.longSumData?.points ?: emptyList()
        for (point in points) {
            val uid = point.attributes[hostKey]
            val hostMetric = hostMetrics[uid]

            if (hostMetric != null) {
                block(hostMetric, point.value)
            }
        }
    }

    private fun reportProvisionerMetrics(metrics: Map<String, MetricData>) {
        val submittedVms = metrics["servers.submitted"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        val queuedVms = metrics["servers.waiting"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        val unscheduledVms = metrics["servers.unscheduled"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        val runningVms = metrics["servers.active"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        val finishedVms = metrics["servers.finished"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        val hosts = metrics["hosts.total"]?.longSumData?.points?.last()?.value?.toInt() ?: 0
        val availableHosts = metrics["hosts.available"]?.longSumData?.points?.last()?.value?.toInt() ?: 0

        monitor.reportProvisionerMetrics(
            clock.millis(),
            hosts,
            availableHosts,
            submittedVms,
            runningVms,
            finishedVms,
            queuedVms,
            unscheduledVms
        )
    }

    private class HostMetrics {
        var requestedBurst: Long = 0
        var grantedBurst: Long = 0
        var overcommissionedBurst: Long = 0
        var interferedBurst: Long = 0
        var cpuUsage: Double = 0.0
        var cpuDemand: Double = 0.0
        var numberOfDeployedImages: Int = 0
        var powerDraw: Double = 0.0
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
