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
import org.opendc.simulator.flow.internal.D_MS_TO_S
import org.opendc.simulator.flow.internal.MutableFlowCounters
import kotlin.math.max

/**
 * A class that acts as a [FlowSource] and [FlowConsumer] at the same time.
 *
 * @param engine The [FlowEngine] the forwarder runs in.
 * @param listener The convergence lister to use.
 * @param isCoupled A flag to indicate that the transformer will exit when the resource consumer exits.
 */
public class FlowForwarder(
    private val engine: FlowEngine,
    private val listener: FlowConvergenceListener? = null,
    private val isCoupled: Boolean = false
) : FlowSource, FlowConsumer, AutoCloseable {
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
        override var shouldSourceConverge: Boolean = false
            set(value) {
                field = value
                _innerCtx?.shouldSourceConverge = value
            }

        override val capacity: Double
            get() = _innerCtx?.capacity ?: 0.0

        override val demand: Double
            get() = _innerCtx?.demand ?: 0.0

        override val rate: Double
            get() = _innerCtx?.rate ?: 0.0

        override fun pull() {
            _innerCtx?.pull()
        }

        override fun pull(now: Long) {
            _innerCtx?.pull(now)
        }

        @JvmField var lastPull = Long.MAX_VALUE

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
                val now = engine.clock.millis()
                val delta = max(0, now - lastPull)
                delegate.onStop(this, now, delta)
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
    private val _counters = MutableFlowCounters()

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

    override fun onStart(conn: FlowConnection, now: Long) {
        _innerCtx = conn

        if (listener != null || _ctx.shouldSourceConverge) {
            conn.shouldSourceConverge = true
        }
    }

    override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
        _innerCtx = null

        val delegate = delegate
        if (delegate != null) {
            reset()

            try {
                delegate.onStop(this._ctx, now, delta)
            } catch (cause: Throwable) {
                logger.error(cause) { "Uncaught exception" }
            }
        }
    }

    override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
        val delegate = delegate

        if (!hasDelegateStarted) {
            start()
        }

        _ctx.lastPull = now
        updateCounters(conn, delta)

        return try {
            delegate?.onPull(_ctx, now, delta) ?: Long.MAX_VALUE
        } catch (cause: Throwable) {
            logger.error(cause) { "Uncaught exception" }

            reset()
            Long.MAX_VALUE
        }
    }

    override fun onConverge(conn: FlowConnection, now: Long, delta: Long) {
        try {
            delegate?.onConverge(this._ctx, now, delta)
            listener?.onConverge(now, delta)
        } catch (cause: Throwable) {
            logger.error(cause) { "Uncaught exception" }

            _innerCtx = null
            reset()
        }
    }

    /**
     * Start the delegate.
     */
    private fun start() {
        val delegate = delegate ?: return

        try {
            delegate.onStart(_ctx, engine.clock.millis())
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
        val deltaS = delta * D_MS_TO_S
        val total = ctx.capacity * deltaS
        val work = _demand * deltaS
        val actualWork = ctx.rate * deltaS

        counters.increment(work, actualWork, (total - actualWork), 0.0)
    }
}
