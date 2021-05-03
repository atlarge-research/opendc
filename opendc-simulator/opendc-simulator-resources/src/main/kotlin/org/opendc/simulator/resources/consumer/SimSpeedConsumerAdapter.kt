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
import org.opendc.simulator.resources.SimResourceEvent
import kotlin.math.min

/**
 * Helper class to expose an observable [speed] field describing the speed of the consumer.
 */
public class SimSpeedConsumerAdapter(
    private val delegate: SimResourceConsumer,
    private val callback: (Double) -> Unit = {}
) : SimResourceConsumer by delegate {
    /**
     * The resource processing speed at this instant.
     */
    public var speed: Double = 0.0
        private set(value) {
            if (field != value) {
                callback(value)
                field = value
            }
        }

    init {
        callback(0.0)
    }

    override fun onNext(ctx: SimResourceContext): SimResourceCommand {
        return delegate.onNext(ctx)
    }

    override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
        val oldSpeed = speed

        delegate.onEvent(ctx, event)

        when (event) {
            SimResourceEvent.Run -> speed = ctx.speed
            SimResourceEvent.Capacity -> {
                // Check if the consumer interrupted the consumer and updated the resource consumption. If not, we might
                // need to update the current speed.
                if (oldSpeed == speed) {
                    speed = min(ctx.capacity, speed)
                }
            }
            SimResourceEvent.Exit -> speed = 0.0
            else -> {}
        }
    }

    override fun onFailure(ctx: SimResourceContext, cause: Throwable) {
        speed = 0.0

        delegate.onFailure(ctx, cause)
    }

    override fun toString(): String = "SimSpeedConsumerAdapter[delegate=$delegate]"
}
