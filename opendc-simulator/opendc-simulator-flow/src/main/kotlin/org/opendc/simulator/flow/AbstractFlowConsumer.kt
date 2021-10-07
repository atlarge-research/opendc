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

import org.opendc.simulator.flow.internal.MutableFlowCounters

/**
 * Abstract implementation of the [FlowConsumer] which can be re-used by other implementations.
 */
public abstract class AbstractFlowConsumer(private val engine: FlowEngine, initialCapacity: Double) : FlowConsumer {
    /**
     * A flag to indicate that the flow consumer is active.
     */
    public override val isActive: Boolean
        get() = ctx != null

    /**
     * The capacity of the consumer.
     */
    public override var capacity: Double = initialCapacity
        set(value) {
            field = value
            ctx?.capacity = value
        }

    /**
     * The current processing rate of the consumer.
     */
    public override val rate: Double
        get() = ctx?.rate ?: 0.0

    /**
     * The flow processing rate demand at this instant.
     */
    public override val demand: Double
        get() = ctx?.demand ?: 0.0

    /**
     * The [FlowConsumerContext] that is currently running.
     */
    protected var ctx: FlowConsumerContext? = null
        private set

    /**
     * Construct the [FlowConsumerLogic] instance for a new source.
     */
    protected abstract fun createLogic(): FlowConsumerLogic

    /**
     * Start the specified [FlowConsumerContext].
     */
    protected open fun start(ctx: FlowConsumerContext) {
        ctx.start()
    }

    /**
     * The previous demand for the consumer.
     */
    private var _previousDemand = 0.0
    private var _previousCapacity = 0.0

    /**
     * Update the counters of the flow consumer.
     */
    protected fun MutableFlowCounters.update(ctx: FlowConnection, delta: Long) {
        val demand = _previousDemand
        val capacity = _previousCapacity

        _previousDemand = ctx.demand
        _previousCapacity = ctx.capacity

        if (delta <= 0) {
            return
        }

        val deltaS = delta / 1000.0
        val total = demand * deltaS
        val work = capacity * deltaS
        val actualWork = ctx.rate * deltaS

        increment(work, actualWork, (total - actualWork), 0.0)
    }

    final override fun startConsumer(source: FlowSource) {
        check(ctx == null) { "Consumer is in invalid state" }
        val ctx = engine.newContext(source, createLogic())

        ctx.capacity = capacity
        this.ctx = ctx

        start(ctx)
    }

    final override fun pull() {
        ctx?.pull()
    }

    final override fun cancel() {
        val ctx = ctx
        if (ctx != null) {
            this.ctx = null
            ctx.close()
        }
    }

    override fun toString(): String = "AbstractFlowConsumer[capacity=$capacity]"
}
