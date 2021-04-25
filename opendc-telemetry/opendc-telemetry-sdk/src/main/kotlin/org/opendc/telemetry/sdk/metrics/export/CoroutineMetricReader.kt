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

import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A helper class to read the metrics from a list of [MetricProducer]s and automatically export the metrics every
 * export interval.
 *
 * The reader runs in a [CoroutineScope] which enables collection of metrics in environments with a custom clock.
 *
 * @param scope The [CoroutineScope] to run the reader in.
 * @param producers The metric producers to gather metrics from.
 * @param exporter The export to export the metrics to.
 * @param exportInterval The export interval in milliseconds.
 */
public class CoroutineMetricReader(
    scope: CoroutineScope,
    private val producers: List<MetricProducer>,
    private val exporter: MetricExporter,
    private val exportInterval: Long = 60_000
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val chan = Channel<List<MetricData>>(Channel.RENDEZVOUS)

    /**
     * The metric reader job.
     */
    private val readerJob = scope.launch {
        while (isActive) {
            delay(exportInterval)

            val metrics = mutableListOf<MetricData>()
            for (producer in producers) {
                metrics.addAll(producer.collectAllMetrics())
            }
            chan.send(Collections.unmodifiableList(metrics))
        }
    }

    /**
     * The exporter job runs in the background to actually export the metrics.
     */
    private val exporterJob = chan.consumeAsFlow()
        .onEach { metrics ->
            suspendCoroutine<Unit> { cont ->
                try {
                    val result = exporter.export(metrics)
                    result.whenComplete {
                        if (!result.isSuccess) {
                            logger.trace { "Exporter failed" }
                        }
                        cont.resume(Unit)
                    }
                } catch (cause: Throwable) {
                    logger.warn(cause) { "Exporter threw an Exception" }
                    cont.resume(Unit)
                }
            }
        }
        .launchIn(scope)

    override fun close() {
        readerJob.cancel()
        exporterJob.cancel()
    }
}
