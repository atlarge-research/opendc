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

import org.opendc.simulator.resources.impl.SimResourceCountersImpl
import org.opendc.simulator.resources.interference.InterferenceDomain
import org.opendc.simulator.resources.interference.InterferenceKey
import kotlin.math.min

/**
 * A [SimResourceDistributor] that distributes the capacity of a resource over consumers using max-min fair sharing.
 *
 * @param interpreter The interpreter for managing the resource contexts.
 * @param parent The parent resource system of the distributor.
 * @param interferenceDomain The interference domain of the distributor.
 */
public class SimResourceDistributorMaxMin(
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem? = null,
    private val interferenceDomain: InterferenceDomain? = null
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
     * The total amount of work allocated to be executed.
     */
    private var totalAllocatedWork = 0.0

    /**
     * The total allocated speed for the output resources.
     */
    private var totalAllocatedSpeed = 0.0

    /**
     * The total requested speed for the output resources.
     */
    private var totalRequestedSpeed = 0.0

    /**
     * The resource counters of this distributor.
     */
    public val counters: SimResourceCounters
        get() = _counters
    private val _counters = SimResourceCountersImpl()

    /* SimResourceDistributor */
    override fun newOutput(key: InterferenceKey?): SimResourceCloseableProvider {
        val provider = Output(ctx?.capacity ?: 0.0, key)
        _outputs.add(provider)
        return provider
    }

    /* SimResourceConsumer */
    override fun onNext(ctx: SimResourceContext): SimResourceCommand {
        return doNext(ctx)
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
     * Update the counters of the distributor.
     */
    private fun updateCounters(ctx: SimResourceControllableContext, work: Double, willOvercommit: Boolean) {
        if (work <= 0.0) {
            return
        }

        val counters = _counters
        val remainingWork = ctx.remainingWork

        counters.demand += work
        counters.actual += work - remainingWork

        if (willOvercommit && remainingWork > 0.0) {
            counters.overcommit += remainingWork
        }
    }

    /**
     * Schedule the work of the outputs.
     */
    private fun doNext(ctx: SimResourceContext): SimResourceCommand {
        // If there is no work yet, mark the input as idle.
        if (activeOutputs.isEmpty()) {
            return SimResourceCommand.Idle()
        }

        val capacity = ctx.capacity
        var duration: Double = Double.MAX_VALUE
        var deadline: Long = Long.MAX_VALUE
        var availableSpeed = capacity
        var totalRequestedSpeed = 0.0

        // Pull in the work of the outputs
        val outputIterator = activeOutputs.listIterator()
        for (output in outputIterator) {
            output.pull()

            // Remove outputs that have finished
            if (!output.isActive) {
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
            val grantedSpeed = min(output.allowedSpeed, availableShare)

            deadline = min(deadline, output.deadline)

            // Ignore idle computation
            if (grantedSpeed <= 0.0 || output.work <= 0.0) {
                output.actualSpeed = 0.0
                continue
            }

            totalRequestedSpeed += output.limit

            output.actualSpeed = grantedSpeed
            availableSpeed -= grantedSpeed

            // The duration that we want to run is that of the shortest request of an output
            duration = min(duration, output.work / grantedSpeed)
        }

        val targetDuration = min(duration, (deadline - interpreter.clock.millis()) / 1000.0)
        var totalRequestedWork = 0.0
        var totalAllocatedWork = 0.0
        for (output in activeOutputs) {
            val work = output.work
            val speed = output.actualSpeed
            if (speed > 0.0) {
                val outputDuration = work / speed
                totalRequestedWork += work * (duration / outputDuration)
                totalAllocatedWork += work * (targetDuration / outputDuration)
            }
        }

        assert(deadline >= interpreter.clock.millis()) { "Deadline already passed" }

        this.totalRequestedSpeed = totalRequestedSpeed
        this.totalAllocatedWork = totalAllocatedWork
        val totalAllocatedSpeed = capacity - availableSpeed
        this.totalAllocatedSpeed = totalAllocatedSpeed

        return if (totalAllocatedWork > 0.0 && totalAllocatedSpeed > 0.0)
            SimResourceCommand.Consume(totalAllocatedWork, totalAllocatedSpeed, deadline)
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
    private inner class Output(capacity: Double, private val key: InterferenceKey?) :
        SimAbstractResourceProvider(interpreter, parent, capacity),
        SimResourceCloseableProvider,
        SimResourceProviderLogic,
        Comparable<Output> {
        /**
         * A flag to indicate that the output is closed.
         */
        private var isClosed: Boolean = false

        /**
         * The current requested work.
         */
        var work: Double = 0.0

        /**
         * The requested limit.
         */
        var limit: Double = 0.0

        /**
         * The current deadline.
         */
        var deadline: Long = Long.MAX_VALUE

        /**
         * The processing speed that is allowed by the model constraints.
         */
        var allowedSpeed: Double = 0.0

        /**
         * The actual processing speed.
         */
        var actualSpeed: Double = 0.0

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
            this.deadline = deadline
            work = 0.0
            limit = 0.0
            lastCommandTimestamp = ctx.clock.millis()

            return Long.MAX_VALUE
        }

        override fun onConsume(ctx: SimResourceControllableContext, work: Double, limit: Double, deadline: Long): Long {
            allowedSpeed = min(ctx.capacity, limit)
            this.work = work
            this.limit = limit
            this.deadline = deadline
            lastCommandTimestamp = ctx.clock.millis()

            return Long.MAX_VALUE
        }

        override fun onUpdate(ctx: SimResourceControllableContext, work: Double, willOvercommit: Boolean) {
            updateCounters(ctx, work, willOvercommit)

            this@SimResourceDistributorMaxMin.updateCounters(ctx, work, willOvercommit)
        }

        override fun onFinish(ctx: SimResourceControllableContext) {
            work = 0.0
            limit = 0.0
            deadline = Long.MAX_VALUE
            lastCommandTimestamp = ctx.clock.millis()
        }

        override fun getConsumedWork(ctx: SimResourceControllableContext, work: Double, speed: Double, duration: Long): Double {
            val totalRemainingWork = this@SimResourceDistributorMaxMin.ctx?.remainingWork ?: 0.0

            // Compute the fraction of compute time allocated to the output
            val fraction = actualSpeed / totalAllocatedSpeed

            // Compute the performance penalty due to resource interference
            val perfScore = if (interferenceDomain != null) {
                val load = totalAllocatedSpeed / requireNotNull(this@SimResourceDistributorMaxMin.ctx).capacity
                interferenceDomain.apply(key, load)
            } else {
                1.0
            }

            // Compute the work that was actually granted to the output.
            return (totalAllocatedWork - totalRemainingWork) * fraction * perfScore
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
