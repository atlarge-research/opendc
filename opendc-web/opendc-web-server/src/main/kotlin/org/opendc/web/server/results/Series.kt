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

import org.opendc.web.launcher.MetricPoint
import org.opendc.web.launcher.Reduction

/**
 * Averages one metric over the seeds a scenario was run with, aligned on the instant rather than on
 * position so an instant one seed lacks does not shift the others.
 */
fun meanAcrossSeeds(perSeed: List<List<MetricPoint>>): List<MetricPoint> {
    perSeed.singleOrNull()?.let { return it }
    val total = sortedMapOf<Long, DoubleArray>()
    for (points in perSeed) {
        for (point in points) {
            val cell = total.getOrPut(point.t) { DoubleArray(2) }
            cell[SUM] += point.value
            cell[COUNT]++
        }
    }
    return total.map { (t, cell) -> MetricPoint(t, cell[SUM] / cell[COUNT]) }
}

/**
 * Buckets [width] ms wide laid end to end from [origin]. Every series of an experiment is folded into
 * the same grid, so the scenarios drawn on one chart share their instants.
 */
data class BucketGrid(
    val origin: Long,
    val width: Long,
)

/**
 * The grid that folds each of [series] to at most [buckets] points. It starts at the earliest instant
 * any of them measured and is the smallest whole multiple of every one of [exportIntervalsMs], doubled
 * until the longest series fits and no series skips a bucket. Each bucket then holds as many samples of
 * a scenario as the next, whichever interval it exported at, and as many of the points a launcher folds
 * by doubling too.
 */
fun bucketGrid(
    series: List<List<MetricPoint>>,
    exportIntervalsMs: List<Long>,
    buckets: Int,
): BucketGrid {
    require(buckets > 0) { "a grid needs at least one bucket, not $buckets" }
    var width = exportIntervalsMs.filter { it > 0 }.fold(1L, ::leastCommonMultiple)
    val measured = series.filter { it.isNotEmpty() }
    if (measured.isEmpty()) {
        return BucketGrid(0, width)
    }
    val origin = measured.minOf { it.first().t }
    val end = measured.maxOf { it.last().t }
    val widestGap = measured.maxOf { points -> points.zipWithNext { first, next -> next.t - first.t }.fold(0L, ::maxOf) }
    while ((end - origin) / width >= buckets || width < widestGap) {
        width *= 2
    }
    return BucketGrid(origin, width)
}

private tailrec fun greatestCommonDivisor(
    left: Long,
    right: Long,
): Long = if (right == 0L) left else greatestCommonDivisor(right, left % right)

private fun leastCommonMultiple(
    left: Long,
    right: Long,
): Long = left / greatestCommonDivisor(left, right) * right

/** Folds [points] into [grid], each bucket the way [fold] says and stamped with the instant it begins. */
fun bucket(
    points: List<MetricPoint>,
    grid: BucketGrid,
    fold: Reduction,
): List<MetricPoint> =
    points
        .groupBy { (it.t - grid.origin) / grid.width }
        .map { (index, inside) -> MetricPoint(grid.origin + index * grid.width, fold.of(inside.map { it.value })) }

/**
 * How far apart the seeds landed: each seed's series reduced by [fold], then the highest minus the
 * lowest. Zero for a single seed.
 */
fun spreadAcrossSeeds(
    perSeed: List<List<MetricPoint>>,
    fold: Reduction,
): Double {
    val reduced = perSeed.filter { it.isNotEmpty() }.map { points -> fold.of(points.map { it.value }) }
    return if (reduced.size < 2) 0.0 else reduced.max() - reduced.min()
}

private const val SUM = 0
private const val COUNT = 1
