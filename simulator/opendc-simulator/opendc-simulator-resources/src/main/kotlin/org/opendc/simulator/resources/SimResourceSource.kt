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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opendc.utils.TimerScheduler
import java.time.Clock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

/**
 * A [SimResourceSource] represents a source for some resource of type [R] that provides bounded processing capacity.
 *
 * @param resource The resource to provide.
 * @param clock The virtual clock to track simulation time.
 */
public class SimResourceSource<R : SimResource>(
    override val resource: R,
    private val clock: Clock,
    private val scheduler: TimerScheduler<Any>
) : SimResourceProvider<R> {
    /**
     * The resource processing speed over time.
     */
    public val speed: StateFlow<Double>
        get() = _speed
    private val _speed = MutableStateFlow(0.0)

    /**
     * A flag to indicate that the resource was closed.
     */
    private var isClosed: Boolean = false

    /**
     * The current active consumer.
     */
    private var cont: CancellableContinuation<Unit>? = null

    /**
     * The [Context] that is currently running.
     */
    private var ctx: Context? = null

    override suspend fun consume(consumer: SimResourceConsumer<R>) {
        check(!isClosed) { "Lifetime of resource has ended." }
        check(cont == null) { "Run should not be called concurrently" }

        try {
            return suspendCancellableCoroutine { cont ->
                val ctx = Context(consumer, cont)

                this.cont = cont
                this.ctx = ctx

                ctx.start()
                cont.invokeOnCancellation {
                    ctx.stop()
                }
            }
        } finally {
            cont = null
            ctx = null
        }
    }

    override fun close() {
        isClosed = true
        cont?.cancel()
        cont = null
        ctx = null
    }

    override fun interrupt() {
        ctx?.interrupt()
    }

    /**
     * Internal implementation of [SimResourceContext] for this class.
     */
    private inner class Context(
        consumer: SimResourceConsumer<R>,
        val cont: Continuation<Unit>
    ) : SimAbstractResourceContext<R>(resource, clock, consumer) {
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

        override fun onFinish() {
            speed = 0.0
            scheduler.cancel(this)
            cont.resume(Unit)
        }

        override fun onFailure(cause: Throwable) {
            speed = 0.0
            scheduler.cancel(this)
            cont.resumeWithException(cause)
        }

        override fun toString(): String = "SimResourceSource.Context[resource=$resource]"
    }
}
