/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.simulator.compute.workload

import org.opendc.simulator.compute.SimExecutionContext
import kotlin.math.min

/**
 * A [SimWorkload] that models applications performing a static number of floating point operations ([flops]) on
 * a compute resource.
 *
 * @property flops The number of floating point operations to perform for this task in MFLOPs.
 * @property cores The number of cores that the image is able to utilize.
 * @property utilization A model of the CPU utilization of the application.
 */
public class SimFlopsWorkload(
    public val flops: Long,
    public val cores: Int,
    public val utilization: Double = 0.8
) : SimWorkload {
    init {
        require(flops >= 0) { "Negative number of flops" }
        require(cores > 0) { "Negative number of cores or no cores" }
        require(utilization > 0.0 && utilization <= 1.0) { "Utilization must be in (0, 1]" }
    }

    /**
     * Execute the runtime behavior based on a number of floating point operations to execute.
     */
    override suspend fun run(ctx: SimExecutionContext) {
        val cores = min(this.cores, ctx.machine.cpus.size)
        val burst = LongArray(cores) { flops / cores }
        val maxUsage = DoubleArray(cores) { i -> ctx.machine.cpus[i].frequency * utilization }

        ctx.run(SimExecutionContext.Slice(burst, maxUsage, Long.MAX_VALUE), triggerMode = SimExecutionContext.TriggerMode.LAST)
    }
}
