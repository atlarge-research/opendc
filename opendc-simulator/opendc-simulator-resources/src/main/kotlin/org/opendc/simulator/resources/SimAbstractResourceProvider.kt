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

/**
 * Abstract implementation of the [SimResourceProvider] which can be re-used by other implementations.
 */
public abstract class SimAbstractResourceProvider(
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem?,
    initialCapacity: Double
) : SimResourceProvider {
    /**
     * A flag to indicate that the resource provider is active.
     */
    public override val isActive: Boolean
        get() = ctx != null

    /**
     * The capacity of the resource.
     */
    public override var capacity: Double = initialCapacity
        set(value) {
            field = value
            ctx?.capacity = value
        }

    /**
     * The current processing speed of the resource.
     */
    public override val speed: Double
        get() = ctx?.speed ?: 0.0

    /**
     * The resource processing speed demand at this instant.
     */
    public override val demand: Double
        get() = ctx?.demand ?: 0.0

    /**
     * The resource counters to track the execution metrics of the resource.
     */
    public override val counters: SimResourceCounters
        get() = _counters
    private val _counters = SimResourceCountersImpl()

    /**
     * The [SimResourceControllableContext] that is currently running.
     */
    protected var ctx: SimResourceControllableContext? = null
        private set

    /**
     * Construct the [SimResourceProviderLogic] instance for a new consumer.
     */
    protected abstract fun createLogic(): SimResourceProviderLogic

    /**
     * Start the specified [SimResourceControllableContext].
     */
    protected open fun start(ctx: SimResourceControllableContext) {
        ctx.start()
    }

    /**
     * Update the counters of the resource provider.
     */
    protected fun updateCounters(ctx: SimResourceContext, work: Double) {
        if (work <= 0.0) {
            return
        }

        val counters = _counters
        val remainingWork = ctx.remainingWork
        counters.demand += work
        counters.actual += work - remainingWork
        counters.overcommit += remainingWork
    }

    final override fun startConsumer(consumer: SimResourceConsumer) {
        check(ctx == null) { "Resource is in invalid state" }
        val ctx = interpreter.newContext(consumer, createLogic(), parent)

        ctx.capacity = capacity
        this.ctx = ctx

        start(ctx)
    }

    final override fun interrupt() {
        ctx?.interrupt()
    }

    final override fun cancel() {
        val ctx = ctx
        if (ctx != null) {
            this.ctx = null
            ctx.close()
        }
    }

    override fun toString(): String = "SimAbstractResourceProvider[capacity=$capacity]"
}
