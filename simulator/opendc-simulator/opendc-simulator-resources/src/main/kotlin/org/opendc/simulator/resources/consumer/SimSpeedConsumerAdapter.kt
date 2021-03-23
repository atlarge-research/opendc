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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import kotlin.math.min

/**
 * Helper class to expose an observable [speed] field describing the speed of the consumer.
 */
public class SimSpeedConsumerAdapter(private val delegate: SimResourceConsumer) : SimResourceConsumer by delegate {
    /**
     * The resource processing speed over time.
     */
    public val speed: StateFlow<Double>
        get() = _speed
    private val _speed = MutableStateFlow(0.0)

    override fun onNext(ctx: SimResourceContext): SimResourceCommand {
        val command = delegate.onNext(ctx)

        when (command) {
            is SimResourceCommand.Idle -> _speed.value = 0.0
            is SimResourceCommand.Consume -> _speed.value = min(ctx.capacity, command.limit)
            is SimResourceCommand.Exit -> _speed.value = 0.0
        }

        return command
    }

    override fun onCapacityChanged(ctx: SimResourceContext, isThrottled: Boolean) {
        val oldSpeed = _speed.value

        delegate.onCapacityChanged(ctx, isThrottled)

        // Check if the consumer interrupted the consumer and updated the resource consumption. If not, we might
        // need to update the current speed.
        if (oldSpeed == _speed.value) {
            _speed.value = min(ctx.capacity, _speed.value)
        }
    }

    override fun toString(): String = "SimSpeedConsumerAdapter[delegate=$delegate]"
}
