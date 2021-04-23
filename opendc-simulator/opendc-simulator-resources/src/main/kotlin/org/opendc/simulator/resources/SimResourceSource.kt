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

import kotlin.math.ceil
import kotlin.math.min

/**
 * A [SimResourceSource] represents a source for some resource of type [R] that provides bounded processing capacity.
 *
 * @param initialCapacity The initial capacity of the resource.
 * @param scheduler The scheduler to schedule the interrupts.
 */
public class SimResourceSource(
    initialCapacity: Double,
    private val scheduler: SimResourceScheduler
) : SimResourceProvider {
    /**
     * The current processing speed of the resource.
     */
    public val speed: Double
        get() = ctx?.speed ?: 0.0

    /**
     * The capacity of the resource.
     */
    public var capacity: Double = initialCapacity
        set(value) {
            field = value
            ctx?.capacity = value
        }

    /**
     * The [Context] that is currently running.
     */
    private var ctx: Context? = null

    override var state: SimResourceState = SimResourceState.Pending
        private set

    override fun startConsumer(consumer: SimResourceConsumer) {
        check(state == SimResourceState.Pending) { "Resource is in invalid state" }
        val ctx = Context(consumer)

        this.ctx = ctx
        this.state = SimResourceState.Active

        ctx.start()
    }

    override fun close() {
        cancel()
        state = SimResourceState.Stopped
    }

    override fun interrupt() {
        ctx?.interrupt()
    }

    override fun cancel() {
        val ctx = ctx
        if (ctx != null) {
            this.ctx = null
            ctx.stop()
        }

        if (state != SimResourceState.Stopped) {
            state = SimResourceState.Pending
        }
    }

    /**
     * Internal implementation of [SimResourceContext] for this class.
     */
    private inner class Context(consumer: SimResourceConsumer) : SimAbstractResourceContext(capacity, scheduler, consumer) {
        override fun onIdle(deadline: Long) {
            // Do not resume if deadline is "infinite"
            if (deadline != Long.MAX_VALUE) {
                scheduler.schedule(this, deadline)
            }
        }

        override fun onConsume(work: Double, limit: Double, deadline: Long) {
            val until = min(deadline, clock.millis() + getDuration(work, speed))
            scheduler.schedule(this, until)
        }

        override fun onFinish() {
            cancel()

            ctx = null

            if (this@SimResourceSource.state != SimResourceState.Stopped) {
                this@SimResourceSource.state = SimResourceState.Pending
            }
        }

        override fun toString(): String = "SimResourceSource.Context[capacity=$capacity]"
    }

    /**
     * Compute the duration that a resource consumption will take with the specified [speed].
     */
    private fun getDuration(work: Double, speed: Double): Long {
        return ceil(work / speed * 1000).toLong()
    }
}
