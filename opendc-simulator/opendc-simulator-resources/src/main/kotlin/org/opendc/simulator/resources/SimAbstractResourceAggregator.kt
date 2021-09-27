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

/**
 * Abstract implementation of [SimResourceAggregator].
 */
public abstract class SimAbstractResourceAggregator(
    interpreter: SimResourceInterpreter,
    parent: SimResourceSystem?
) : SimResourceAggregator {
    /**
     * This method is invoked when the resource consumer consumes resources.
     */
    protected abstract fun doConsume(limit: Double)

    /**
     * This method is invoked when the resource consumer finishes processing.
     */
    protected abstract fun doFinish()

    /**
     * This method is invoked when an input context is started.
     */
    protected abstract fun onInputStarted(input: SimResourceContext)

    /**
     * This method is invoked when an input is stopped.
     */
    protected abstract fun onInputFinished(input: SimResourceContext)

    /* SimResourceAggregator */
    override fun addInput(input: SimResourceProvider) {
        val consumer = Consumer()
        _inputs.add(input)
        _inputConsumers.add(consumer)
        input.startConsumer(consumer)
    }

    override val inputs: Set<SimResourceProvider>
        get() = _inputs
    private val _inputs = mutableSetOf<SimResourceProvider>()
    private val _inputConsumers = mutableListOf<Consumer>()

    /* SimResourceProvider */
    override val isActive: Boolean
        get() = _output.isActive

    override val capacity: Double
        get() = _output.capacity

    override val speed: Double
        get() = _output.speed

    override val demand: Double
        get() = _output.demand

    override val counters: SimResourceCounters
        get() = _output.counters

    override fun startConsumer(consumer: SimResourceConsumer) {
        _output.startConsumer(consumer)
    }

    override fun cancel() {
        _output.cancel()
    }

    override fun interrupt() {
        _output.interrupt()
    }

    private val _output = object : SimAbstractResourceProvider(interpreter, initialCapacity = 0.0) {
        override fun createLogic(): SimResourceProviderLogic {
            return object : SimResourceProviderLogic {

                override fun onConsume(ctx: SimResourceControllableContext, now: Long, delta: Long, limit: Double, duration: Long) {
                    updateCounters(ctx, delta)
                    doConsume(limit)
                }

                override fun onFinish(ctx: SimResourceControllableContext, now: Long, delta: Long) {
                    updateCounters(ctx, delta)
                    doFinish()
                }

                override fun onConverge(ctx: SimResourceControllableContext, now: Long, delta: Long) {
                    parent?.onConverge(now)
                }
            }
        }
    }

    /**
     * An internal [SimResourceConsumer] implementation for aggregator inputs.
     */
    private inner class Consumer : SimResourceConsumer {
        /**
         * The resource context associated with the input.
         */
        private var _ctx: SimResourceContext? = null

        private fun updateCapacity() {
            // Adjust capacity of output resource
            _output.capacity = _inputConsumers.sumOf { it._ctx?.capacity ?: 0.0 }
        }

        /* SimResourceConsumer */
        override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
            return Long.MAX_VALUE
        }

        override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
            when (event) {
                SimResourceEvent.Start -> {
                    _ctx = ctx
                    updateCapacity()

                    onInputStarted(ctx)
                }
                SimResourceEvent.Capacity -> updateCapacity()
                SimResourceEvent.Exit -> onInputFinished(ctx)
                else -> {}
            }
        }
    }
}
