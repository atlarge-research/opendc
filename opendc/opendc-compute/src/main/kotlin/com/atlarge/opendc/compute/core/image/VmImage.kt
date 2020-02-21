package com.atlarge.opendc.compute.core.image

import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.core.resource.TagContainer
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min

public val VM_SCHEDULING_SLICE_DURATION = 5 * 60 * 1000L

class VmImage(
    public override val uid: UUID,
    public override val name: String,
    public override val tags: TagContainer,
    public val flopsHistory: List<FlopsHistoryFragment>,
    public val cores: Int
) : Image {
    override suspend fun invoke(ctx: ServerContext) {
        flopsHistory.forEach { fragment ->
            if (fragment.flops == 0L) {
                delay(fragment.duration)
            } else {
                for (time in fragment.tick until fragment.tick + fragment.duration step VM_SCHEDULING_SLICE_DURATION) {
                    val cores = min(this.cores, ctx.server.flavor.cpus.sumBy { it.cores })
                    val req =
                        (fragment.flops / (ceil(fragment.duration.toDouble() / VM_SCHEDULING_SLICE_DURATION)) / cores).toLong()
                    ctx.run(LongArray(cores) { req }, VM_SCHEDULING_SLICE_DURATION)
                }
            }
        }
    }
}
