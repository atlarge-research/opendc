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

import org.opendc.simulator.flow.internal.D_MS_TO_S
import org.opendc.simulator.flow.internal.MutableFlowCounters

/**
 * A [FlowSink] represents a sink with a fixed capacity.
 *
 * @param initialCapacity The initial capacity of the resource.
 * @param engine The engine that is used for driving the flow simulation.
 * @param parent The parent flow system.
 */
public class FlowSink(
    private val engine: FlowEngine,
    initialCapacity: Double,
    private val parent: FlowConvergenceListener? = null
) : FlowConsumer {
    /**
     * A flag to indicate that the flow consumer is active.
     */
    public override val isActive: Boolean
        get() = _ctx != null

    /**
     * The capacity of the consumer.
     */
    public override var capacity: Double = initialCapacity
        set(value) {
            field = value
            _ctx?.capacity = value
        }

    /**
     * The current processing rate of the consumer.
     */
    public override val rate: Double
        get() = _ctx?.rate ?: 0.0

    /**
     * The flow processing rate demand at this instant.
     */
    public override val demand: Double
        get() = _ctx?.demand ?: 0.0

    /**
     * The flow counters to track the flow metrics of the consumer.
     */
    public override val counters: FlowCounters
        get() = _counters
    private val _counters = MutableFlowCounters()

    /**
     * The current active [FlowConsumerLogic] of this sink.
     */
    private var _ctx: FlowConsumerContext? = null

    override fun startConsumer(source: FlowSource) {
        check(_ctx == null) { "Consumer is in invalid state" }

        val ctx = engine.newContext(source, Logic(parent, _counters))
        _ctx = ctx

        ctx.capacity = capacity
        if (parent != null) {
            ctx.shouldConsumerConverge = true
        }

        ctx.start()
    }

    override fun pull() {
        _ctx?.pull()
    }

    override fun cancel() {
        _ctx?.close()
    }

    override fun toString(): String = "FlowSink[capacity=$capacity]"

    /**
     * [FlowConsumerLogic] of a sink.
     */
    private inner class Logic(private val parent: FlowConvergenceListener?, private val counters: MutableFlowCounters) : FlowConsumerLogic {
        override fun onPush(
            ctx: FlowConsumerContext,
            now: Long,
            delta: Long,
            rate: Double
        ) {
            updateCounters(ctx, delta, rate, ctx.capacity)
        }

        override fun onFinish(ctx: FlowConsumerContext, now: Long, delta: Long, cause: Throwable?) {
            updateCounters(ctx, delta, 0.0, 0.0)

            _ctx = null
        }

        override fun onConverge(ctx: FlowConsumerContext, now: Long, delta: Long) {
            parent?.onConverge(now, delta)
        }

        /**
         * The previous demand and capacity for the consumer.
         */
        private val _previous = DoubleArray(2)

        /**
         * Update the counters of the flow consumer.
         */
        private fun updateCounters(ctx: FlowConnection, delta: Long, nextDemand: Double, nextCapacity: Double) {
            val counters = counters
            val previous = _previous
            val demand = previous[0]
            val capacity = previous[1]

            previous[0] = nextDemand
            previous[1] = nextCapacity

            if (delta <= 0) {
                return
            }

            val deltaS = delta * D_MS_TO_S
            val total = demand * deltaS
            val work = capacity * deltaS
            val actualWork = ctx.rate * deltaS

            counters.increment(work, actualWork, (total - actualWork), 0.0)
        }
    }
}
