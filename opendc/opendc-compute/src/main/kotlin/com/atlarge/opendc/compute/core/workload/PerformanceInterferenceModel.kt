/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.core.workload

import com.atlarge.opendc.compute.core.Server
import java.util.SortedSet
import java.util.TreeSet
import kotlin.random.Random

/**
 * Meta-data key for the [PerformanceInterferenceModel] of an image.
 */
const val IMAGE_PERF_INTERFERENCE_MODEL = "image:performance-interference"

/**
 * Performance Interference Model describing the variability incurred by different sets of workloads if colocated.
 *
 * @param items The [PerformanceInterferenceModelItem]s that make up this model.
 */
class PerformanceInterferenceModel(
    items: Set<PerformanceInterferenceModelItem>,
    val random: Random = Random(0)
) {
    private var intersectingItems: List<PerformanceInterferenceModelItem> = emptyList()
    private val comparator = Comparator<PerformanceInterferenceModelItem> { lhs, rhs ->
        var cmp = lhs.performanceScore.compareTo(rhs.performanceScore)
        if (cmp != 0) {
            return@Comparator cmp
        }

        cmp = lhs.minServerLoad.compareTo(rhs.minServerLoad)
        if (cmp != 0) {
            return@Comparator cmp
        }

        lhs.hashCode().compareTo(rhs.hashCode())
    }
    val items = TreeSet(comparator)
    private val colocatedWorkloads = TreeSet<String>()

    init {
        this.items.addAll(items)
    }

    fun vmStarted(server: Server) {
        colocatedWorkloads.add(server.image.name)
        intersectingItems = items.filter { item -> doesMatch(item) }
    }

    fun vmStopped(server: Server) {
        colocatedWorkloads.remove(server.image.name)
        intersectingItems = items.filter { item -> doesMatch(item) }
    }

    private fun doesMatch(item: PerformanceInterferenceModelItem): Boolean {
        var count = 0
        for (name in item.workloadNames.subSet(colocatedWorkloads.first(), colocatedWorkloads.last() + "\u0000")) {
            if (name in colocatedWorkloads)
                count++
            if (count > 1)
                return true
        }
        return false
    }

    fun apply(currentServerLoad: Double): Double {
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
data class PerformanceInterferenceModelItem(
    val workloadNames: SortedSet<String>,
    val minServerLoad: Double,
    val performanceScore: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PerformanceInterferenceModelItem

        if (workloadNames != other.workloadNames) return false

        return true
    }

    override fun hashCode(): Int = workloadNames.hashCode()
}
