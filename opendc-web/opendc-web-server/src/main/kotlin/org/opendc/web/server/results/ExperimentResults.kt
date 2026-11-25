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

package org.opendc.web.server.results

import kotlinx.serialization.Serializable

/** One metric at one instant of simulated time, [t] milliseconds in. */
@Serializable
data class ResultPoint(
    val t: Long,
    val value: Double,
)

/**
 * One metric of one scenario, averaged over the seeds it was run with.
 *
 * @property spread How far apart those seeds landed over the whole series; zero for a single seed.
 */
@Serializable
data class ResultSeries(
    val metric: String,
    val points: List<ResultPoint>,
    val spread: Double,
)

// No defaults on these collections: the wire form omits defaulted fields, and an empty list must be sent.
@Serializable
data class ScenarioResults(
    val scenarioIndex: Int,
    val seeds: Int,
    val complete: Boolean,
    val series: List<ResultSeries>,
)

/**
 * @property exportIntervalMs How often the simulation itself took a sample.
 * @property bucketMs How far apart the points reported here are, after folding samples into buckets.
 */
@Serializable
data class ExperimentResults(
    val experimentId: String,
    val exportIntervalMs: Long,
    val bucketMs: Long,
    val complete: Boolean,
    val scenarios: List<ScenarioResults>,
)
