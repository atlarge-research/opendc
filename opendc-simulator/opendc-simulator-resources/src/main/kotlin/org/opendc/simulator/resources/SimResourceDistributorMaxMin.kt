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

import org.opendc.simulator.resources.interference.InterferenceDomain
import org.opendc.simulator.resources.interference.InterferenceKey
import kotlin.math.max
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
    public val counters: Counters
        get() = _counters
    private val _counters = object : Counters {
        override var demand: Double = 0.0
        override var actual: Double = 0.0
        override var overcommit: Double = 0.0
        override var interference: Double = 0.0

        override fun reset() {
            demand = 0.0
            actual = 0.0
            overcommit = 0.0
            interference = 0.0
        }

        override fun toString(): String = "SimResourceDistributorMaxMin.Counters[demand=$demand,actual=$actual,overcommit=$overcommit,interference=$interference]"
    }

    /* SimResourceDistributor */
    override fun newOutput(key: InterferenceKey?): SimResourceCloseableProvider {
        val provider = Output(ctx?.capacity ?: 0.0, key)
        _outputs.add(provider)
        return provider
    }

    /* SimResourceConsumer */
    override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
        return doNext(ctx, now)
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
     * Extended [SimResourceCounters] interface for the distributor.
     */
    public interface Counters : SimResourceCounters {
        /**
         * The amount of work lost due to interference.
         */
        public val interference: Double
    }

    /**
     * Schedule the work of the outputs.
     */
    private fun doNext(ctx: SimResourceContext, now: Long): Long {
        // If there is no work yet, mark the input as idle.
        if (activeOutputs.isEmpty()) {
            return Long.MAX_VALUE
        }

        val capacity = ctx.capacity
        var duration: Long = Long.MAX_VALUE
        var availableSpeed = capacity
        var totalRequestedSpeed = 0.0

        // Pull in the work of the outputs
        val outputIterator = activeOutputs.listIterator()
        for (output in outputIterator) {
            output.pull(now)

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

            duration = min(duration, output.duration)

            // Ignore idle computation
            if (grantedSpeed <= 0.0) {
                output.actualSpeed = 0.0
                continue
            }

            totalRequestedSpeed += output.limit

            output.actualSpeed = grantedSpeed
            availableSpeed -= grantedSpeed
        }

        val durationS = duration / 1000.0
        var totalRequestedWork = 0.0
        var totalAllocatedWork = 0.0
        for (output in activeOutputs) {
            val limit = output.limit
            val speed = output.actualSpeed
            if (speed > 0.0) {
                totalRequestedWork += limit * durationS
                totalAllocatedWork += speed * durationS
            }
        }

        this.totalRequestedSpeed = totalRequestedSpeed
        val totalAllocatedSpeed = capacity - availableSpeed
        this.totalAllocatedSpeed = totalAllocatedSpeed

        ctx.push(totalAllocatedSpeed)
        return duration
    }

    private fun updateCapacity(ctx: SimResourceContext) {
        for (output in _outputs) {
            output.capacity = ctx.capacity
        }
    }

    /**
     * An internal [SimResourceProvider] implementation for switch outputs.
     */
    private inner class Output(capacity: Double, val key: InterferenceKey?) :
        SimAbstractResourceProvider(interpreter, parent, capacity),
        SimResourceCloseableProvider,
        SimResourceProviderLogic,
        Comparable<Output> {
        /**
         * A flag to indicate that the output is closed.
         */
        private var isClosed: Boolean = false

        /**
         * The requested limit.
         */
        @JvmField var limit: Double = 0.0

        /**
         * The current deadline.
         */
        @JvmField var duration: Long = Long.MAX_VALUE

        /**
         * The processing speed that is allowed by the model constraints.
         */
        @JvmField var allowedSpeed: Double = 0.0

        /**
         * The actual processing speed.
         */
        @JvmField var actualSpeed: Double = 0.0

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
        override fun onConsume(ctx: SimResourceControllableContext, now: Long, limit: Double, duration: Long): Long {
            allowedSpeed = min(ctx.capacity, limit)
            this.limit = limit
            this.duration = duration
            lastCommandTimestamp = now

            return super.onConsume(ctx, now, limit, duration)
        }

        override fun onUpdate(ctx: SimResourceControllableContext, delta: Long, limit: Double, willOvercommit: Boolean) {
            if (delta <= 0.0) {
                return
            }

            // Compute the performance penalty due to resource interference
            val perfScore = if (interferenceDomain != null) {
                val load = totalAllocatedSpeed / requireNotNull(this@SimResourceDistributorMaxMin.ctx).capacity
                interferenceDomain.apply(key, load)
            } else {
                1.0
            }

            val deltaS = delta / 1000.0
            val work = limit * deltaS
            val actualWork = actualSpeed * deltaS
            val remainingWork = work - actualWork
            val overcommit = if (willOvercommit && remainingWork > 0.0) {
                remainingWork
            } else {
                0.0
            }

            updateCounters(work, actualWork, overcommit)

            val distCounters = _counters
            distCounters.demand += work
            distCounters.actual += actualWork
            distCounters.overcommit += overcommit
            distCounters.interference += actualWork * max(0.0, 1 - perfScore)
        }

        override fun onFinish(ctx: SimResourceControllableContext) {
            limit = 0.0
            duration = Long.MAX_VALUE
            lastCommandTimestamp = ctx.clock.millis()
        }

        /* Comparable */
        override fun compareTo(other: Output): Int = allowedSpeed.compareTo(other.allowedSpeed)

        /**
         * Pull the next command if necessary.
         */
        fun pull(now: Long) {
            val ctx = ctx
            if (ctx != null && lastCommandTimestamp < now) {
                ctx.flush()
            }
        }
    }
}
