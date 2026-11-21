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

package org.opendc.web.launcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where a launcher says how far it has got. Never authoritative: the platform's account of the process is. */
@Serializable
sealed interface TelemetryTarget {
    @Serializable
    @SerialName("none")
    data object None : TelemetryTarget

    /**
     * @property url Has to be reachable from wherever the launcher runs, not from the server.
     * @property token Bearer credential that can only write progress into this one execution.
     */
    @Serializable
    @SerialName("endpoint")
    data class Endpoint(
        val url: String,
        val token: String,
    ) : TelemetryTarget
}

/**
 * Everything the launcher currently knows about its runs. Each post carries the whole picture, since
 * posts are fire-and-forget and a lost one has to be corrected by the next.
 */
@Serializable
data class TelemetryReport(
    val runs: List<RunTelemetry>,
)

/**
 * How one `(scenario, seed)` run is getting on.
 *
 * The total task count is not reported: the server fixes it at submit, so the denominator of a
 * progress bar never moves.
 */
@Serializable
data class RunTelemetry(
    val scenarioIndex: Int,
    val seed: Long,
    val completedTasks: Int,
    val series: List<MetricSeries>,
)

@Serializable
data class MetricSeries(
    val metric: String,
    val points: List<MetricPoint>,
)

/** One metric at one instant of simulated time, [t] milliseconds in. */
@Serializable
data class MetricPoint(
    val t: Long,
    val value: Double,
)
