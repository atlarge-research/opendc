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

package org.opendc.simulator.compute.workload

import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.flow.FlowConnection
import org.opendc.simulator.flow.FlowSource

/**
 * A helper class to manage the lifecycle of a [SimWorkload]
 */
public class SimWorkloadLifecycle(private val ctx: SimMachineContext) {
    /**
     * The resource consumers which represent the lifecycle of the workload.
     */
    private val waiting = HashSet<Wrapper>()

    /**
     * Wait for the specified [source] to complete before ending the lifecycle of the workload.
     */
    public fun waitFor(source: FlowSource): FlowSource {
        val wrapper = Wrapper(source)
        waiting.add(wrapper)
        return wrapper
    }

    /**
     * Complete the specified [Wrapper].
     */
    private fun complete(wrapper: Wrapper) {
        if (waiting.remove(wrapper) && waiting.isEmpty()) {
            ctx.close()
        }
    }

    /**
     * A [FlowSource] that wraps [delegate] and informs [SimWorkloadLifecycle] that is has completed.
     */
    private inner class Wrapper(private val delegate: FlowSource) : FlowSource {
        override fun onStart(conn: FlowConnection, now: Long) {
            delegate.onStart(conn, now)
        }

        override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
            return delegate.onPull(conn, now, delta)
        }

        override fun onConverge(conn: FlowConnection, now: Long, delta: Long) {
            delegate.onConverge(conn, now, delta)
        }

        override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
            try {
                delegate.onStop(conn, now, delta)
            } finally {
                complete(this)
            }
        }

        override fun toString(): String = "SimWorkloadLifecycle.Wrapper[delegate=$delegate]"
    }
}
