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

package org.opendc.simulator.compute.util

import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import org.opendc.simulator.resources.SimResourceEvent

/**
 * A helper class to manage the lifecycle of a [SimWorkload]
 */
public class SimWorkloadLifecycle(private val ctx: SimMachineContext) {
    /**
     * The resource consumers which represent the lifecycle of the workload.
     */
    private val waiting = mutableSetOf<SimResourceConsumer>()

    /**
     * Wait for the specified [consumer] to complete before ending the lifecycle of the workload.
     */
    public fun waitFor(consumer: SimResourceConsumer): SimResourceConsumer {
        waiting.add(consumer)
        return object : SimResourceConsumer by consumer {
            override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
                try {
                    consumer.onEvent(ctx, event)
                } finally {
                    if (event == SimResourceEvent.Exit) {
                        complete(consumer)
                    }
                }
            }

            override fun onFailure(ctx: SimResourceContext, cause: Throwable) {
                try {
                    consumer.onFailure(ctx, cause)
                } finally {
                    complete(consumer)
                }
            }

            override fun toString(): String = "SimWorkloadLifecycle.Consumer[delegate=$consumer]"
        }
    }

    /**
     * Complete the specified [SimResourceConsumer].
     */
    private fun complete(consumer: SimResourceConsumer) {
        if (waiting.remove(consumer) && waiting.isEmpty()) {
            ctx.close()
        }
    }
}
