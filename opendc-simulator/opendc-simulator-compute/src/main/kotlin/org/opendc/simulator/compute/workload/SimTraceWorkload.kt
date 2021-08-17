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

import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext

/**
 * A [SimWorkload] that replays a workload trace consisting of multiple fragments, each indicating the resource
 * consumption for some period of time.
 *
 * @param trace The trace of fragments to use.
 * @param offset The offset for the timestamps.
 */
public class SimTraceWorkload(public val trace: Sequence<Fragment>, private val offset: Long = 0L) : SimWorkload {
    private val iterator = trace.iterator()
    private var fragment: Fragment? = null

    override fun onStart(ctx: SimMachineContext) {
        val lifecycle = SimWorkloadLifecycle(ctx)

        for (cpu in ctx.cpus) {
            cpu.startConsumer(lifecycle.waitFor(Consumer(cpu.model)))
        }
    }

    override fun toString(): String = "SimTraceWorkload"

    /**
     * Obtain the fragment with a timestamp equal or greater than [now].
     */
    private fun pullFragment(now: Long): Fragment? {
        var fragment = fragment
        if (fragment != null && !fragment.isExpired(now)) {
            return fragment
        }

        while (iterator.hasNext()) {
            fragment = iterator.next()
            if (!fragment.isExpired(now)) {
                this.fragment = fragment
                return fragment
            }
        }

        this.fragment = null
        return null
    }

    /**
     * Determine if the specified [Fragment] is expired, i.e., it has already passed.
     */
    private fun Fragment.isExpired(now: Long): Boolean {
        val timestamp = this.timestamp + offset
        return now >= timestamp + duration
    }

    private inner class Consumer(val cpu: ProcessingUnit) : SimResourceConsumer {
        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            val now = ctx.clock.millis()
            val fragment = pullFragment(now) ?: return SimResourceCommand.Exit
            val timestamp = fragment.timestamp + offset

            // Fragment is in the future
            if (timestamp > now) {
                return SimResourceCommand.Idle(timestamp)
            }

            val usage = fragment.usage / fragment.cores
            val deadline = timestamp + fragment.duration
            val duration = deadline - now
            val work = duration * usage / 1000

            return if (cpu.id < fragment.cores && work > 0.0)
                SimResourceCommand.Consume(work, usage, deadline)
            else
                SimResourceCommand.Idle(deadline)
        }
    }

    /**
     * A fragment of the workload.
     */
    public data class Fragment(val timestamp: Long, val duration: Long, val usage: Double, val cores: Int)
}
