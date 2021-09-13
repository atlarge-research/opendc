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

package org.opendc.telemetry.compute

import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostListener
import org.opendc.compute.service.driver.HostState
import org.opendc.telemetry.compute.table.ServiceData
import org.opendc.telemetry.sdk.metrics.export.CoroutineMetricReader
import java.time.Clock
import java.time.Duration

/**
 * Attach the specified monitor to the OpenDC Compute service.
 */
public suspend fun withMonitor(
    scheduler: ComputeService,
    clock: Clock,
    metricProducer: MetricProducer,
    monitor: ComputeMonitor,
    exportInterval: Duration = Duration.ofMinutes(5), /* Every 5 min (which is the granularity of the workload trace) */
    block: suspend CoroutineScope.() -> Unit
): Unit = coroutineScope {
    // Monitor host events
    for (host in scheduler.hosts) {
        monitor.onStateChange(clock.millis(), host, HostState.UP)
        host.addListener(object : HostListener {
            override fun onStateChanged(host: Host, newState: HostState) {
                monitor.onStateChange(clock.millis(), host, newState)
            }
        })
    }

    val reader = CoroutineMetricReader(
        this,
        listOf(metricProducer),
        ComputeMetricExporter(clock, scheduler.hosts.associateBy { it.uid.toString() }, monitor),
        exportInterval
    )

    try {
        block(this)
    } finally {
        reader.close()
    }
}

/**
 * Collect the metrics of the compute service.
 */
public fun collectServiceMetrics(timestamp: Long, metricProducer: MetricProducer): ServiceData {
    return extractServiceMetrics(timestamp, metricProducer.collectAllMetrics())
}

/**
 * Extract a [ServiceData] object from the specified list of metric data.
 */
public fun extractServiceMetrics(timestamp: Long, metrics: Collection<MetricData>): ServiceData {
    var submittedVms = 0
    var queuedVms = 0
    var unscheduledVms = 0
    var runningVms = 0
    var finishedVms = 0
    var hosts = 0
    var availableHosts = 0

    for (metric in metrics) {
        val points = metric.longSumData.points

        if (points.isEmpty()) {
            continue
        }

        val value = points.first().value.toInt()
        when (metric.name) {
            "servers.submitted" -> submittedVms = value
            "servers.waiting" -> queuedVms = value
            "servers.unscheduled" -> unscheduledVms = value
            "servers.active" -> runningVms = value
            "servers.finished" -> finishedVms = value
            "hosts.total" -> hosts = value
            "hosts.available" -> availableHosts = value
        }
    }

    return ServiceData(timestamp, hosts, availableHosts, submittedVms, runningVms, finishedVms, queuedVms, unscheduledVms)
}
