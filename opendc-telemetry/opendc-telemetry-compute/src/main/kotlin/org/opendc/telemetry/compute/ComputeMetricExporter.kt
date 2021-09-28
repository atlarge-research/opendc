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

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.data.*
import io.opentelemetry.sdk.metrics.export.MetricExporter
import mu.KotlinLogging

/**
 * A [MetricExporter] that redirects data to a [ComputeMonitor] implementation.
 */
public abstract class ComputeMetricExporter : MetricExporter, ComputeMonitor {
    /**
     * The logging instance for this exporter.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * A [ComputeMetricAggregator] that actually performs the aggregation.
     */
    private val agg = ComputeMetricAggregator()

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        return try {
            agg.process(metrics)
            agg.collect(this)

            CompletableResultCode.ofSuccess()
        } catch (e: Throwable) {
            logger.warn(e) { "Failed to export results" }
            CompletableResultCode.ofFailure()
        }
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
