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

package org.opendc.simulator.flow.source

import org.opendc.simulator.flow.FlowConnection
import org.opendc.simulator.flow.FlowEvent
import org.opendc.simulator.flow.FlowSource
import kotlin.math.min

/**
 * Helper class to expose an observable [speed] field describing the speed of the consumer.
 */
public class FlowSourceRateAdapter(
    private val delegate: FlowSource,
    private val callback: (Double) -> Unit = {}
) : FlowSource by delegate {
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

    override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
        return delegate.onPull(conn, now, delta)
    }

    override fun onEvent(conn: FlowConnection, now: Long, event: FlowEvent) {
        val oldSpeed = speed

        delegate.onEvent(conn, now, event)

        when (event) {
            FlowEvent.Converge -> speed = conn.rate
            FlowEvent.Capacity -> {
                // Check if the consumer interrupted the consumer and updated the resource consumption. If not, we might
                // need to update the current speed.
                if (oldSpeed == speed) {
                    speed = min(conn.capacity, speed)
                }
            }
            FlowEvent.Exit -> speed = 0.0
            else -> {}
        }
    }

    override fun onFailure(conn: FlowConnection, cause: Throwable) {
        speed = 0.0

        delegate.onFailure(conn, cause)
    }

    override fun toString(): String = "FlowSourceRateAdapter[delegate=$delegate]"
}
