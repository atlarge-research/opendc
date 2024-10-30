package org.opendc.compute.topology.specs

import java.util.UUID

// TODO: add name to class
public data class PowerSourceSpec(
    val uid: UUID,
    val name: String = "unknown",
    val meta: Map<String, Any> = emptyMap(),
    val totalPower: Long
)
