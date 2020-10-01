/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.compute.core.workload

import org.opendc.compute.core.Server
import java.util.*
import kotlin.random.Random

/**
 * Meta-data key for the [PerformanceInterferenceModel] of an image.
 */
public const val IMAGE_PERF_INTERFERENCE_MODEL: String = "image:performance-interference"

/**
 * Performance Interference Model describing the variability incurred by different sets of workloads if colocated.
 *
 * @param items The [PerformanceInterferenceModelItem]s that make up this model.
 */
public class PerformanceInterferenceModel(
    public val items: SortedSet<PerformanceInterferenceModelItem>,
    private val random: Random = Random(0)
) {
    private var intersectingItems: List<PerformanceInterferenceModelItem> = emptyList()
    private val colocatedWorkloads = TreeMap<String, Int>()

    internal fun vmStarted(server: Server) {
        colocatedWorkloads.merge(server.image.name, 1, Int::plus)
        intersectingItems = items.filter { item -> doesMatch(item) }
    }

    internal fun vmStopped(server: Server) {
        colocatedWorkloads.computeIfPresent(server.image.name) { _, v -> (v - 1).takeUnless { it == 0 } }
        intersectingItems = items.filter { item -> doesMatch(item) }
    }

    private fun doesMatch(item: PerformanceInterferenceModelItem): Boolean {
        var count = 0
        for (
            name in item.workloadNames.subSet(
                colocatedWorkloads.firstKey(),
                colocatedWorkloads.lastKey() + "\u0000"
            )
        ) {
            count += colocatedWorkloads.getOrDefault(name, 0)
            if (count > 1)
                return true
        }
        return false
    }

    internal fun apply(currentServerLoad: Double): Double {
        if (intersectingItems.isEmpty()) {
            return 1.0
        }
        val score = intersectingItems
            .firstOrNull { it.minServerLoad <= currentServerLoad }

        // Apply performance penalty to (on average) only one of the VMs
        return if (score != null && random.nextInt(score.workloadNames.size) == 0) {
            score.performanceScore
        } else {
            1.0
        }
    }
}

/**
 * Model describing how a specific set of workloads causes performance variability for each workload.
 *
 * @param workloadNames The names of the workloads that together cause performance variability for each workload in the set.
 * @param minServerLoad The minimum total server load at which this interference is activated and noticeable.
 * @param performanceScore The performance score that should be applied to each workload's performance. 1 means no
 * influence, <1 means that performance degrades, and >1 means that performance improves.
 */
public data class PerformanceInterferenceModelItem(
    public val workloadNames: SortedSet<String>,
    public val minServerLoad: Double,
    public val performanceScore: Double
) : Comparable<PerformanceInterferenceModelItem> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PerformanceInterferenceModelItem

        if (workloadNames != other.workloadNames) return false

        return true
    }

    override fun hashCode(): Int = workloadNames.hashCode()

    override fun compareTo(other: PerformanceInterferenceModelItem): Int {
        var cmp = performanceScore.compareTo(other.performanceScore)
        if (cmp != 0) {
            return cmp
        }

        cmp = minServerLoad.compareTo(other.minServerLoad)
        if (cmp != 0) {
            return cmp
        }

        return hashCode().compareTo(other.hashCode())
    }
}
