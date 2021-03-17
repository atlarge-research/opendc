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

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

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
     * A flag to indicate that the forwarder is closed.
     */
    private var isClosed: Boolean = false

    /**
     * The continuation to resume after consumption.
     */
    private var cont: Continuation<Unit>? = null

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

    override suspend fun consume(consumer: SimResourceConsumer<R>) {
        check(!isClosed) { "Lifecycle of forwarder has ended" }
        check(cont == null) { "Run should not be called concurrently" }

        return suspendCancellableCoroutine { cont ->
            this.cont = cont
            this.delegate = consumer

            cont.invokeOnCancellation { reset() }

            ctx?.interrupt()
        }
    }

    override fun interrupt() {
        ctx?.interrupt()
    }

    override fun close() {
        isClosed = true
        interrupt()
        ctx = null
    }

    override fun onStart(ctx: SimResourceContext<R>): SimResourceCommand {
        this.ctx = ctx

        return onNext(ctx, 0.0)
    }

    override fun onNext(ctx: SimResourceContext<R>, remainingWork: Double): SimResourceCommand {
        this.remainingWork = remainingWork

        return if (isClosed) {
            SimResourceCommand.Exit
        } else if (!hasDelegateStarted) {
            start()
        } else {
            next()
        }
    }

    /**
     * Start the delegate.
     */
    private fun start(): SimResourceCommand {
        val delegate = delegate ?: return SimResourceCommand.Idle()
        val command = delegate.onStart(checkNotNull(ctx))

        hasDelegateStarted = true

        return forward(command)
    }

    /**
     * Obtain the next command to process.
     */
    private fun next(): SimResourceCommand {
        val delegate = delegate
        return forward(delegate?.onNext(checkNotNull(ctx), remainingWork) ?: SimResourceCommand.Idle())
    }

    /**
     * Forward the specified [command].
     */
    private fun forward(command: SimResourceCommand): SimResourceCommand {
        return if (command == SimResourceCommand.Exit) {
            val cont = checkNotNull(cont)

            // Warning: resumption of the continuation might change the entire state of the forwarder. Make sure we
            // reset beforehand the existing state and check whether it has been updated afterwards
            reset()
            cont.resume(Unit)

            if (isClosed)
                SimResourceCommand.Exit
            else
                start()
        } else {
            command
        }
    }

    /**
     * Reset the delegate.
     */
    private fun reset() {
        cont = null
        delegate = null
        hasDelegateStarted = false
    }
}
