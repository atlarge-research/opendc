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

import java.time.Clock

/**
 * Abstract implementation of [SimResourceAggregator].
 */
public abstract class SimAbstractResourceAggregator(private val clock: Clock) : SimResourceAggregator {
    /**
     * The available resource provider contexts.
     */
    protected val inputContexts: Set<SimResourceContext>
        get() = _inputContexts
    private val _inputContexts = mutableSetOf<SimResourceContext>()

    /**
     * The output context.
     */
    protected val outputContext: SimResourceContext
        get() = context

    /**
     * The commands to submit to the underlying input resources.
     */
    protected val commands: MutableMap<SimResourceContext, SimResourceCommand> = mutableMapOf()

    /**
     * This method is invoked when the resource consumer consumes resources.
     */
    protected abstract fun doConsume(work: Double, limit: Double, deadline: Long)

    /**
     * This method is invoked when the resource consumer enters an idle state.
     */
    protected open fun doIdle(deadline: Long) {
        for (input in inputContexts) {
            commands[input] = SimResourceCommand.Idle(deadline)
        }
    }

    /**
     * This method is invoked when the resource consumer finishes processing.
     */
    protected open fun doFinish(cause: Throwable?) {
        for (input in inputContexts) {
            commands[input] = SimResourceCommand.Exit
        }
    }

    /**
     * This method is invoked when an input context is started.
     */
    protected open fun onContextStarted(ctx: SimResourceContext) {
        _inputContexts.add(ctx)
    }

    protected open fun onContextFinished(ctx: SimResourceContext) {
        assert(_inputContexts.remove(ctx)) { "Lost context" }
    }

    override fun addInput(input: SimResourceProvider) {
        check(output.state != SimResourceState.Stopped) { "Aggregator has been stopped" }

        val consumer = Consumer()
        _inputs.add(input)
        input.startConsumer(consumer)
    }

    override fun close() {
        output.close()
    }

    override val output: SimResourceProvider
        get() = _output
    private val _output = SimResourceForwarder()

    override val inputs: Set<SimResourceProvider>
        get() = _inputs
    private val _inputs = mutableSetOf<SimResourceProvider>()

    private val context = object : SimAbstractResourceContext(inputContexts.sumByDouble { it.capacity }, clock, _output) {
        override val remainingWork: Double
            get() {
                val now = clock.millis()

                return if (_remainingWorkFlush < now) {
                    _remainingWorkFlush = now
                    _inputContexts.sumByDouble { it.remainingWork }.also { _remainingWork = it }
                } else {
                    _remainingWork
                }
            }
        private var _remainingWork: Double = 0.0
        private var _remainingWorkFlush: Long = Long.MIN_VALUE

        override fun interrupt() {
            super.interrupt()

            interruptAll()
        }

        override fun onConsume(work: Double, limit: Double, deadline: Long) = doConsume(work, limit, deadline)

        override fun onIdle(deadline: Long) = doIdle(deadline)

        override fun onFinish(cause: Throwable?) {
            doFinish(cause)

            super.onFinish(cause)

            interruptAll()
        }
    }

    /**
     * A flag to indicate that an interrupt is active.
     */
    private var isInterrupting: Boolean = false

    /**
     * Schedule the work over the input resources.
     */
    private fun doSchedule() {
        context.flush(isIntermediate = true)
        interruptAll()
    }

    /**
     * Interrupt all inputs.
     */
    private fun interruptAll() {
        // Prevent users from interrupting the resource while they are constructing their next command, as this will
        // only lead to infinite recursion.
        if (isInterrupting) {
            return
        }

        try {
            isInterrupting = true

            val iterator = _inputs.iterator()
            while (iterator.hasNext()) {
                val input = iterator.next()
                input.interrupt()

                if (input.state != SimResourceState.Active) {
                    iterator.remove()
                }
            }
        } finally {
            isInterrupting = false
        }
    }

    /**
     * An internal [SimResourceConsumer] implementation for aggregator inputs.
     */
    private inner class Consumer : SimResourceConsumer {
        override fun onStart(ctx: SimResourceContext) {
            onContextStarted(ctx)
            onCapacityChanged(ctx, false)

            // Make sure we initialize the output if we have not done so yet
            if (context.state == SimResourceState.Pending) {
                context.start()
            }
        }

        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            doSchedule()

            return commands[ctx] ?: SimResourceCommand.Idle()
        }

        override fun onCapacityChanged(ctx: SimResourceContext, isThrottled: Boolean) {
            // Adjust capacity of output resource
            context.capacity = inputContexts.sumByDouble { it.capacity }
        }

        override fun onFinish(ctx: SimResourceContext, cause: Throwable?) {
            onContextFinished(ctx)

            super.onFinish(ctx, cause)
        }
    }
}
