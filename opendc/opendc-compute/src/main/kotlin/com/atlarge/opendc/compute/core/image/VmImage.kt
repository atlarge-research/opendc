package com.atlarge.opendc.compute.core.image

import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.core.resource.TagContainer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.min

class VmImage(
    public override val uid: UUID,
    public override val name: String,
    public override val tags: TagContainer,
    public val flopsHistory: List<FlopsHistoryFragment>,
    public val cores: Int,
    public val requiredMemory: Long
) : Image {

    override suspend fun invoke(ctx: ServerContext) {
        flopsHistory.forEach { fragment ->
            if (fragment.flops == 0L) {
                delay(fragment.duration)
            } else {
                val cores = min(this.cores, ctx.server.flavor.cpuCount)
                val req = fragment.flops / cores
                coroutineScope {
                    for (cpu in ctx.cpus.take(cores)) {
                        val usage = req / (fragment.usage * 1_000_000L)
                        launch { cpu.run(req, usage, simulationContext.clock.millis() + fragment.duration) }
                    }
                }
            }
        }
    }
}
