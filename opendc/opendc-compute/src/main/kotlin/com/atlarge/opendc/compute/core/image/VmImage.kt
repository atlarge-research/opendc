package com.atlarge.opendc.compute.core.image

import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.core.resource.TagContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.min

class VmImage(
    public override val uid: UUID,
    public override val name: String,
    public override val tags: TagContainer,
    public val flopsHistory: List<FlopsHistoryFragment>,
    public val maxCores: Int,
    public val requiredMemory: Long
) : Image {

    override suspend fun invoke(ctx: ServerContext) {
        for (fragment in flopsHistory) {
            coroutineContext.ensureActive()

            if (fragment.flops == 0L) {
                delay(fragment.duration)
            } else {
                val cores = min(fragment.cores, ctx.server.flavor.cpuCount)
                val burst = LongArray(cores) { fragment.flops / cores }
                val usage = DoubleArray(cores) { fragment.usage }

                ctx.run(burst, usage, simulationContext.clock.millis() + fragment.duration)
            }
        }
    }

    override fun toString(): String = "VmImage(uid=$uid, name=$name, cores=$maxCores, requiredMemory=$requiredMemory)"
}
