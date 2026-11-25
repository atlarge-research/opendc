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

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Launch

/** How a launch went, as a closed set of tag values. */
enum class LaunchOutcome {
    ACCEPTED,
    REJECTED,
    UNAVAILABLE,
    ;

    companion object {
        fun of(launch: Launch): LaunchOutcome =
            when (launch) {
                Launch.Accepted -> ACCEPTED
                is Launch.Rejected -> REJECTED
                is Launch.Unavailable -> UNAVAILABLE
            }
    }
}

/**
 * What the execution loop does, counted. Every tag value is registered up front, so a dashboard
 * shows a zero rather than no data, and starts eagerly so the counters exist before the first launch.
 */
@Startup
@ApplicationScoped
class ServerMetrics(registry: MeterRegistry) {
    private val launches =
        LaunchOutcome.entries.associateWith {
            Counter.builder("opendc.executions.launched").tag("outcome", it.name.lowercase()).register(registry)
        }

    private val settlements =
        ExitReason.entries.associateWith {
            Counter.builder("opendc.executions.settled").tag("reason", it.name.lowercase()).register(registry)
        }

    fun launched(launch: Launch) = launches.getValue(LaunchOutcome.of(launch)).increment()

    fun settled(reason: ExitReason) = settlements.getValue(reason).increment()
}
