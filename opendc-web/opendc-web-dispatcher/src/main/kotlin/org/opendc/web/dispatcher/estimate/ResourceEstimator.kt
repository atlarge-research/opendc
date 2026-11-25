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

package org.opendc.web.dispatcher.estimate

import org.opendc.sdk.model.scenario.ScenarioSpec

/**
 * Predicts what one `(scenario, seed)` run will cost, before anything is simulated.
 *
 * Implementations are pure, so a model can be checked against recorded runs with no database and no
 * cluster.
 */
fun interface ResourceEstimator {
    fun estimate(
        scenario: ScenarioSpec,
        workload: TraceExtent,
    ): UnitEstimate
}

/**
 * How much workload one run has to get through, in rows.
 *
 * Loading builds one object per task and per fragment, so rows predict the heap where a compressed
 * byte count does not.
 */
data class TraceExtent(
    val taskCount: Long,
    val fragmentCount: Long,
) {
    companion object {
        /** A workload that has never been measured. Estimates from it rest on the topology alone. */
        val UNKNOWN = TraceExtent(taskCount = 0, fragmentCount = 0)
    }
}

/**
 * What one run is expected to cost. Seconds are wall clock on a core of its own, not the simulated
 * seconds a user is billed for.
 */
data class UnitEstimate(
    val peakMemoryMb: Double,
    val cpuSeconds: Double,
)

/**
 * The same estimator scaled to the machine a deployment runs on.
 *
 * Both multipliers are held within a quarter and four times the model: past that the model has the
 * wrong shape and no constant will save it.
 */
fun ResourceEstimator.scaledBy(
    runtimeMultiplier: Double,
    memoryMultiplier: Double,
): ResourceEstimator {
    val runtime = bounded(runtimeMultiplier)
    val memory = bounded(memoryMultiplier)
    return ResourceEstimator { scenario, workload ->
        val estimate = estimate(scenario, workload)
        UnitEstimate(peakMemoryMb = estimate.peakMemoryMb * memory, cpuSeconds = estimate.cpuSeconds * runtime)
    }
}

private fun bounded(multiplier: Double): Double = if (multiplier.isNaN()) 1.0 else multiplier.coerceIn(0.25, 4.0)
