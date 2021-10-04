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
    override val outputs: Set<FlowSource>
        get() = _outputs
    private val _outputs = mutableSetOf<Output>()
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
    public override val rate: Double
        get() = _rate
    private var _rate = 0.0

    /**
     * The demanded processing rate of the input.
     */
    public override val demand: Double
        get() = _demand
    private var _demand = 0.0

    /**
     * The capacity of the outputs.
     */
    public override val capacity: Double
        get() = _capacity
    private var _capacity = 0.0

    /**
     * Flag to indicate that the scheduler is active.
     */
    private var _schedulerActive = false
    private var _lastSchedulerCycle = Long.MAX_VALUE

    /**
     * The last convergence timestamp and the input.
     */
    private var _lastConverge: Long = Long.MIN_VALUE
    private var _lastConvergeInput: Input? = null

    /**
     * An [Output] that is used to activate the scheduler.
     */
    private var _activationOutput: Output? = null

    override fun newInput(key: InterferenceKey?): FlowConsumer {
        val provider = Input(_capacity, key)
        _inputs.add(provider)
        return provider
    }

    override fun removeInput(input: FlowConsumer) {
        if (!_inputs.remove(input)) {
            return
        }
        // This cast should always succeed since only `Input` instances should be added to `_inputs`
        (input as Input).close()
    }

    override fun newOutput(): FlowSource {
        val output = Output()
        _outputs.add(output)
        return output
    }

    override fun removeOutput(output: FlowSource) {
        if (!_outputs.remove(output)) {
            return
        }

        // This cast should always succeed since only `Output` instances should be added to `_outputs`
        (output as Output).cancel()
    }

    override fun clearInputs() {
        for (input in _inputs) {
            input.cancel()
        }
        _inputs.clear()
    }

    override fun clearOutputs() {
        for (output in _outputs) {
            output.cancel()
        }
        _outputs.clear()
    }

    override fun clear() {
        clearOutputs()
        clearInputs()
    }

    /**
     * Trigger the scheduler of the multiplexer.
     *
     * @param now The current virtual timestamp of the simulation.
     */
    private fun triggerScheduler(now: Long) {
        if (_schedulerActive) {
            // No need to trigger the scheduler in case it is already active
            return
        }

        val activationOutput = _activationOutput

        // We can run the scheduler in two ways:
        // (1) We can pull one of the multiplexer's outputs. This allows us to cascade multiple pushes by the input
        //     into a single scheduling cycle, but is slower in case of a few changes at the same timestamp.
        // (2) We run the scheduler directly from this method call. This is the fastest approach when there are only
        //     a few inputs and little changes at the same timestamp.
        // We always pick for option (1) unless there are no outputs available.
        if (activationOutput != null) {
            activationOutput.pull()
            return
        } else {
            runScheduler(now)
        }
    }

    /**
     * Synchronously run the scheduler of the multiplexer.
     */
    private fun runScheduler(now: Long): Long {
        val lastSchedulerCycle = _lastSchedulerCycle
        _lastSchedulerCycle = now

        val delta = max(0, now - lastSchedulerCycle)

        return try {
            _schedulerActive = true
            doSchedule(delta)
        } finally {
            _schedulerActive = false
        }
    }

    /**
     * Schedule the inputs over the outputs.
     *
     * @return The deadline after which a new scheduling cycle should start.
     */
    private fun doSchedule(delta: Long): Long {
        val activeInputs = _activeInputs
        val activeOutputs = _activeOutputs

        // Update the counters of the scheduler
        updateCounters(delta)

        // If there is no work yet, mark the inputs as idle.
        if (activeInputs.isEmpty()) {
            _demand = 0.0
            _rate = 0.0
            return Long.MAX_VALUE
        }

        val capacity = _capacity
        var availableCapacity = capacity

        // Pull in the work of the outputs
        val inputIterator = activeInputs.listIterator()
        for (input in inputIterator) {
            input.pullSync()

            // Remove outputs that have finished
            if (!input.isActive) {
                input.actualRate = 0.0
                inputIterator.remove()
            }
        }

        var demand = 0.0
        var deadline = Long.MAX_VALUE

        // Sort in-place the inputs based on their pushed flow.
        // Profiling shows that it is faster than maintaining some kind of sorted set.
        activeInputs.sort()

        // Divide the available output capacity fairly over the inputs using max-min fair sharing
        var remaining = activeInputs.size
        for (i in activeInputs.indices) {
            val input = activeInputs[i]
            val availableShare = availableCapacity / remaining--
            val grantedRate = min(input.allowedRate, availableShare)

            demand += input.limit
            deadline = min(deadline, input.deadline)
            availableCapacity -= grantedRate

            input.actualRate = grantedRate
        }

        val rate = capacity - availableCapacity

        _demand = demand
        _rate = rate

        // Sort all consumers by their capacity
        activeOutputs.sort()

        // Divide the requests over the available capacity of the input resources fairly
        for (i in activeOutputs.indices) {
            val output = activeOutputs[i]
            val inputCapacity = output.capacity
            val fraction = inputCapacity / capacity
            val grantedSpeed = rate * fraction

            output.push(grantedSpeed)
        }

        return deadline
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
     * The previous capacity of the multiplexer.
     */
    private var _previousCapacity = 0.0

    /**
     * Update the counters of the scheduler.
     */
    private fun updateCounters(delta: Long) {
        val previousCapacity = _previousCapacity
        _previousCapacity = _capacity

        if (delta <= 0) {
            return
        }

        val deltaS = delta / 1000.0

        _counters.demand += _demand * deltaS
        _counters.actual += _rate * deltaS
        _counters.remaining += (previousCapacity - _rate) * deltaS
    }

    /**
     * Updates the output that is used for scheduler activation.
     */
    private fun updateActivationOutput() {
        val output = _activeOutputs.firstOrNull()
        _activationOutput = output

        for (input in _activeInputs) {
            input.enableTimers = output == null
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
         * The deadline of the input.
         */
        val deadline: Long
            get() = ctx?.deadline ?: Long.MAX_VALUE

        /**
         * A flag to enable timers for the input.
         */
        var enableTimers: Boolean = true
            set(value) {
                field = value
                ctx?.enableTimers = value
            }

        /**
         * A flag to indicate that the input is closed.
         */
        private var _isClosed: Boolean = false

        /**
         * The interference domain this input belongs to.
         */
        private val interferenceDomain = this@MaxMinFlowMultiplexer.interferenceDomain

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
            enableTimers = _activationOutput == null // Disable timers of the source if one of the output manages it

            super.start(ctx)

            triggerScheduler(engine.clock.millis())
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
            limit = rate

            triggerScheduler(now)
        }

        override fun onConverge(ctx: FlowConsumerContext, now: Long, delta: Long) {
            val lastConverge = _lastConverge
            val parent = parent

            if (parent != null && (lastConverge < now || _lastConvergeInput == null)) {
                _lastConverge = now
                _lastConvergeInput = this

                parent.onConverge(now, max(0, now - lastConverge))
            }
        }

        override fun onFinish(ctx: FlowConsumerContext, now: Long, delta: Long, cause: Throwable?) {
            doUpdateCounters(delta)

            limit = 0.0
            actualRate = 0.0

            // Assign a new input responsible for handling the convergence events
            if (_lastConvergeInput == this) {
                _lastConvergeInput = null
            }

            // Re-run scheduler to distribute new load
            triggerScheduler(now)

            // BUG: Cancel the connection so that `ctx` is set to `null`
            cancel()
        }

        /* Comparable */
        override fun compareTo(other: Input): Int = allowedRate.compareTo(other.allowedRate)

        /**
         * Pull the source if necessary.
         */
        fun pullSync() {
            ctx?.pullSync()
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
                val load = _rate / _capacity
                interferenceDomain.apply(key, load)
            } else {
                1.0
            }

            val deltaS = delta / 1000.0
            val demand = limit * deltaS
            val actual = actualRate * deltaS
            val remaining = (capacity - actualRate) * deltaS

            updateCounters(demand, actual, remaining)

            _counters.interference += actual * max(0.0, 1 - perfScore)
        }
    }

    /**
     * An internal [FlowSource] implementation for multiplexer outputs.
     */
    private inner class Output : FlowSource, Comparable<Output> {
        /**
         * The active [FlowConnection] of this source.
         */
        private var _conn: FlowConnection? = null

        /**
         * The capacity of this output.
         */
        @JvmField var capacity: Double = 0.0

        /**
         * Push the specified rate to the consumer.
         */
        fun push(rate: Double) {
            _conn?.push(rate)
        }

        /**
         * Cancel this output.
         */
        fun cancel() {
            _conn?.close()
        }

        /**
         * Pull this output.
         */
        fun pull() {
            _conn?.pull()
        }

        override fun onStart(conn: FlowConnection, now: Long) {
            assert(_conn == null) { "Source running concurrently" }
            _conn = conn
            capacity = conn.capacity
            _activeOutputs.add(this)

            updateCapacity()
            updateActivationOutput()
        }

        override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
            _conn = null
            capacity = 0.0
            _activeOutputs.remove(this)

            updateCapacity()
            updateActivationOutput()

            triggerScheduler(now)
        }

        override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
            val capacity = capacity
            if (capacity != conn.capacity) {
                this.capacity = capacity
                updateCapacity()
            }

            return if (_activationOutput == this) {
                // If this output is the activation output, synchronously run the scheduler and return the new deadline
                val deadline = runScheduler(now)
                if (deadline == Long.MAX_VALUE)
                    deadline
                else
                    deadline - now
            } else {
                // Output is not the activation output, so trigger activation output and do not install timer for this
                // output (by returning `Long.MAX_VALUE`)
                triggerScheduler(now)
                Long.MAX_VALUE
            }
        }

        override fun compareTo(other: Output): Int = capacity.compareTo(other.capacity)
    }
}
