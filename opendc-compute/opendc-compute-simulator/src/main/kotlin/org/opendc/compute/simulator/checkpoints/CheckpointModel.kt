package org.opendc.compute.simulator.checkpoints

public data class CheckpointModel(
    val checkpointWait: Long = 60*60*1000,
    val checkpointTime: Long = 5*60*1000
)
