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
    private val waiting = mutableSetOf<FlowSource>()

    /**
     * Wait for the specified [consumer] to complete before ending the lifecycle of the workload.
     */
    public fun waitFor(consumer: FlowSource): FlowSource {
        waiting.add(consumer)
        return object : FlowSource by consumer {
            override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
                try {
                    consumer.onStop(conn, now, delta)
                } finally {
                    complete(consumer)
                }
            }
            override fun toString(): String = "SimWorkloadLifecycle.Consumer[delegate=$consumer]"
        }
    }

    /**
     * Complete the specified [FlowSource].
     */
    private fun complete(consumer: FlowSource) {
        if (waiting.remove(consumer) && waiting.isEmpty()) {
            ctx.close()
        }
    }
}
