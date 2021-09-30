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
) : AbstractFlowConsumer(engine, initialCapacity) {

    override fun start(ctx: FlowConsumerContext) {
        if (parent != null) {
            ctx.shouldConsumerConverge = true
        }
        super.start(ctx)
    }

    override fun createLogic(): FlowConsumerLogic {
        return object : FlowConsumerLogic {
            override fun onPush(
                ctx: FlowConsumerContext,
                now: Long,
                delta: Long,
                rate: Double
            ) {
                updateCounters(ctx, delta)
            }

            override fun onFinish(ctx: FlowConsumerContext, now: Long, delta: Long, cause: Throwable?) {
                updateCounters(ctx, delta)
                cancel()
            }

            override fun onConverge(ctx: FlowConsumerContext, now: Long, delta: Long) {
                parent?.onConverge(now, delta)
            }
        }
    }

    override fun toString(): String = "FlowSink[capacity=$capacity]"
}
