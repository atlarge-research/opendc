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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opendc.utils.TimerScheduler
import java.time.Clock
import kotlin.math.min

/**
 * A [SimResourceSource] represents a source for some resource of type [R] that provides bounded processing capacity.
 *
 * @param initialCapacity The initial capacity of the resource.
 * @param clock The virtual clock to track simulation time.
 * @param scheduler The scheduler to schedule the interrupts.
 */
public class SimResourceSource(
    private val initialCapacity: Double,
    private val clock: Clock,
    private val scheduler: TimerScheduler<Any>
) : SimResourceProvider {
    /**
     * The resource processing speed over time.
     */
    public val speed: StateFlow<Double>
        get() = _speed
    private val _speed = MutableStateFlow(0.0)

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
    private inner class Context(consumer: SimResourceConsumer) : SimAbstractResourceContext(clock, consumer) {
        override val capacity: Double = initialCapacity

        /**
         * The processing speed of the resource.
         */
        private var speed: Double = 0.0
            set(value) {
                field = value
                _speed.value = field
            }

        override fun onIdle(deadline: Long) {
            speed = 0.0

            // Do not resume if deadline is "infinite"
            if (deadline != Long.MAX_VALUE) {
                scheduler.startSingleTimerTo(this, deadline) { flush() }
            }
        }

        override fun onConsume(work: Double, limit: Double, deadline: Long) {
            speed = getSpeed(limit)
            val until = min(deadline, clock.millis() + getDuration(work, speed))

            scheduler.startSingleTimerTo(this, until, ::flush)
        }

        override fun onFinish(cause: Throwable?) {
            speed = 0.0
            scheduler.cancel(this)
            cancel()

            super.onFinish(cause)
        }

        override fun toString(): String = "SimResourceSource.Context[capacity=$capacity]"
    }
}
