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
 * A helper class to construct a [SimResourceProvider] which forwards the requests to a [SimResourceConsumer].
 */
public class SimResourceForwarder<R : SimResource>(override val resource: R) :
    SimResourceProvider<R>, SimResourceConsumer<R> {
    /**
     * The [SimResourceContext] in which the forwarder runs.
     */
    private var ctx: SimResourceContext<R>? = null

    /**
     * The delegate [SimResourceConsumer].
     */
    private var delegate: SimResourceConsumer<R>? = null

    /**
     * A flag to indicate that the delegate was started.
     */
    private var hasDelegateStarted: Boolean = false

    /**
     * The remaining amount of work last cycle.
     */
    private var remainingWork: Double = 0.0

    /**
     * The state of the forwarder.
     */
    override var state: SimResourceState = SimResourceState.Pending
        private set

    override fun startConsumer(consumer: SimResourceConsumer<R>) {
        check(state == SimResourceState.Pending) { "Resource is in invalid state" }

        state = SimResourceState.Active
        delegate = consumer
        interrupt()
    }

    override fun interrupt() {
        ctx?.interrupt()
    }

    override fun cancel() {
        val delegate = delegate
        val ctx = ctx

        state = SimResourceState.Pending

        if (delegate != null && ctx != null) {
            this.delegate = null
            delegate.onFinish(ctx)
        }
    }

    override fun close() {
        val ctx = ctx

        state = SimResourceState.Stopped

        if (ctx != null) {
            this.ctx = null
            ctx.interrupt()
        }
    }

    override fun onStart(ctx: SimResourceContext<R>) {
        this.ctx = ctx
    }

    override fun onNext(ctx: SimResourceContext<R>, capacity: Double, remainingWork: Double): SimResourceCommand {
        val delegate = delegate
        this.remainingWork = remainingWork

        if (!hasDelegateStarted) {
            start()
        }

        return if (state == SimResourceState.Stopped) {
            SimResourceCommand.Exit
        } else if (delegate != null) {
            val command = delegate.onNext(ctx, capacity, remainingWork)
            if (command == SimResourceCommand.Exit) {
                // Warning: resumption of the continuation might change the entire state of the forwarder. Make sure we
                // reset beforehand the existing state and check whether it has been updated afterwards
                reset()

                delegate.onFinish(ctx)

                if (state == SimResourceState.Stopped)
                    SimResourceCommand.Exit
                else
                    onNext(ctx, capacity, 0.0)
            } else {
                command
            }
        } else {
            SimResourceCommand.Idle()
        }
    }

    override fun onFinish(ctx: SimResourceContext<R>, cause: Throwable?) {
        this.ctx = null

        val delegate = delegate
        if (delegate != null) {
            reset()
            delegate.onFinish(ctx, cause)
        }
    }

    /**
     * Start the delegate.
     */
    private fun start() {
        val delegate = delegate ?: return
        delegate.onStart(checkNotNull(ctx))

        hasDelegateStarted = true
    }

    /**
     * Reset the delegate.
     */
    private fun reset() {
        delegate = null
        hasDelegateStarted = false

        if (state != SimResourceState.Stopped) {
            state = SimResourceState.Pending
        }
    }
}
