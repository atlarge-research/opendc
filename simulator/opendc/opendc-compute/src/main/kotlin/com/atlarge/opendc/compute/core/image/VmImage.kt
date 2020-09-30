package com.atlarge.opendc.compute.core.image

import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.core.resource.TagContainer
import java.util.UUID
import kotlin.math.min

class VmImage(
    public override val uid: UUID,
    public override val name: String,
    public override val tags: TagContainer,
    public val flopsHistory: Sequence<FlopsHistoryFragment>,
    public val maxCores: Int,
    public val requiredMemory: Long
) : Image {

    override suspend fun invoke(ctx: ServerContext) {
        var offset = ctx.clock.millis()

        val batch = flopsHistory.map { fragment ->
            val cores = min(fragment.cores, ctx.server.flavor.cpuCount)
            val burst = LongArray(cores) { fragment.flops / cores }
            val usage = DoubleArray(cores) { fragment.usage / cores }
            offset += fragment.duration
            ServerContext.Slice(burst, usage, offset)
        }

        ctx.run(batch)
    }

    override fun toString(): String = "VmImage(uid=$uid, name=$name, cores=$maxCores, requiredMemory=$requiredMemory)"
}
