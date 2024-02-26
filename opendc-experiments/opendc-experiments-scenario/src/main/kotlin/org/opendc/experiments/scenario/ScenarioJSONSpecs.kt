package org.opendc.experiments.scenario

import kotlinx.serialization.Serializable
import org.opendc.compute.workload.ComputeWorkload

@Serializable
public data class ScenarioJSONSpec(
    val topology: TopologyJSONSpec,
    val workload: WorkloadJSONSpec,
    val operationalPhenomena: OperationalPhenomenaJsonSpec,
    val allocationPolicy: AllocationPolicyJSONSpec,
    val partitions: Map<String, String> = emptyMap()
)

@Serializable
public data class TopologyJSONSpec (
    val pathToFile: String // TODO: Path?
)

@Serializable
public data class WorkloadJSONSpec (
    val name: String,
    val source: ComputeWorkload
)

@Serializable
public data class WorkloadSourceJSONSpec (
    val sourceType: String,
    val sourceName: String
)

@Serializable
public data class OperationalPhenomenaJsonSpec (
    val failureFrequency: Double,
    val hasInterface: Boolean
)

@Serializable
public data class AllocationPolicyJSONSpec (
    val policyType: String
)
