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

package org.opendc.simulator.resources

import org.opendc.simulator.resources.impl.SimResourceCountersImpl
import java.time.Clock

/**
 * A class that acts as a [SimResourceConsumer] and [SimResourceProvider] at the same time.
 *
 * @param isCoupled A flag to indicate that the transformer will exit when the resource consumer exits.
 */
public class SimResourceForwarder(private val isCoupled: Boolean = false) : SimResourceConsumer, SimResourceProvider, AutoCloseable {
    /**
     * The delegate [SimResourceConsumer].
     */
    private var delegate: SimResourceConsumer? = null

    /**
     * A flag to indicate that the delegate was started.
     */
    private var hasDelegateStarted: Boolean = false

    /**
     * The exposed [SimResourceContext].
     */
    private val _ctx = object : SimResourceContext {
        override val clock: Clock
            get() = _innerCtx!!.clock

        override val capacity: Double
            get() = _innerCtx?.capacity ?: 0.0

        override val demand: Double
            get() = _innerCtx?.demand ?: 0.0

        override val speed: Double
            get() = _innerCtx?.speed ?: 0.0

        override fun interrupt() {
            _innerCtx?.interrupt()
        }

        override fun push(rate: Double) {
            _innerCtx?.push(rate)
            _limit = rate
        }

        override fun close() {
            val delegate = checkNotNull(delegate) { "Delegate not active" }

            if (isCoupled)
                _innerCtx?.close()
            else
                _innerCtx?.push(0.0)

            // Warning: resumption of the continuation might change the entire state of the forwarder. Make sure we
            // reset beforehand the existing state and check whether it has been updated afterwards
            reset()

            delegate.onEvent(this, SimResourceEvent.Exit)
        }
    }

    /**
     * The [SimResourceContext] in which the forwarder runs.
     */
    private var _innerCtx: SimResourceContext? = null

    override val isActive: Boolean
        get() = delegate != null

    override val capacity: Double
        get() = _ctx.capacity

    override val speed: Double
        get() = _ctx.speed

    override val demand: Double
        get() = _ctx.demand

    override val counters: SimResourceCounters
        get() = _counters
    private val _counters = SimResourceCountersImpl()

    override fun startConsumer(consumer: SimResourceConsumer) {
        check(delegate == null) { "Resource transformer already active" }

        delegate = consumer

        // Interrupt the provider to replace the consumer
        interrupt()
    }

    override fun interrupt() {
        _ctx.interrupt()
    }

    override fun cancel() {
        val delegate = delegate
        val ctx = _innerCtx

        if (delegate != null) {
            this.delegate = null

            if (ctx != null) {
                delegate.onEvent(this._ctx, SimResourceEvent.Exit)
            }
        }
    }

    override fun close() {
        val ctx = _innerCtx

        if (ctx != null) {
            this._innerCtx = null
            ctx.interrupt()
        }
    }

    override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
        val delegate = delegate

        if (!hasDelegateStarted) {
            start()
        }

        updateCounters(ctx, delta)

        return delegate?.onNext(this._ctx, now, delta) ?: Long.MAX_VALUE
    }

    override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
        when (event) {
            SimResourceEvent.Start -> {
                _innerCtx = ctx
            }
            SimResourceEvent.Exit -> {
                _innerCtx = null

                val delegate = delegate
                if (delegate != null) {
                    reset()
                    delegate.onEvent(this._ctx, SimResourceEvent.Exit)
                }
            }
            else -> delegate?.onEvent(this._ctx, event)
        }
    }

    override fun onFailure(ctx: SimResourceContext, cause: Throwable) {
        _innerCtx = null

        val delegate = delegate
        if (delegate != null) {
            reset()
            delegate.onFailure(this._ctx, cause)
        }
    }

    /**
     * Start the delegate.
     */
    private fun start() {
        val delegate = delegate ?: return
        delegate.onEvent(checkNotNull(_innerCtx), SimResourceEvent.Start)

        hasDelegateStarted = true
    }

    /**
     * Reset the delegate.
     */
    private fun reset() {
        delegate = null
        hasDelegateStarted = false
    }

    /**
     * The requested speed.
     */
    private var _limit: Double = 0.0

    /**
     * Update the resource counters for the transformer.
     */
    private fun updateCounters(ctx: SimResourceContext, delta: Long) {
        if (delta <= 0) {
            return
        }

        val counters = _counters
        val deltaS = delta / 1000.0
        val work = _limit * deltaS
        val actualWork = ctx.speed * deltaS
        counters.demand += work
        counters.actual += actualWork
        counters.overcommit += (work - actualWork)
    }
}
