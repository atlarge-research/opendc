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
 * Abstract implementation of the [SimResourceProvider] which can be re-used by other implementations.
 */
public abstract class SimAbstractResourceProvider(
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem? = null,
    initialCapacity: Double
) : SimResourceProvider {
    /**
     * The capacity of the resource.
     */
    public open var capacity: Double = initialCapacity
        protected set(value) {
            field = value
            ctx?.capacity = value
        }

    /**
     * The [SimResourceControllableContext] that is currently running.
     */
    protected var ctx: SimResourceControllableContext? = null

    /**
     * The state of the resource provider.
     */
    final override var state: SimResourceState = SimResourceState.Pending
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

    final override fun startConsumer(consumer: SimResourceConsumer) {
        check(state == SimResourceState.Pending) { "Resource is in invalid state" }
        val ctx = interpreter.newContext(consumer, createLogic(), parent)

        ctx.capacity = capacity
        this.ctx = ctx
        this.state = SimResourceState.Active

        start(ctx)
    }

    override fun close() {
        cancel()
        state = SimResourceState.Stopped
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

        if (state != SimResourceState.Stopped) {
            state = SimResourceState.Pending
        }
    }

    override fun toString(): String = "SimAbstractResourceProvider[capacity=$capacity]"
}
