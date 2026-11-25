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

package org.opendc.web.server.telemetry

import org.opendc.web.launcher.MetricSeries
import java.util.UUID

/** The `(scenario, seed)` run a launcher reports samples for. */
data class RunKey(
    val experimentId: UUID,
    val scenarioIndex: Int,
    val seed: Long,
) {
    /** The store key this run is filed under. */
    override fun toString(): String = "telemetry:$experimentId:$scenarioIndex:$seed"
}

/**
 * The samples of runs that have not finished yet. Everything here expires and may be lost: the
 * parquet is a run's canonical account, so the backend may be a cache with persistence off.
 */
interface TelemetryStore {
    /** Replaces what [key] has reported; reports are whole, never appended. */
    fun write(
        key: RunKey,
        series: List<MetricSeries>,
    )

    /** What each of [keys] has reported, leaving out the ones that have reported nothing. */
    fun read(keys: List<RunKey>): Map<RunKey, List<MetricSeries>>

    /** Drops runs whose samples describe an attempt that is being replaced. */
    fun forget(keys: List<RunKey>)
}
