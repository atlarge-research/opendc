/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.atlarge.opendc.compute.core.image

import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.core.resource.TagContainer
import kotlinx.coroutines.ensureActive
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/**
 * An application [Image] that models applications performing a static number of floating point operations ([flops]) on
 * a compute resource.
 *
 * @property uid The unique identifier of this image.
 * @property name The name of this image.
 * @property tags The tags attached to the image.
 * @property flops The number of floating point operations to perform for this task in MFLOPs.
 * @property cores The number of cores that the image is able to utilize.
 * @property utilization A model of the CPU utilization of the application.
 */
data class FlopsApplicationImage(
    public override val uid: UUID,
    public override val name: String,
    public override val tags: TagContainer,
    public val flops: Long,
    public val cores: Int,
    public val utilization: Double = 0.8
) : Image {
    init {
        require(flops >= 0) { "Negative number of flops" }
        require(cores > 0) { "Negative number of cores or no cores" }
        require(utilization > 0.0 && utilization <= 1.0) { "Utilization must be in (0, 1]" }
    }

    /**
     * Execute the runtime behavior based on a number of floating point operations to execute.
     */
    override suspend fun invoke(ctx: ServerContext) {
        val cores = min(this.cores, ctx.server.flavor.cpuCount)
        val burst = LongArray(cores) { flops / cores }
        val maxUsage = DoubleArray(cores) { i -> ctx.cpus[i].frequency * utilization }

        while (burst.any { it != 0L }) {
            coroutineContext.ensureActive()
            ctx.run(burst, maxUsage, Long.MAX_VALUE)
        }
    }
}
