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
 * A [SimWorkload] that replays a workload trace consisting of multiple fragments, each indicating the resource
 * consumption for some period of time.
 */
public class SimTraceWorkload(private val trace: Sequence<Fragment>) : SimWorkload {
    override suspend fun run(ctx: SimExecutionContext) {
        var offset = ctx.clock.millis()

        val batch = trace.map { fragment ->
            val cores = min(fragment.cores, ctx.machine.cpus.size)
            val burst = LongArray(cores) { fragment.flops / cores }
            val usage = DoubleArray(cores) { fragment.usage / cores }
            offset += fragment.duration
            SimExecutionContext.Slice(burst, usage, offset)
        }

        ctx.run(batch)
    }

    override fun toString(): String = "SimTraceWorkload"

    /**
     * A fragment of the workload.
     */
    public data class Fragment(val time: Long, val flops: Long, val duration: Long, val usage: Double, val cores: Int)
}
