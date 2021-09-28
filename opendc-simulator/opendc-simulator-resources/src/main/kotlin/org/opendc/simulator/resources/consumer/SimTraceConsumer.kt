/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.resources.consumer

import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import org.opendc.simulator.resources.SimResourceEvent

/**
 * A [SimResourceConsumer] that replays a workload trace consisting of multiple fragments, each indicating the resource
 * consumption for some period of time.
 */
public class SimTraceConsumer(private val trace: Sequence<Fragment>) : SimResourceConsumer {
    private var iterator: Iterator<Fragment>? = null

    override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
        val iterator = checkNotNull(iterator)
        return if (iterator.hasNext()) {
            val fragment = iterator.next()
            ctx.push(fragment.usage)
            fragment.duration
        } else {
            ctx.close()
            Long.MAX_VALUE
        }
    }

    override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
        when (event) {
            SimResourceEvent.Start -> {
                check(iterator == null) { "Consumer already running" }
                iterator = trace.iterator()
            }
            SimResourceEvent.Exit -> {
                iterator = null
            }
            else -> {}
        }
    }

    /**
     * A fragment of the workload.
     */
    public data class Fragment(val duration: Long, val usage: Double)
}
