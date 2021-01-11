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

/**
 * A [SimWorkload] that replays a workload trace consisting of multiple fragments, each indicating the resource
 * consumption for some period of time.
 */
public class SimTraceWorkload(public val trace: Sequence<Fragment>) : SimWorkload {
    private var offset = 0L
    private val iterator = trace.iterator()
    private var fragment: Fragment? = null
    private lateinit var barrier: SimWorkloadBarrier

    override fun onStart(ctx: SimExecutionContext) {
        barrier = SimWorkloadBarrier(ctx.machine.cpus.size)
        fragment = nextFragment()
        offset = ctx.clock.millis()
    }

    override fun onStart(ctx: SimExecutionContext, cpu: Int): SimResourceCommand {
        return onNext(ctx, cpu, 0.0)
    }

    override fun onNext(ctx: SimExecutionContext, cpu: Int, remainingWork: Double): SimResourceCommand {
        val now = ctx.clock.millis()
        val fragment = fragment ?: return SimResourceCommand.Exit
        val work = (fragment.duration / 1000) * fragment.usage
        val deadline = offset + fragment.duration

        assert(deadline >= now) { "Deadline already passed" }

        val cmd =
            if (cpu < fragment.cores && work > 0.0)
                SimResourceCommand.Consume(work, fragment.usage, deadline)
            else
                SimResourceCommand.Idle(deadline)

        if (barrier.enter()) {
            this.fragment = nextFragment()
            this.offset += fragment.duration
        }

        return cmd
    }

    override fun toString(): String = "SimTraceWorkload"

    /**
     * Obtain the next fragment.
     */
    private fun nextFragment(): Fragment? {
        return if (iterator.hasNext()) {
            iterator.next()
        } else {
            null
        }
    }

    /**
     * A fragment of the workload.
     */
    public data class Fragment(val duration: Long, val usage: Double, val cores: Int)
}
