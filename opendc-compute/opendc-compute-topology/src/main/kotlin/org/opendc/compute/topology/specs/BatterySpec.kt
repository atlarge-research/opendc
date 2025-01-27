package org.opendc.compute.topology.specs

import java.util.UUID

public data class BatterySpec(
    val uid: UUID,

    val capacity: Double,
    val chargingSpeed: Double,
    val batteryPolicy: BatteryPolicyJSONSpec
)
