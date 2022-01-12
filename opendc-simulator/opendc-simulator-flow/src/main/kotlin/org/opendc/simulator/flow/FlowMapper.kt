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

package org.opendc.simulator.flow

/**
 * A [FlowConsumer] that maps the pushed flow through [transform].
 *
 * @param source The source of the flow.
 * @param transform The method to transform the flow.
 */
public class FlowMapper(
    private val source: FlowSource,
    private val transform: (FlowConnection, Double) -> Double
) : FlowSource {

    /**
     * The current active connection.
     */
    private var _conn: Connection? = null

    override fun onStart(conn: FlowConnection, now: Long) {
        check(_conn == null) { "Concurrent access" }
        val delegate = Connection(conn, transform)
        _conn = delegate
        source.onStart(delegate, now)
    }

    override fun onStop(conn: FlowConnection, now: Long) {
        val delegate = checkNotNull(_conn) { "Invariant violation" }
        _conn = null
        source.onStop(delegate, now)
    }

    override fun onPull(conn: FlowConnection, now: Long): Long {
        val delegate = checkNotNull(_conn) { "Invariant violation" }
        return source.onPull(delegate, now)
    }

    override fun onConverge(conn: FlowConnection, now: Long) {
        val delegate = _conn ?: return
        source.onConverge(delegate, now)
    }

    /**
     * The wrapper [FlowConnection] that is used to transform the flow.
     */
    private class Connection(
        private val delegate: FlowConnection,
        private val transform: (FlowConnection, Double) -> Double
    ) : FlowConnection by delegate {
        override fun push(rate: Double) {
            delegate.push(transform(this, rate))
        }
    }
}
