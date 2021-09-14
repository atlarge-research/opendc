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

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricProducer
import org.opendc.telemetry.compute.table.ServiceData

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
    val resultKey = AttributeKey.stringKey("result")
    val stateKey = AttributeKey.stringKey("state")

    var hostsUp = 0
    var hostsDown = 0

    var serversPending = 0
    var serversActive = 0

    var attemptsSuccess = 0
    var attemptsFailure = 0
    var attemptsError = 0

    for (metric in metrics) {
        when (metric.name) {
            "scheduler.hosts" -> {
                for (point in metric.longSumData.points) {
                    when (point.attributes[stateKey]) {
                        "up" -> hostsUp = point.value.toInt()
                        "down" -> hostsDown = point.value.toInt()
                    }
                }
            }
            "scheduler.servers" -> {
                for (point in metric.longSumData.points) {
                    when (point.attributes[stateKey]) {
                        "pending" -> serversPending = point.value.toInt()
                        "active" -> serversActive = point.value.toInt()
                    }
                }
            }
            "scheduler.attempts" -> {
                for (point in metric.longSumData.points) {
                    when (point.attributes[resultKey]) {
                        "success" -> attemptsSuccess = point.value.toInt()
                        "failure" -> attemptsFailure = point.value.toInt()
                        "error" -> attemptsError = point.value.toInt()
                    }
                }
            }
        }
    }

    return ServiceData(timestamp, hostsUp, hostsDown, serversPending, serversActive, attemptsSuccess, attemptsFailure, attemptsError)
}
