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

import kotlin.math.max
import kotlin.math.min

/**
 * A [SimResourceDistributor] that distributes the capacity of a resource over consumers using max-min fair sharing.
 */
public class SimResourceDistributorMaxMin(
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem? = null
) : SimResourceDistributor {
    override val outputs: Set<SimResourceCloseableProvider>
        get() = _outputs
    private val _outputs = mutableSetOf<Output>()

    /**
     * The resource context of the consumer.
     */
    private var ctx: SimResourceContext? = null

    /**
     * The active outputs.
     */
    private val activeOutputs: MutableList<Output> = mutableListOf()

    /**
     * The total amount of work requested by the output resources.
     */
    private var totalRequestedWork = 0.0

    /**
     * The total allocated speed for the output resources.
     */
    private var totalAllocatedSpeed = 0.0

    /* SimResourceDistributor */
    override fun newOutput(): SimResourceCloseableProvider {
        val provider = Output(ctx?.capacity ?: 0.0)
        _outputs.add(provider)
        return provider
    }

    /* SimResourceConsumer */
    override fun onNext(ctx: SimResourceContext): SimResourceCommand {
        return doNext(ctx.capacity)
    }

    override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
        when (event) {
            SimResourceEvent.Start -> {
                this.ctx = ctx
                updateCapacity(ctx)
            }
            SimResourceEvent.Exit -> {
                val iterator = _outputs.iterator()
                while (iterator.hasNext()) {
                    val output = iterator.next()

                    // Remove the output from the outputs to prevent ConcurrentModificationException when removing it
                    // during the call to output.close()
                    iterator.remove()

                    output.close()
                }
            }
            SimResourceEvent.Capacity -> updateCapacity(ctx)
            else -> {}
        }
    }

    /**
     * Schedule the work of the outputs.
     */
    private fun doNext(capacity: Double): SimResourceCommand {
        // If there is no work yet, mark the input as idle.
        if (activeOutputs.isEmpty()) {
            return SimResourceCommand.Idle()
        }

        var duration: Double = Double.MAX_VALUE
        var deadline: Long = Long.MAX_VALUE
        var availableSpeed = capacity
        var totalRequestedSpeed = 0.0
        var totalRequestedWork = 0.0

        // Pull in the work of the outputs
        val outputIterator = activeOutputs.listIterator()
        for (output in outputIterator) {
            output.pull()

            // Remove outputs that have finished
            if (output.isFinished) {
                outputIterator.remove()
            }
        }

        // Sort in-place the outputs based on their requested usage.
        // Profiling shows that it is faster than maintaining some kind of sorted set.
        activeOutputs.sort()

        // Divide the available input capacity fairly across the outputs using max-min fair sharing
        var remaining = activeOutputs.size
        for (output in activeOutputs) {
            val availableShare = availableSpeed / remaining--

            when (val command = output.activeCommand) {
                is SimResourceCommand.Idle -> {
                    deadline = min(deadline, command.deadline)
                    output.actualSpeed = 0.0
                }
                is SimResourceCommand.Consume -> {
                    val grantedSpeed = min(output.allowedSpeed, availableShare)
                    deadline = min(deadline, command.deadline)

                    // Ignore idle computation
                    if (grantedSpeed <= 0.0 || command.work <= 0.0) {
                        output.actualSpeed = 0.0
                        continue
                    }

                    totalRequestedSpeed += command.limit
                    totalRequestedWork += command.work

                    output.actualSpeed = grantedSpeed
                    availableSpeed -= grantedSpeed

                    // The duration that we want to run is that of the shortest request of an output
                    duration = min(duration, command.work / grantedSpeed)
                }
                SimResourceCommand.Exit -> assert(false) { "Did not expect output to be stopped" }
            }
        }

        assert(deadline >= interpreter.clock.millis()) { "Deadline already passed" }

        this.totalRequestedWork = totalRequestedWork
        this.totalAllocatedSpeed = capacity - availableSpeed
        val totalAllocatedWork = min(
            totalRequestedWork,
            totalAllocatedSpeed * min((deadline - interpreter.clock.millis()) / 1000.0, duration)
        )

        return if (totalAllocatedWork > 0.0 && totalAllocatedSpeed > 0.0)
            SimResourceCommand.Consume(totalRequestedWork, totalAllocatedSpeed, deadline)
        else
            SimResourceCommand.Idle(deadline)
    }

    private fun updateCapacity(ctx: SimResourceContext) {
        for (output in _outputs) {
            output.capacity = ctx.capacity
        }
    }

    /**
     * An internal [SimResourceProvider] implementation for switch outputs.
     */
    private inner class Output(capacity: Double) :
        SimAbstractResourceProvider(interpreter, parent, capacity),
        SimResourceCloseableProvider,
        SimResourceProviderLogic,
        Comparable<Output> {
        /**
         * A flag to indicate that the output is closed.
         */
        private var isClosed: Boolean = false

        /**
         * The current command that is processed by the resource.
         */
        var activeCommand: SimResourceCommand = SimResourceCommand.Idle()

        /**
         * The processing speed that is allowed by the model constraints.
         */
        var allowedSpeed: Double = 0.0

        /**
         * The actual processing speed.
         */
        var actualSpeed: Double = 0.0

        /**
         * A flag to indicate that the output is finished.
         */
        val isFinished
            get() = activeCommand is SimResourceCommand.Exit

        /**
         * The timestamp at which we received the last command.
         */
        private var lastCommandTimestamp: Long = Long.MIN_VALUE

        /* SimAbstractResourceProvider */
        override fun createLogic(): SimResourceProviderLogic = this

        override fun start(ctx: SimResourceControllableContext) {
            check(!isClosed) { "Cannot re-use closed output" }

            activeOutputs += this

            interpreter.batch {
                ctx.start()
                // Interrupt the input to re-schedule the resources
                this@SimResourceDistributorMaxMin.ctx?.interrupt()
            }
        }

        override fun close() {
            isClosed = true
            cancel()
            _outputs.remove(this)
        }

        /* SimResourceProviderLogic */
        override fun onIdle(ctx: SimResourceControllableContext, deadline: Long): Long {
            allowedSpeed = 0.0
            activeCommand = SimResourceCommand.Idle(deadline)
            lastCommandTimestamp = ctx.clock.millis()

            return Long.MAX_VALUE
        }

        override fun onConsume(ctx: SimResourceControllableContext, work: Double, limit: Double, deadline: Long): Long {
            allowedSpeed = ctx.speed
            activeCommand = SimResourceCommand.Consume(work, limit, deadline)
            lastCommandTimestamp = ctx.clock.millis()

            return Long.MAX_VALUE
        }

        override fun onUpdate(ctx: SimResourceControllableContext, work: Double) {
            updateCounters(ctx, work)
        }

        override fun onFinish(ctx: SimResourceControllableContext) {
            activeCommand = SimResourceCommand.Exit
            lastCommandTimestamp = ctx.clock.millis()
        }

        override fun getRemainingWork(ctx: SimResourceControllableContext, work: Double, speed: Double, duration: Long): Double {
            val totalRemainingWork = this@SimResourceDistributorMaxMin.ctx?.remainingWork ?: 0.0

            return if (work > 0.0) {
                // Compute the fraction of compute time allocated to the output
                val fraction = actualSpeed / totalAllocatedSpeed

                // Compute the work that was actually granted to the output.
                val processingAvailable = max(0.0, totalRequestedWork - totalRemainingWork) * fraction
                max(0.0, work - processingAvailable)
            } else {
                0.0
            }
        }

        /* Comparable */
        override fun compareTo(other: Output): Int = allowedSpeed.compareTo(other.allowedSpeed)

        /**
         * Pull the next command if necessary.
         */
        fun pull() {
            val ctx = ctx
            if (ctx != null && lastCommandTimestamp < ctx.clock.millis()) {
                ctx.flush()
            }
        }
    }
}
