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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.web.launcher.MetricPoint
import org.opendc.web.launcher.Reduction

/**
 * A chart joins its scenarios on their instants, so every series of an experiment is folded onto one
 * grid, and each bucket is folded the way its metric adds up.
 */
class SeriesTest {
    // A failure model can stretch a run to twice the length of the same run without one.
    @Test
    fun `puts runs of different lengths on the same instants`() {
        val short = exports(List(40) { 1.0 })
        val long = exports(List(75) { 1.0 })

        val grid = bucketGrid(listOf(short, long), listOf(EXPORT_INTERVAL_MS), buckets = 8)
        val shortInstants = bucket(short, grid, Reduction.MEAN).map { it.t }
        val longInstants = bucket(long, grid, Reduction.MEAN).map { it.t }

        assertEquals(shortInstants, longInstants.take(shortInstants.size))
        assertTrue(longInstants.size <= 8, "no more points than asked for: $longInstants")
    }

    @Test
    fun `leaves a series that already fits as the simulation sampled it`() {
        val sampled = exports(listOf(1.0, 2.0, 3.0))

        assertEquals(sampled, bucket(sampled, bucketGrid(listOf(sampled), listOf(EXPORT_INTERVAL_MS), buckets = 10), Reduction.MEAN))
    }

    @Test
    fun `preserves an additive total instead of averaging it away`() {
        val energy = exports(List(6) { 1.0 })

        val folded = bucket(energy, bucketGrid(listOf(energy), listOf(EXPORT_INTERVAL_MS), buckets = 3), Reduction.SUM)

        assertEquals(listOf(2.0, 2.0, 2.0), folded.map { it.value })
    }

    @Test
    fun `swallows no samples when the count does not divide evenly`() {
        val energy = exports(List(7) { 1.0 })

        val folded = bucket(energy, bucketGrid(listOf(energy), listOf(EXPORT_INTERVAL_MS), buckets = 3), Reduction.SUM)

        assertEquals(7.0, folded.sumOf { it.value })
    }

    @Test
    fun `carries a cumulative counter forward rather than averaging inside a bucket`() {
        val completed = exports(listOf(1.0, 2.0, 3.0, 4.0))

        val folded = bucket(completed, bucketGrid(listOf(completed), listOf(EXPORT_INTERVAL_MS), buckets = 2), Reduction.LAST)

        assertEquals(listOf(2.0, 4.0), folded.map { it.value })
    }

    @Test
    fun `keeps a spike visible instead of smoothing it into the mean`() {
        val queued = exports(listOf(0.0, 90.0, 0.0, 0.0))

        val folded = bucket(queued, bucketGrid(listOf(queued), listOf(EXPORT_INTERVAL_MS), buckets = 2), Reduction.MAX)

        assertEquals(listOf(90.0, 0.0), folded.map { it.value })
    }

    // The writer records the moment the last task ends as well, which is rarely on the interval.
    @Test
    fun `folds the closing instant of a run into its last bucket instead of drawing a point of its own`() {
        val sampled = exports(listOf(1.0, 1.0, 1.0)) + MetricPoint(3 * EXPORT_INTERVAL_MS + 1_000, 1.0)

        val grid = bucketGrid(listOf(sampled), listOf(EXPORT_INTERVAL_MS), buckets = 512)

        assertEquals(EXPORT_INTERVAL_MS, grid.width)
        assertEquals(3, bucket(sampled, grid, Reduction.SUM).size)
    }

    // A sweep over export intervals samples its scenarios at different rates. A bucket holding two of a
    // scenario's samples where its neighbours hold one would make that scenario's sums jump.
    @Test
    fun `gives every bucket as many samples of a scenario as the next when scenarios export at different intervals`() {
        val everyFive = exports(List(72) { 1.0 })
        val everyFifteen = exports(List(24) { 1.0 }, every = 3 * EXPORT_INTERVAL_MS)

        val grid = bucketGrid(listOf(everyFive, everyFifteen), listOf(EXPORT_INTERVAL_MS, 3 * EXPORT_INTERVAL_MS), buckets = 16)

        assertEquals(setOf(2.0), bucket(everyFifteen, grid, Reduction.SUM).map { it.value }.toSet())
        assertEquals(setOf(6.0), bucket(everyFive, grid, Reduction.SUM).map { it.value }.toSet())
    }

    // A launcher folds its live samples by doubling too, so a running scenario arrives coarser than a
    // finished one. A grid finer than it would leave its line with a gap in every other bucket.
    @Test
    fun `gives a series its launcher already folded a point in every bucket`() {
        val finished = exports(List(64) { 1.0 })
        val live = exports(List(16) { 4.0 }, every = 4 * EXPORT_INTERVAL_MS)

        val grid = bucketGrid(listOf(finished, live), listOf(EXPORT_INTERVAL_MS), buckets = 512)
        val instants = bucket(live, grid, Reduction.SUM).map { it.t }

        assertTrue(instants.zipWithNext().all { (first, next) -> next - first == grid.width }, "gaps in $instants")
    }

    /** One sample per export, the first one interval in, as the simulator writes them. */
    private fun exports(
        values: List<Double>,
        every: Long = EXPORT_INTERVAL_MS,
    ): List<MetricPoint> = values.mapIndexed { index, value -> MetricPoint(every * (index + 1), value) }

    private companion object {
        const val EXPORT_INTERVAL_MS = 300_000L
    }
}
