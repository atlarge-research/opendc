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

package org.opendc.simulator.flow.internal

import org.opendc.simulator.flow.FlowCounters

/**
 * Mutable implementation of the [FlowCounters] interface.
 */
public class MutableFlowCounters : FlowCounters {
    override val demand: Double
        get() = _counters[0]
    override val actual: Double
        get() = _counters[1]
    override val remaining: Double
        get() = _counters[2]
    private val _counters = DoubleArray(3)

    override fun reset() {
        _counters.fill(0.0)
    }

    public fun increment(demand: Double, actual: Double, remaining: Double) {
        val counters = _counters
        counters[0] += demand
        counters[1] += actual
        counters[2] += remaining
    }

    override fun toString(): String {
        return "FlowCounters[demand=$demand,actual=$actual,remaining=$remaining]"
    }
}
