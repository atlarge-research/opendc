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
 * A [SimResourceFlow] that transforms the resource commands emitted by the resource commands to the resource provider.
 *
 * @param isCoupled A flag to indicate that the transformer will exit when the resource consumer exits.
 * @param transform The function to transform the received resource command.
 */
public class SimResourceTransformer(
    private val isCoupled: Boolean = false,
    private val transform: (SimResourceContext, SimResourceCommand) -> SimResourceCommand
) : SimResourceFlow {
    /**
     * The [SimResourceContext] in which the forwarder runs.
     */
    private var ctx: SimResourceContext? = null

    /**
     * The delegate [SimResourceConsumer].
     */
    private var delegate: SimResourceConsumer? = null

    /**
     * A flag to indicate that the delegate was started.
     */
    private var hasDelegateStarted: Boolean = false

    /**
     * The state of the forwarder.
     */
    override var state: SimResourceState = SimResourceState.Pending
        private set

    override fun startConsumer(consumer: SimResourceConsumer) {
        check(state == SimResourceState.Pending) { "Resource is in invalid state" }

        state = SimResourceState.Active
        delegate = consumer

        // Interrupt the provider to replace the consumer
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
            delegate.onEvent(ctx, SimResourceEvent.Exit)
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

    override fun onNext(ctx: SimResourceContext): SimResourceCommand {
        val delegate = delegate

        if (!hasDelegateStarted) {
            start()
        }

        return if (state == SimResourceState.Stopped) {
            SimResourceCommand.Exit
        } else if (delegate != null) {
            val command = transform(ctx, delegate.onNext(ctx))
            if (command == SimResourceCommand.Exit) {
                // Warning: resumption of the continuation might change the entire state of the forwarder. Make sure we
                // reset beforehand the existing state and check whether it has been updated afterwards
                reset()

                delegate.onEvent(ctx, SimResourceEvent.Exit)

                if (isCoupled || state == SimResourceState.Stopped)
                    SimResourceCommand.Exit
                else
                    onNext(ctx)
            } else {
                command
            }
        } else {
            SimResourceCommand.Idle()
        }
    }

    override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
        when (event) {
            SimResourceEvent.Start -> {
                this.ctx = ctx
            }
            SimResourceEvent.Exit -> {
                this.ctx = null

                val delegate = delegate
                if (delegate != null) {
                    reset()
                    delegate.onEvent(ctx, SimResourceEvent.Exit)
                }
            }
            else -> delegate?.onEvent(ctx, event)
        }
    }

    override fun onFailure(ctx: SimResourceContext, cause: Throwable) {
        this.ctx = null

        val delegate = delegate
        if (delegate != null) {
            reset()
            delegate.onFailure(ctx, cause)
        }
    }

    /**
     * Start the delegate.
     */
    private fun start() {
        val delegate = delegate ?: return
        delegate.onEvent(checkNotNull(ctx), SimResourceEvent.Start)

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

/**
 * Constructs a [SimResourceTransformer] that forwards the received resource command with an identity transform.
 *
 * @param isCoupled A flag to indicate that the transformer will exit when the resource consumer exits.
 */
public fun SimResourceForwarder(isCoupled: Boolean = false): SimResourceTransformer {
    return SimResourceTransformer(isCoupled) { _, command -> command }
}
