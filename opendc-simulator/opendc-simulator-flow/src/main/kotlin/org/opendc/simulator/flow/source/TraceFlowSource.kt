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

/**
 * A [FlowSource] that replays a sequence of [Fragment], each indicating the flow rate for some period of time.
 */
public class TraceFlowSource(private val trace: Sequence<Fragment>) : FlowSource {
    private var _iterator: Iterator<Fragment>? = null
    private var _nextTarget = Long.MIN_VALUE

    override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
        // Check whether the trace fragment was fully consumed, otherwise wait until we have done so
        val nextTarget = _nextTarget
        if (nextTarget > now) {
            return now - nextTarget
        }

        val iterator = checkNotNull(_iterator)
        return if (iterator.hasNext()) {
            val fragment = iterator.next()
            _nextTarget = now + fragment.duration
            conn.push(fragment.usage)
            fragment.duration
        } else {
            conn.close()
            Long.MAX_VALUE
        }
    }

    override fun onEvent(conn: FlowConnection, now: Long, event: FlowEvent) {
        when (event) {
            FlowEvent.Start -> {
                check(_iterator == null) { "Source already running" }
                _iterator = trace.iterator()
            }
            FlowEvent.Exit -> {
                _iterator = null
            }
            else -> {}
        }
    }

    /**
     * A fragment of the tgrace.
     */
    public data class Fragment(val duration: Long, val usage: Double)
}
