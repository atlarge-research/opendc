package com.atlarge.opendc.core.workload

import com.atlarge.opendc.core.resource.Resource

/**
 * Meta-data key for the [PerformanceInterferenceModel] of an image.
 */
const val IMAGE_PERF_INTERFERENCE_MODEL = "image:performance-interference"

/**
 * Performance Interference Model describing the variability incurred by different sets of workloads if colocated.
 *
 * @param items The [PerformanceInterferenceModelItem]s that make up this model.
 */
data class PerformanceInterferenceModel(
    val items: Set<PerformanceInterferenceModelItem>
) {
    fun apply(colocatedWorkloads: Set<Resource>, currentServerLoad: Double): Double {
        val colocatedWorkloadIds = colocatedWorkloads.map { it.name }
        val intersectingItems = items.filter { item ->
            colocatedWorkloadIds.intersect(item.workloadNames).size > 1
        }

        if (intersectingItems.isEmpty()) {
            return 1.0
        }
        return intersectingItems
            .filter { it.minServerLoad <= currentServerLoad }
            .map { it.performanceScore }
            .min() ?: 1.0
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
    val workloadNames: Set<String>,
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

    override fun hashCode(): Int {
        return workloadNames.hashCode()
    }
}
