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
 * A [SimResourceFlow] that transforms the resource commands emitted by the resource commands to the resource provider.
 *
 * @param isCoupled A flag to indicate that the transformer will exit when the resource consumer exits.
 * @param transform The function to transform the received resource command.
 */
public class SimResourceTransformer(
    private val isCoupled: Boolean = false,
    private val transform: (SimResourceContext, Long) -> Long
) : SimResourceFlow, AutoCloseable {
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
    private val ctx = object : SimResourceContext {
        override val clock: Clock
            get() = _ctx!!.clock

        override val capacity: Double
            get() = _ctx?.capacity ?: 0.0

        override val demand: Double
            get() = _ctx?.demand ?: 0.0

        override val speed: Double
            get() = _ctx?.speed ?: 0.0

        override fun interrupt() {
            _ctx?.interrupt()
        }

        override fun push(rate: Double) {
            _ctx?.push(rate)
            _limit = rate
        }

        override fun close() {
            val delegate = checkNotNull(delegate) { "Delegate not active" }

            // Warning: resumption of the continuation might change the entire state of the forwarder. Make sure we
            // reset beforehand the existing state and check whether it has been updated afterwards
            reset()

            delegate.onEvent(this, SimResourceEvent.Exit)

            if (isCoupled)
                _ctx?.close()
            else
                _ctx?.push(0.0)
        }
    }

    /**
     * The [SimResourceContext] in which the forwarder runs.
     */
    private var _ctx: SimResourceContext? = null

    override val isActive: Boolean
        get() = delegate != null

    override val capacity: Double
        get() = ctx.capacity

    override val speed: Double
        get() = ctx.speed

    override val demand: Double
        get() = ctx.demand

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
        ctx.interrupt()
    }

    override fun cancel() {
        val delegate = delegate
        val ctx = _ctx

        if (delegate != null) {
            this.delegate = null

            if (ctx != null) {
                delegate.onEvent(this.ctx, SimResourceEvent.Exit)
            }
        }
    }

    override fun close() {
        val ctx = _ctx

        if (ctx != null) {
            this._ctx = null
            ctx.interrupt()
        }
    }

    override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
        val delegate = delegate

        if (!hasDelegateStarted) {
            start()
        }

        updateCounters(ctx, delta)

        return if (delegate != null) {
            transform(ctx, delegate.onNext(this.ctx, now, delta))
        } else {
            Long.MAX_VALUE
        }
    }

    override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
        when (event) {
            SimResourceEvent.Start -> {
                _ctx = ctx
            }
            SimResourceEvent.Exit -> {
                _ctx = null

                val delegate = delegate
                if (delegate != null) {
                    reset()
                    delegate.onEvent(this.ctx, SimResourceEvent.Exit)
                }
            }
            else -> delegate?.onEvent(this.ctx, event)
        }
    }

    override fun onFailure(ctx: SimResourceContext, cause: Throwable) {
        _ctx = null

        val delegate = delegate
        if (delegate != null) {
            reset()
            delegate.onFailure(this.ctx, cause)
        }
    }

    /**
     * Start the delegate.
     */
    private fun start() {
        val delegate = delegate ?: return
        delegate.onEvent(checkNotNull(_ctx), SimResourceEvent.Start)

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
        val counters = _counters
        val deltaS = delta / 1000.0
        val work = _limit * deltaS
        val actualWork = ctx.speed * deltaS
        counters.demand += work
        counters.actual += actualWork
        counters.overcommit += (work - actualWork)
    }
}

/**
 * Constructs a [SimResourceTransformer] that forwards the received resource command with an identity transform.
 *
 * @param isCoupled A flag to indicate that the transformer will exit when the resource consumer exits.
 */
public fun SimResourceForwarder(isCoupled: Boolean = false): SimResourceTransformer {
    return SimResourceTransformer(isCoupled) { _, command -> command }
}
