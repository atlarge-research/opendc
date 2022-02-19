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
import org.opendc.simulator.flow.FlowSource

/**
 * Helper class to expose an observable [rate] field describing the flow rate of the source.
 */
public class FlowSourceRateAdapter(
    private val delegate: FlowSource,
    private val callback: (Double) -> Unit = {}
) : FlowSource by delegate {
    /**
     * The resource processing speed at this instant.
     */
    public var rate: Double = 0.0
        private set(value) {
            if (field != value) {
                callback(value)
                field = value
            }
        }

    init {
        callback(0.0)
    }

    override fun onStart(conn: FlowConnection, now: Long) {
        conn.shouldSourceConverge = true

        delegate.onStart(conn, now)
    }

    override fun onStop(conn: FlowConnection, now: Long) {
        try {
            delegate.onStop(conn, now)
        } finally {
            rate = 0.0
        }
    }

    override fun onPull(conn: FlowConnection, now: Long): Long {
        return delegate.onPull(conn, now)
    }

    override fun onConverge(conn: FlowConnection, now: Long) {
        try {
            delegate.onConverge(conn, now)
        } finally {
            rate = conn.rate
        }
    }

    override fun toString(): String = "FlowSourceRateAdapter[delegate=$delegate]"
}
