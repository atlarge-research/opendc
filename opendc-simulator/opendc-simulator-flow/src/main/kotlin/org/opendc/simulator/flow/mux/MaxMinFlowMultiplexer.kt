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
    parent: FlowConvergenceListener? = null,
    private val interferenceDomain: InterferenceDomain? = null
) : FlowMultiplexer {
    /**
     * The inputs of the multiplexer.
     */
    override val inputs: Set<FlowConsumer>
        get() = _inputs
    private val _inputs = mutableSetOf<Input>()

    /**
     * The outputs of the multiplexer.
     */
    override val outputs: Set<FlowSource>
        get() = _outputs
    private val _outputs = mutableSetOf<Output>()

    /**
     * The flow counters of this multiplexer.
     */
    public override val counters: FlowCounters
        get() = scheduler.counters

    /**
     * The actual processing rate of the multiplexer.
     */
    public override val rate: Double
        get() = scheduler.rate

    /**
     * The demanded processing rate of the input.
     */
    public override val demand: Double
        get() = scheduler.demand

    /**
     * The capacity of the outputs.
     */
    public override val capacity: Double
        get() = scheduler.capacity

    /**
     * The [Scheduler] instance of this multiplexer.
     */
    private val scheduler = Scheduler(engine, parent)

    override fun newInput(key: InterferenceKey?): FlowConsumer {
        val provider = Input(engine, scheduler, interferenceDomain, key)
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
        val output = Output(scheduler)
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
     * Helper class containing the scheduler state.
     */
    private class Scheduler(private val engine: FlowEngine, private val parent: FlowConvergenceListener?) {
        /**
         * The flow counters of this scheduler.
         */
        @JvmField val counters = FlowCountersImpl()

        /**
         * The flow rate of the multiplexer.
         */
        @JvmField var rate = 0.0

        /**
         * The demand for the multiplexer.
         */
        @JvmField var demand = 0.0

        /**
         * The capacity of the multiplexer.
         */
        @JvmField var capacity = 0.0

        /**
         * An [Output] that is used to activate the scheduler.
         */
        @JvmField var activationOutput: Output? = null

        /**
         * The active inputs registered with the scheduler.
         */
        private val _activeInputs = mutableListOf<Input>()

        /**
         * The active outputs registered with the scheduler.
         */
        private val _activeOutputs = mutableListOf<Output>()

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
         * Register the specified [input] to this scheduler.
         */
        fun registerInput(input: Input) {
            _activeInputs.add(input)

            val hasActivationOutput = activationOutput != null

            // Disable timers and convergence of the source if one of the output manages it
            input.shouldConsumerConverge = !hasActivationOutput
            input.enableTimers = !hasActivationOutput
            input.capacity = capacity
            trigger(engine.clock.millis())
        }

        /**
         * De-register the specified [input] from this scheduler.
         */
        fun deregisterInput(input: Input, now: Long) {
            // Assign a new input responsible for handling the convergence events
            if (_lastConvergeInput == input) {
                _lastConvergeInput = null
            }

            // Re-run scheduler to distribute new load
            trigger(now)
        }

        /**
         * This method is invoked when one of the inputs converges.
         */
        fun convergeInput(input: Input, now: Long) {

            val lastConverge = _lastConverge
            val lastConvergeInput = _lastConvergeInput
            val parent = parent

            if (parent != null && (now > lastConverge || lastConvergeInput == null || lastConvergeInput == input)) {
                _lastConverge = now
                _lastConvergeInput = input

                parent.onConverge(now, max(0, now - lastConverge))
            }
        }

        /**
         * Register the specified [output] to this scheduler.
         */
        fun registerOutput(output: Output) {
            _activeOutputs.add(output)

            updateCapacity()
            updateActivationOutput()
        }

        /**
         * De-register the specified [output] from this scheduler.
         */
        fun deregisterOutput(output: Output, now: Long) {
            _activeOutputs.remove(output)
            updateCapacity()

            trigger(now)
        }

        /**
         * This method is invoked when one of the outputs converges.
         */
        fun convergeOutput(output: Output, now: Long) {
            val lastConverge = _lastConverge
            val parent = parent

            if (parent != null) {
                _lastConverge = now

                parent.onConverge(now, max(0, now - lastConverge))
            }

            if (!output.isActive) {
                output.isActivationOutput = false
                updateActivationOutput()
            }
        }

        /**
         * Trigger the scheduler of the multiplexer.
         *
         * @param now The current virtual timestamp of the simulation.
         */
        fun trigger(now: Long) {
            if (_schedulerActive) {
                // No need to trigger the scheduler in case it is already active
                return
            }

            val activationOutput = activationOutput

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
        fun runScheduler(now: Long): Long {
            val lastSchedulerCycle = _lastSchedulerCycle
            _lastSchedulerCycle = now

            val delta = max(0, now - lastSchedulerCycle)

            return try {
                _schedulerActive = true
                doRunScheduler(delta)
            } finally {
                _schedulerActive = false
            }
        }

        /**
         * Recompute the capacity of the multiplexer.
         */
        fun updateCapacity() {
            val newCapacity = _activeOutputs.sumOf(Output::capacity)

            // No-op if the capacity is unchanged
            if (capacity == newCapacity) {
                return
            }

            capacity = newCapacity

            for (input in _activeInputs) {
                input.capacity = newCapacity
            }

            // Sort outputs by their capacity
            _activeOutputs.sort()
        }

        /**
         * Updates the output that is used for scheduler activation.
         */
        private fun updateActivationOutput() {
            val output = _activeOutputs.firstOrNull()
            activationOutput = output

            if (output != null) {
                output.isActivationOutput = true
            }

            val hasActivationOutput = output != null

            for (input in _activeInputs) {
                input.shouldConsumerConverge = !hasActivationOutput
                input.enableTimers = !hasActivationOutput
            }
        }

        /**
         * Schedule the inputs over the outputs.
         *
         * @return The deadline after which a new scheduling cycle should start.
         */
        private fun doRunScheduler(delta: Long): Long {
            val activeInputs = _activeInputs
            val activeOutputs = _activeOutputs

            // Update the counters of the scheduler
            updateCounters(delta)

            // If there is no work yet, mark the inputs as idle.
            if (activeInputs.isEmpty()) {
                demand = 0.0
                rate = 0.0
                return Long.MAX_VALUE
            }

            val capacity = capacity
            var availableCapacity = capacity
            var deadline = Long.MAX_VALUE
            var demand = 0.0

            // Pull in the work of the outputs
            val inputIterator = activeInputs.listIterator()
            for (input in inputIterator) {
                input.pullSync()

                // Remove outputs that have finished
                if (!input.isActive) {
                    input.actualRate = 0.0
                    inputIterator.remove()
                } else {
                    demand += input.limit
                    deadline = min(deadline, input.deadline)
                }
            }

            val rate = if (demand > capacity) {
                // If the demand is higher than the capacity, we need use max-min fair sharing to distribute the
                // constrained capacity across the inputs.

                // Sort in-place the inputs based on their pushed flow.
                // Profiling shows that it is faster than maintaining some kind of sorted set.
                activeInputs.sort()

                // Divide the available output capacity fairly over the inputs using max-min fair sharing
                val size = activeInputs.size
                for (i in activeInputs.indices) {
                    val input = activeInputs[i]
                    val availableShare = availableCapacity / (size - i)
                    val grantedRate = min(input.allowedRate, availableShare)

                    availableCapacity -= grantedRate
                    input.actualRate = grantedRate
                }

                capacity - availableCapacity
            } else {
                demand
            }

            this.demand = demand
            this.rate = rate

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
         * The previous capacity of the multiplexer.
         */
        private var _previousCapacity = 0.0

        /**
         * Update the counters of the scheduler.
         */
        private fun updateCounters(delta: Long) {
            val previousCapacity = _previousCapacity
            _previousCapacity = capacity

            if (delta <= 0) {
                return
            }

            val deltaS = delta / 1000.0

            counters.demand += demand * deltaS
            counters.actual += rate * deltaS
            counters.remaining += (previousCapacity - rate) * deltaS
        }
    }

    /**
     * An internal [FlowConsumer] implementation for multiplexer inputs.
     */
    private class Input(
        engine: FlowEngine,
        private val scheduler: Scheduler,
        private val interferenceDomain: InterferenceDomain?,
        @JvmField val key: InterferenceKey?
    ) : AbstractFlowConsumer(engine, scheduler.capacity), FlowConsumerLogic, Comparable<Input> {
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
        @JvmField var allowedRate: Double = 0.0

        /**
         * The deadline of the input.
         */
        val deadline: Long
            get() = ctx?.deadline ?: Long.MAX_VALUE

        /**
         * The capacity of the input.
         */
        override var capacity: Double
            get() = super.capacity
            set(value) {
                allowedRate = min(limit, value)
                super.capacity = value
            }

        /**
         * A flag to enable timers for the input.
         */
        var enableTimers: Boolean = true
            set(value) {
                field = value
                ctx?.enableTimers = value
            }

        /**
         * A flag to control whether the input should converge.
         */
        var shouldConsumerConverge: Boolean = true
            set(value) {
                field = value
                ctx?.shouldConsumerConverge = value
            }

        /**
         * A flag to indicate that the input is closed.
         */
        private var _isClosed: Boolean = false

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
            scheduler.registerInput(this)
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

            val allowed = min(rate, capacity)
            limit = rate
            actualRate = allowed
            allowedRate = allowed

            scheduler.trigger(now)
        }

        override fun onFinish(ctx: FlowConsumerContext, now: Long, delta: Long, cause: Throwable?) {
            doUpdateCounters(delta)

            limit = 0.0
            actualRate = 0.0
            allowedRate = 0.0

            scheduler.deregisterInput(this, now)

            // BUG: Cancel the connection so that `ctx` is set to `null`
            cancel()
        }

        override fun onConverge(ctx: FlowConsumerContext, now: Long, delta: Long) {
            scheduler.convergeInput(this, now)
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
                val load = scheduler.rate / scheduler.capacity
                interferenceDomain.apply(key, load)
            } else {
                1.0
            }

            val deltaS = delta / 1000.0
            val demand = limit * deltaS
            val actual = actualRate * deltaS
            val remaining = (capacity - actualRate) * deltaS

            updateCounters(demand, actual, remaining)

            scheduler.counters.interference += actual * max(0.0, 1 - perfScore)
        }
    }

    /**
     * An internal [FlowSource] implementation for multiplexer outputs.
     */
    private class Output(private val scheduler: Scheduler) : FlowSource, Comparable<Output> {
        /**
         * The active [FlowConnection] of this source.
         */
        private var _conn: FlowConnection? = null

        /**
         * The capacity of this output.
         */
        @JvmField var capacity: Double = 0.0

        /**
         * A flag to indicate that this output is the activation output.
         */
        var isActivationOutput: Boolean
            get() = _isActivationOutput
            set(value) {
                _isActivationOutput = value
                _conn?.shouldSourceConverge = value
            }
        private var _isActivationOutput: Boolean = false

        /**
         * A flag to indicate that the output is active.
         */
        @JvmField var isActive = false

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
            isActive = true

            scheduler.registerOutput(this)
        }

        override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
            _conn = null
            capacity = 0.0
            isActive = false

            scheduler.deregisterOutput(this, now)
        }

        override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
            val capacity = capacity
            if (capacity != conn.capacity) {
                this.capacity = capacity
                scheduler.updateCapacity()
            }

            return if (_isActivationOutput) {
                // If this output is the activation output, synchronously run the scheduler and return the new deadline
                val deadline = scheduler.runScheduler(now)
                if (deadline == Long.MAX_VALUE)
                    deadline
                else
                    deadline - now
            } else {
                // Output is not the activation output, so trigger activation output and do not install timer for this
                // output (by returning `Long.MAX_VALUE`)
                scheduler.trigger(now)
                Long.MAX_VALUE
            }
        }

        override fun onConverge(conn: FlowConnection, now: Long, delta: Long) {
            if (_isActivationOutput) {
                scheduler.convergeOutput(this, now)
            }
        }

        override fun compareTo(other: Output): Int = capacity.compareTo(other.capacity)
    }
}
