/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.web.server.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.runtime.Startup
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.ExecutionState
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.UnitState
import java.util.concurrent.atomic.AtomicLong

private const val REFRESH_INTERVAL = "15s"

/**
 * The state of the queue and the platform, read from Postgres on a timer and held, so a scrape never
 * queries the database. Every replica reports the same numbers, so dashboards take the max.
 *
 * Nothing injects this, so it must start eagerly or the gauges would not exist until a refresh.
 */
@Startup
@ApplicationScoped
class ExecutionGauges(
    registry: MeterRegistry,
    private val dispatcher: Dispatcher,
) {
    private val executions = ExecutionState.entries.associateWith { AtomicLong() }
    private val queuedUnits = AtomicLong()

    init {
        for ((state, value) in executions) {
            Gauge.builder("opendc.executions", value) { it.get().toDouble() }.tag("state", state.name.lowercase()).register(registry)
        }
        Gauge.builder("opendc.units.queued", queuedUnits) { it.get().toDouble() }.register(registry)
        Gauge.builder("opendc.platform.cores.total", dispatcher) { it.capacity().totalCores.toDouble() }.register(registry)
        Gauge.builder("opendc.platform.cores.allocated", dispatcher) { it.capacity().allocatedCores.toDouble() }.register(registry)
    }

    @Scheduled(every = REFRESH_INTERVAL, concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    fun refresh() {
        for ((state, value) in executions) {
            value.set(Execution.count("state = ?1", state))
        }
        queuedUnits.set(RunUnit.count("state = ?1", UnitState.QUEUED))
    }
}
