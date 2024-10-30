package org.opendc.compute.topology.specs

public data class ClusterSpec (
    val name: String,
    val hostSpecs: List<HostSpec>,
    val powerSource: PowerSourceSpec
)
