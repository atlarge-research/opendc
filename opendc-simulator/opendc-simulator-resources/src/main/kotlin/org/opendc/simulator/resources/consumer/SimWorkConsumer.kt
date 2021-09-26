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

import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import kotlin.math.roundToLong

/**
 * A [SimResourceConsumer] that consumes the specified amount of work at the specified utilization.
 */
public class SimWorkConsumer(
    private val work: Double,
    private val utilization: Double
) : SimResourceConsumer {

    init {
        require(work >= 0.0) { "Work must be positive" }
        require(utilization > 0.0 && utilization <= 1.0) { "Utilization must be in (0, 1]" }
    }

    private var remainingWork = work

    override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): SimResourceCommand {
        val actualWork = ctx.speed * delta / 1000.0
        val limit = ctx.capacity * utilization

        remainingWork -= actualWork

        val remainingWork = remainingWork
        val duration = (remainingWork / limit * 1000).roundToLong()

        return if (duration > 0) {
            SimResourceCommand.Consume(limit, duration)
        } else {
            SimResourceCommand.Exit
        }
    }
}
