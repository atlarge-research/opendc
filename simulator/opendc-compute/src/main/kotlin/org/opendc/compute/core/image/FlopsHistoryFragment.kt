package org.opendc.compute.core.image

data class FlopsHistoryFragment(val tick: Long, val flops: Long, val duration: Long, val usage: Double, val cores: Int)
