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

import mu.KotlinLogging
import org.opendc.simulator.flow.internal.FlowCountersImpl

/**
 * A class that acts as a [FlowSource] and [FlowConsumer] at the same time.
 *
 * @param engine The [FlowEngine] the forwarder runs in.
 * @param isCoupled A flag to indicate that the transformer will exit when the resource consumer exits.
 */
public class FlowForwarder(private val engine: FlowEngine, private val isCoupled: Boolean = false) : FlowSource, FlowConsumer, AutoCloseable {
    /**
     * The logging instance of this connection.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The delegate [FlowSource].
     */
    private var delegate: FlowSource? = null

    /**
     * A flag to indicate that the delegate was started.
     */
    private var hasDelegateStarted: Boolean = false

    /**
     * The exposed [FlowConnection].
     */
    private val _ctx = object : FlowConnection {
        override val capacity: Double
            get() = _innerCtx?.capacity ?: 0.0

        override val demand: Double
            get() = _innerCtx?.demand ?: 0.0

        override val rate: Double
            get() = _innerCtx?.rate ?: 0.0

        override fun pull() {
            _innerCtx?.pull()
        }

        override fun push(rate: Double) {
            if (delegate == null) {
                return
            }

            _innerCtx?.push(rate)
            _demand = rate
        }

        override fun close() {
            val delegate = delegate ?: return
            val hasDelegateStarted = hasDelegateStarted

            // Warning: resumption of the continuation might change the entire state of the forwarder. Make sure we
            // reset beforehand the existing state and check whether it has been updated afterwards
            reset()

            if (hasDelegateStarted) {
                delegate.onEvent(this, engine.clock.millis(), FlowEvent.Exit)
            }
        }
    }

    /**
     * The [FlowConnection] in which the forwarder runs.
     */
    private var _innerCtx: FlowConnection? = null

    override val isActive: Boolean
        get() = delegate != null

    override val capacity: Double
        get() = _ctx.capacity

    override val rate: Double
        get() = _ctx.rate

    override val demand: Double
        get() = _ctx.demand

    override val counters: FlowCounters
        get() = _counters
    private val _counters = FlowCountersImpl()

    override fun startConsumer(source: FlowSource) {
        check(delegate == null) { "Forwarder already active" }

        delegate = source

        // Pull to replace the source
        pull()
    }

    override fun pull() {
        _ctx.pull()
    }

    override fun cancel() {
        _ctx.close()
    }

    override fun close() {
        val ctx = _innerCtx

        if (ctx != null) {
            this._innerCtx = null
            ctx.pull()
        }
    }

    override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
        val delegate = delegate

        if (!hasDelegateStarted) {
            start()
        }

        updateCounters(conn, delta)

        return try {
            delegate?.onPull(this._ctx, now, delta) ?: Long.MAX_VALUE
        } catch (cause: Throwable) {
            logger.error(cause) { "Uncaught exception" }

            reset()
            Long.MAX_VALUE
        }
    }

    override fun onEvent(conn: FlowConnection, now: Long, event: FlowEvent) {
        when (event) {
            FlowEvent.Start -> _innerCtx = conn
            FlowEvent.Exit -> {
                _innerCtx = null

                val delegate = delegate
                if (delegate != null) {
                    reset()

                    try {
                        delegate.onEvent(this._ctx, now, FlowEvent.Exit)
                    } catch (cause: Throwable) {
                        logger.error(cause) { "Uncaught exception" }
                    }
                }
            }
            else ->
                try {
                    delegate?.onEvent(this._ctx, now, event)
                } catch (cause: Throwable) {
                    logger.error(cause) { "Uncaught exception" }

                    _innerCtx = null
                    reset()
                }
        }
    }

    /**
     * Start the delegate.
     */
    private fun start() {
        val delegate = delegate ?: return

        try {
            delegate.onEvent(_ctx, engine.clock.millis(), FlowEvent.Start)
            hasDelegateStarted = true
        } catch (cause: Throwable) {
            logger.error(cause) { "Uncaught exception" }
            reset()
        }
    }

    /**
     * Reset the delegate.
     */
    private fun reset() {
        if (isCoupled)
            _innerCtx?.close()
        else
            _innerCtx?.push(0.0)

        delegate = null
        hasDelegateStarted = false
    }

    /**
     * The requested flow rate.
     */
    private var _demand: Double = 0.0

    /**
     * Update the flow counters for the transformer.
     */
    private fun updateCounters(ctx: FlowConnection, delta: Long) {
        if (delta <= 0) {
            return
        }

        val counters = _counters
        val deltaS = delta / 1000.0
        val work = _demand * deltaS
        val actualWork = ctx.rate * deltaS
        counters.demand += work
        counters.actual += actualWork
        counters.overcommit += (work - actualWork)
    }
}
