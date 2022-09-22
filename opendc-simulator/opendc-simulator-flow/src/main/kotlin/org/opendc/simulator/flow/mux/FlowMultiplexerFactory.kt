/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.flow.mux

import org.opendc.simulator.flow.FlowConvergenceListener
import org.opendc.simulator.flow.FlowEngine

/**
 * Factory interface for a [FlowMultiplexer] implementation.
 */
public fun interface FlowMultiplexerFactory {
    /**
     * Construct a new [FlowMultiplexer] using the specified [engine] and [listener].
     */
    public fun newMultiplexer(engine: FlowEngine, listener: FlowConvergenceListener?): FlowMultiplexer

    public companion object {
        /**
         * A [FlowMultiplexerFactory] constructing a [MaxMinFlowMultiplexer].
         */
        private val MAX_MIN_FACTORY = FlowMultiplexerFactory { engine, listener -> MaxMinFlowMultiplexer(engine, listener) }

        /**
         * A [FlowMultiplexerFactory] constructing a [ForwardingFlowMultiplexer].
         */
        private val FORWARDING_FACTORY = FlowMultiplexerFactory { engine, listener -> ForwardingFlowMultiplexer(engine, listener) }

        /**
         * Return a [FlowMultiplexerFactory] that returns [MaxMinFlowMultiplexer] instances.
         */
        @JvmStatic
        public fun maxMinMultiplexer(): FlowMultiplexerFactory = MAX_MIN_FACTORY

        /**
         * Return a [ForwardingFlowMultiplexer] that returns [ForwardingFlowMultiplexer] instances.
         */
        @JvmStatic
        public fun forwardingMultiplexer(): FlowMultiplexerFactory = FORWARDING_FACTORY
    }
}
