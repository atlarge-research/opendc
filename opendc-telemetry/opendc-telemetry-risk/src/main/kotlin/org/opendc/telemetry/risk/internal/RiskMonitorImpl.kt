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

package org.opendc.telemetry.risk.internal

import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.opendc.telemetry.compute.ComputeMetricAggregator
import org.opendc.telemetry.compute.ComputeMonitor
import org.opendc.telemetry.compute.table.HostTableReader
import org.opendc.telemetry.compute.table.ServerTableReader
import org.opendc.telemetry.compute.table.ServiceTableReader
import org.opendc.telemetry.risk.RiskFactor
import org.opendc.telemetry.risk.RiskMonitor
import org.opendc.telemetry.risk.RiskMonitorListener
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Internal implementation of [RiskMonitor].
 *
 * @param context The [CoroutineContext] to run the monitor in.
 * @param clock The [Clock] instance used to track time.
 * @param producers The list of metric producers.
 * @param factors The list of risk factors to consider.
 */
internal class RiskMonitorImpl(
    context: CoroutineContext,
    private val clock: Clock,
    private val producers: List<MetricProducer>,
    factors: List<RiskFactor>
) : RiskMonitor, ComputeMonitor {
    /**
     * The logger instance for this monitor.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The scope in which the monitor runs.
     */
    private val scope = CoroutineScope(context + Job())

    /**
     * The [ComputeMetricAggregator] to aggregate the metrics reported by the simulator.
     */
    private val agg = ComputeMetricAggregator()

    /**
     * The metric reader job.
     */
    private var readerJob: Job? = null

    /**
     * The state per risk factor.
     */
    private val factorState = factors.associateWith { RiskFactorState(it) }

    /**
     * The listeners of this monitor.
     */
    private val listeners = mutableListOf<RiskMonitorListener>()

    override fun start() {
        if (readerJob != null) {
            return
        }

        readerJob = scope.launch {
            var now = clock.instant()
            var minTarget = Instant.MAX

            // Initialize all targets
            for ((_, state) in factorState) {
                state.updateTarget(clock, now)
                minTarget = minOf(minTarget, state.target)
            }

            while (true) {
                try {
                    delay(Duration.between(now, minTarget).toMillis())
                } catch (e: Throwable) {
                    flushRisks(clock.instant(), flushAll = true)
                    throw e
                }

                now = clock.instant()
                minTarget = flushRisks(now)

                logger.trace { "Next risk evaluation at $minTarget" }
            }
        }
    }

    override fun addListener(listener: RiskMonitorListener) {
        listeners += listener
    }

    override fun removeListener(listener: RiskMonitorListener) {
        listeners -= listener
    }

    override fun record(reader: ServiceTableReader) {
        for ((factor, _) in factorState) {
            factor.indicator.record(reader)
        }
    }

    override fun record(reader: ServerTableReader) {
        for ((factor, _) in factorState) {
            factor.indicator.record(reader)
        }
    }

    override fun record(reader: HostTableReader) {
        for ((factor, _) in factorState) {
            factor.indicator.record(reader)
        }
    }

    /**
     * Flush all the risk states.
     */
    private fun flushRisks(now: Instant, flushAll: Boolean = false): Instant {
        val listeners = listeners

        for (producer in producers) {
            agg.process(producer.collectAllMetrics())
        }
        agg.collect(this@RiskMonitorImpl)

        var minTarget = Instant.MAX

        for ((_, state) in factorState) {
            if (state.shouldEvaluate(now) || flushAll) {
                logger.debug { "Evaluating ${state.factor.id}" }

                try {
                    state.evaluate(now, listeners)
                } catch (e: Throwable) {
                    logger.error(e) { "Failed to evaluate risk factor ${state.factor.id}" }
                }

                state.updateTarget(clock, now)
            }

            minTarget = minOf(minTarget, state.target)
        }

        return minTarget
    }

    /**
     * Stop the monitoring process.
     */
    private fun stop() {
        readerJob?.cancel()
        readerJob = null
    }

    override fun close() {
        stop()
        scope.cancel()
    }
}
