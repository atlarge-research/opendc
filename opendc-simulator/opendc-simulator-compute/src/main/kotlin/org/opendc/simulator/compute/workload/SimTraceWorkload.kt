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
import org.opendc.simulator.compute.util.SimWorkloadLifecycle
import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import org.opendc.simulator.resources.consumer.SimConsumerBarrier

/**
 * A [SimWorkload] that replays a workload trace consisting of multiple fragments, each indicating the resource
 * consumption for some period of time.
 */
public class SimTraceWorkload(public val trace: Sequence<Fragment>) : SimWorkload {
    private var offset = Long.MIN_VALUE
    private val iterator = trace.iterator()
    private var fragment: Fragment? = null
    private lateinit var barrier: SimConsumerBarrier

    override fun onStart(ctx: SimMachineContext) {
        check(offset == Long.MIN_VALUE) { "Workload does not support re-use" }

        barrier = SimConsumerBarrier(ctx.cpus.size)
        fragment = nextFragment()
        offset = ctx.interpreter.clock.millis()

        val lifecycle = SimWorkloadLifecycle(ctx)

        for (cpu in ctx.cpus) {
            cpu.startConsumer(lifecycle.waitFor(Consumer(cpu.model)))
        }
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

    private inner class Consumer(val cpu: ProcessingUnit) : SimResourceConsumer {
        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            val now = ctx.clock.millis()
            val fragment = fragment ?: return SimResourceCommand.Exit
            val usage = fragment.usage / fragment.cores
            val work = (fragment.duration / 1000) * usage
            val deadline = offset + fragment.duration

            assert(deadline >= now) { "Deadline already passed" }

            val cmd =
                if (cpu.id < fragment.cores && work > 0.0)
                    SimResourceCommand.Consume(work, usage, deadline)
                else
                    SimResourceCommand.Idle(deadline)

            if (barrier.enter()) {
                this@SimTraceWorkload.fragment = nextFragment()
                this@SimTraceWorkload.offset += fragment.duration
            }

            return cmd
        }
    }

    /**
     * A fragment of the workload.
     */
    public data class Fragment(val duration: Long, val usage: Double, val cores: Int)
}
