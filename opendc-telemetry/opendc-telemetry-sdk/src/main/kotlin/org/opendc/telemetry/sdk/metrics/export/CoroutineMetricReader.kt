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

package org.opendc.telemetry.sdk.metrics.export

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.MetricProducer
import io.opentelemetry.sdk.metrics.export.MetricReader
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.Duration
import java.util.*

/**
 * A helper class to read the metrics from a list of [MetricProducer]s and automatically export the metrics every
 * export interval.
 *
 * The reader runs in a [CoroutineScope] which enables collection of metrics in environments with a custom clock.
 *
 * @param scope The [CoroutineScope] to run the reader in.
 * @param producer The metric producer to gather metrics from.
 * @param exporter The export to export the metrics to.
 * @param exportInterval The export interval.
 */
public class CoroutineMetricReader private constructor(
    scope: CoroutineScope,
    private val producer: MetricProducer,
    private val exporter: MetricExporter,
    private val exportInterval: Duration
) : MetricReader {
    private val logger = KotlinLogging.logger {}

    /**
     * The background job that is responsible for collecting the metrics every cycle.
     */
    private val job = scope.launch {
        val intervalMs = exportInterval.toMillis()

        try {
            while (isActive) {
                delay(intervalMs)

                try {
                    val metrics = producer.collectAllMetrics()
                    val result = exporter.export(metrics)
                    result.whenComplete {
                        if (!result.isSuccess) {
                            logger.warn { "Exporter failed" }
                        }
                    }
                } catch (cause: Throwable) {
                    logger.warn(cause) { "Exporter threw an Exception" }
                }
            }
        } finally {
            exporter.shutdown()
        }
    }

    override fun getPreferredTemporality(): AggregationTemporality = AggregationTemporality.CUMULATIVE

    override fun flush(): CompletableResultCode {
        return exporter.flush()
    }

    override fun shutdown(): CompletableResultCode {
        job.cancel()
        return CompletableResultCode.ofSuccess()
    }

    public companion object {
        /**
         * Construct a [MetricReaderFactory] for this metric reader.
         */
        public operator fun invoke(
            scope: CoroutineScope,
            exporter: MetricExporter,
            exportInterval: Duration = Duration.ofMinutes(5)
        ): MetricReaderFactory {
            return MetricReaderFactory { producer ->
                CoroutineMetricReader(scope, producer, exporter, exportInterval)
            }
        }
    }
}
