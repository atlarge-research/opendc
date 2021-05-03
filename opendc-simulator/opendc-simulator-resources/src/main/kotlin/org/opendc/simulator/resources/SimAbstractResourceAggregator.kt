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
public abstract class SimAbstractResourceAggregator(private val scheduler: SimResourceScheduler) : SimResourceAggregator {
    /**
     * This method is invoked when the resource consumer consumes resources.
     */
    protected abstract fun doConsume(work: Double, limit: Double, deadline: Long)

    /**
     * This method is invoked when the resource consumer enters an idle state.
     */
    protected abstract fun doIdle(deadline: Long)

    /**
     * This method is invoked when the resource consumer finishes processing.
     */
    protected abstract fun doFinish(cause: Throwable?)

    /**
     * This method is invoked when an input context is started.
     */
    protected abstract fun onInputStarted(input: Input)

    /**
     * This method is invoked when an input is stopped.
     */
    protected abstract fun onInputFinished(input: Input)

    override fun addInput(input: SimResourceProvider) {
        check(output.state != SimResourceState.Stopped) { "Aggregator has been stopped" }

        val consumer = Consumer()
        _inputs.add(input)
        _inputConsumers.add(consumer)
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
    private val _inputConsumers = mutableListOf<Consumer>()

    protected val outputContext: SimResourceContext
        get() = context
    private val context = object : SimAbstractResourceContext(0.0, scheduler, _output) {
        override val remainingWork: Double
            get() {
                val now = clock.millis()

                return if (_remainingWorkFlush < now) {
                    _remainingWorkFlush = now
                    _inputConsumers.sumByDouble { it._ctx?.remainingWork ?: 0.0 }.also { _remainingWork = it }
                } else {
                    _remainingWork
                }
            }
        private var _remainingWork: Double = 0.0
        private var _remainingWorkFlush: Long = Long.MIN_VALUE

        override fun onConsume(work: Double, limit: Double, deadline: Long) = doConsume(work, limit, deadline)

        override fun onIdle(deadline: Long) = doIdle(deadline)

        override fun onFinish() {
            doFinish(null)
        }
    }

    /**
     * An input for the resource aggregator.
     */
    public interface Input {
        /**
         * The [SimResourceContext] associated with the input.
         */
        public val ctx: SimResourceContext

        /**
         * Push the specified [SimResourceCommand] to the input.
         */
        public fun push(command: SimResourceCommand)
    }

    /**
     * An internal [SimResourceConsumer] implementation for aggregator inputs.
     */
    private inner class Consumer : Input, SimResourceConsumer {
        /**
         * The resource context associated with the input.
         */
        override val ctx: SimResourceContext
            get() = _ctx!!
        var _ctx: SimResourceContext? = null

        /**
         * The resource command to run next.
         */
        private var command: SimResourceCommand? = null

        private fun updateCapacity() {
            // Adjust capacity of output resource
            context.capacity = _inputConsumers.sumByDouble { it._ctx?.capacity ?: 0.0 }
        }

        /* Input */
        override fun push(command: SimResourceCommand) {
            this.command = command
            _ctx?.interrupt()
        }

        /* SimResourceConsumer */
        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            var next = command

            return if (next != null) {
                this.command = null
                next
            } else {
                context.flush(isIntermediate = true)
                next = command
                this.command = null
                next ?: SimResourceCommand.Idle()
            }
        }

        override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
            when (event) {
                SimResourceEvent.Start -> {
                    _ctx = ctx
                    updateCapacity()

                    // Make sure we initialize the output if we have not done so yet
                    if (context.state == SimResourceState.Pending) {
                        context.start()
                    }

                    onInputStarted(this)
                }
                SimResourceEvent.Capacity -> updateCapacity()
                SimResourceEvent.Exit -> onInputFinished(this)
                else -> {}
            }
        }
    }
}
