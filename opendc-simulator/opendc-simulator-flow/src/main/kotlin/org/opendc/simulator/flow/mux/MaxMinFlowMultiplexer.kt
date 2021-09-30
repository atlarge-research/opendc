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

package org.opendc.simulator.flow.mux

import org.opendc.simulator.flow.*
import org.opendc.simulator.flow.interference.InterferenceDomain
import org.opendc.simulator.flow.interference.InterferenceKey
import org.opendc.simulator.flow.internal.FlowCountersImpl
import kotlin.math.max
import kotlin.math.min

/**
 * A [FlowMultiplexer] implementation that multiplexes flows over the available outputs using max-min fair sharing.
 *
 * @param engine The [FlowEngine] to drive the flow simulation.
 * @param parent The parent flow system of the multiplexer.
 * @param interferenceDomain The interference domain of the multiplexer.
 */
public class MaxMinFlowMultiplexer(
    private val engine: FlowEngine,
    private val parent: FlowConvergenceListener? = null,
    private val interferenceDomain: InterferenceDomain? = null
) : FlowMultiplexer {
    /**
     * The inputs of the multiplexer.
     */
    override val inputs: Set<FlowConsumer>
        get() = _inputs
    private val _inputs = mutableSetOf<Input>()
    private val _activeInputs = mutableListOf<Input>()

    /**
     * The outputs of the multiplexer.
     */
    override val outputs: Set<FlowConsumer>
        get() = _outputs
    private val _outputs = mutableSetOf<FlowConsumer>()
    private val _activeOutputs = mutableListOf<Output>()

    /**
     * The flow counters of this multiplexer.
     */
    public override val counters: FlowCounters
        get() = _counters
    private val _counters = FlowCountersImpl()

    /**
     * The actual processing rate of the multiplexer.
     */
    private var _rate = 0.0

    /**
     * The demanded processing rate of the input.
     */
    private var _demand = 0.0

    /**
     * The capacity of the outputs.
     */
    private var _capacity = 0.0

    /**
     * Flag to indicate that the scheduler is active.
     */
    private var _schedulerActive = false

    override fun newInput(key: InterferenceKey?): FlowConsumer {
        val provider = Input(_capacity, key)
        _inputs.add(provider)
        return provider
    }

    override fun addOutput(output: FlowConsumer) {
        val consumer = Output(output)
        if (_outputs.add(output)) {
            _activeOutputs.add(consumer)
            output.startConsumer(consumer)
        }
    }

    override fun removeInput(input: FlowConsumer) {
        if (!_inputs.remove(input)) {
            return
        }
        // This cast should always succeed since only `Input` instances should be added to `_inputs`
        (input as Input).close()
    }

    override fun clear() {
        for (input in _activeOutputs) {
            input.cancel()
        }
        _activeOutputs.clear()

        for (output in _activeInputs) {
            output.cancel()
        }
        _activeInputs.clear()
    }

    /**
     * Converge the scheduler of the multiplexer.
     */
    private fun runScheduler(now: Long) {
        if (_schedulerActive) {
            return
        }

        _schedulerActive = true
        try {
            doSchedule(now)
        } finally {
            _schedulerActive = false
        }
    }

    /**
     * Schedule the inputs over the outputs.
     */
    private fun doSchedule(now: Long) {
        val activeInputs = _activeInputs
        val activeOutputs = _activeOutputs

        // If there is no work yet, mark the inputs as idle.
        if (activeInputs.isEmpty()) {
            return
        }

        val capacity = _capacity
        var availableCapacity = capacity

        // Pull in the work of the outputs
        val inputIterator = activeInputs.listIterator()
        for (input in inputIterator) {
            input.pull(now)

            // Remove outputs that have finished
            if (!input.isActive) {
                inputIterator.remove()
            }
        }

        var demand = 0.0

        // Sort in-place the inputs based on their pushed flow.
        // Profiling shows that it is faster than maintaining some kind of sorted set.
        activeInputs.sort()

        // Divide the available output capacity fairly over the inputs using max-min fair sharing
        var remaining = activeInputs.size
        for (input in activeInputs) {
            val availableShare = availableCapacity / remaining--
            val grantedRate = min(input.allowedRate, availableShare)

            // Ignore empty sources
            if (grantedRate <= 0.0) {
                input.actualRate = 0.0
                continue
            }

            input.actualRate = grantedRate
            demand += input.limit
            availableCapacity -= grantedRate
        }

        val rate = capacity - availableCapacity

        _demand = demand
        _rate = rate

        // Sort all consumers by their capacity
        activeOutputs.sort()

        // Divide the requests over the available capacity of the input resources fairly
        for (output in activeOutputs) {
            val inputCapacity = output.capacity
            val fraction = inputCapacity / capacity
            val grantedSpeed = rate * fraction

            output.push(grantedSpeed)
        }
    }

    /**
     * Recompute the capacity of the multiplexer.
     */
    private fun updateCapacity() {
        val newCapacity = _activeOutputs.sumOf(Output::capacity)

        // No-op if the capacity is unchanged
        if (_capacity == newCapacity) {
            return
        }

        _capacity = newCapacity

        for (input in _inputs) {
            input.capacity = newCapacity
        }
    }

    /**
     * An internal [FlowConsumer] implementation for multiplexer inputs.
     */
    private inner class Input(capacity: Double, val key: InterferenceKey?) :
        AbstractFlowConsumer(engine, capacity),
        FlowConsumerLogic,
        Comparable<Input> {
        /**
         * The requested limit.
         */
        @JvmField var limit: Double = 0.0

        /**
         * The actual processing speed.
         */
        @JvmField var actualRate: Double = 0.0

        /**
         * The processing rate that is allowed by the model constraints.
         */
        val allowedRate: Double
            get() = min(capacity, limit)

        /**
         * A flag to indicate that the input is closed.
         */
        private var _isClosed: Boolean = false

        /**
         * The timestamp at which we received the last command.
         */
        private var _lastPull: Long = Long.MIN_VALUE

        /**
         * Close the input.
         *
         * This method is invoked when the user removes an input from the switch.
         */
        fun close() {
            _isClosed = true
            cancel()
        }

        /* AbstractFlowConsumer */
        override fun createLogic(): FlowConsumerLogic = this

        override fun start(ctx: FlowConsumerContext) {
            check(!_isClosed) { "Cannot re-use closed input" }

            _activeInputs += this

            if (parent != null) {
                ctx.shouldConsumerConverge = true
            }

            super.start(ctx)
        }

        /* FlowConsumerLogic */
        override fun onPush(
            ctx: FlowConsumerContext,
            now: Long,
            delta: Long,
            rate: Double
        ) {
            doUpdateCounters(delta)

            actualRate = 0.0
            this.limit = rate
            _lastPull = now

            runScheduler(now)
        }

        override fun onConverge(ctx: FlowConsumerContext, now: Long, delta: Long) {
            parent?.onConverge(now, delta)
        }

        override fun onFinish(ctx: FlowConsumerContext, now: Long, delta: Long, cause: Throwable?) {
            doUpdateCounters(delta)

            limit = 0.0
            actualRate = 0.0
            _lastPull = now
        }

        /* Comparable */
        override fun compareTo(other: Input): Int = allowedRate.compareTo(other.allowedRate)

        /**
         * Pull the source if necessary.
         */
        fun pull(now: Long) {
            val ctx = ctx
            if (ctx != null && _lastPull < now) {
                ctx.flush()
            }
        }

        /**
         * Helper method to update the flow counters of the multiplexer.
         */
        private fun doUpdateCounters(delta: Long) {
            if (delta <= 0L) {
                return
            }

            // Compute the performance penalty due to flow interference
            val perfScore = if (interferenceDomain != null) {
                val load = _rate / capacity
                interferenceDomain.apply(key, load)
            } else {
                1.0
            }

            val deltaS = delta / 1000.0
            val work = limit * deltaS
            val actualWork = actualRate * deltaS
            val remainingWork = work - actualWork

            updateCounters(work, actualWork, remainingWork)

            val distCounters = _counters
            distCounters.demand += work
            distCounters.actual += actualWork
            distCounters.overcommit += remainingWork
            distCounters.interference += actualWork * max(0.0, 1 - perfScore)
        }
    }

    /**
     * An internal [FlowSource] implementation for multiplexer outputs.
     */
    private inner class Output(private val provider: FlowConsumer) : FlowSource, Comparable<Output> {
        /**
         * The active [FlowConnection] of this source.
         */
        private var _ctx: FlowConnection? = null

        /**
         * The capacity of this output.
         */
        @JvmField var capacity: Double = 0.0

        /**
         * Push the specified rate to the consumer.
         */
        fun push(rate: Double) {
            _ctx?.push(rate)
        }

        /**
         * Cancel this output.
         */
        fun cancel() {
            provider.cancel()
        }

        override fun onStart(conn: FlowConnection, now: Long) {
            assert(_ctx == null) { "Source running concurrently" }
            _ctx = conn
            capacity = conn.capacity
            updateCapacity()
        }

        override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
            _ctx = null
            capacity = 0.0
            updateCapacity()
        }

        override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
            val capacity = capacity
            if (capacity != conn.capacity) {
                this.capacity = capacity
                updateCapacity()
            }

            runScheduler(now)
            return Long.MAX_VALUE
        }

        override fun compareTo(other: Output): Int = capacity.compareTo(other.capacity)
    }
}
