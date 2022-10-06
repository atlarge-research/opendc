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

import org.opendc.simulator.flow.FlowConnection
import org.opendc.simulator.flow.FlowConsumer
import org.opendc.simulator.flow.FlowConsumerContext
import org.opendc.simulator.flow.FlowConsumerLogic
import org.opendc.simulator.flow.FlowConvergenceListener
import org.opendc.simulator.flow.FlowCounters
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.FlowSource
import org.opendc.simulator.flow.internal.D_MS_TO_S
import org.opendc.simulator.flow.internal.MutableFlowCounters
import kotlin.math.min

/**
 * A [FlowMultiplexer] implementation that multiplexes flows over the available outputs using max-min fair sharing.
 *
 * @param engine The [FlowEngine] to drive the flow simulation.
 * @param parent The parent flow system of the multiplexer.
 */
public class MaxMinFlowMultiplexer(
    private val engine: FlowEngine,
    parent: FlowConvergenceListener? = null
) : FlowMultiplexer {

    override val maxInputs: Int = Int.MAX_VALUE

    override val maxOutputs: Int = Int.MAX_VALUE

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

    override fun newInput(): FlowConsumer {
        return newInput(isCoupled = true, Double.POSITIVE_INFINITY)
    }

    override fun newInput(capacity: Double): FlowConsumer {
        return newInput(isCoupled = false, capacity)
    }

    private fun newInput(isCoupled: Boolean, initialCapacity: Double): FlowConsumer {
        val provider = Input(engine, scheduler, isCoupled, initialCapacity)
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

    override fun flushCounters() {
        scheduler.updateCounters(engine.clock.millis())
    }

    override fun flushCounters(input: FlowConsumer) {
        (input as Input).doUpdateCounters(engine.clock.millis())
    }

    /**
     * Helper class containing the scheduler state.
     */
    private class Scheduler(engine: FlowEngine, private val parent: FlowConvergenceListener?) {
        /**
         * The flow counters of this scheduler.
         */
        @JvmField val counters = MutableFlowCounters()

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
         * An array containing the active inputs, which is used to reduce the overhead of an [ArrayList].
         */
        private var _inputArray = emptyArray<Input>()

        /**
         * The active outputs registered with the scheduler.
         */
        private val _activeOutputs = mutableListOf<Output>()

        /**
         * Flag to indicate that the scheduler is active.
         */
        private var _schedulerActive = false

        /**
         * The last convergence timestamp and the input.
         */
        private var _lastConverge: Long = Long.MIN_VALUE
        private var _lastConvergeInput: Input? = null

        /**
         * The simulation clock.
         */
        private val _clock = engine.clock

        /**
         * Register the specified [input] to this scheduler.
         */
        fun registerInput(input: Input) {
            _activeInputs.add(input)
            _inputArray = _activeInputs.toTypedArray()

            val hasActivationOutput = activationOutput != null

            // Disable timers and convergence of the source if one of the output manages it
            input.shouldConsumerConverge = !hasActivationOutput
            input.enableTimers = !hasActivationOutput

            if (input.isCoupled) {
                input.capacity = capacity
            }

            trigger(_clock.millis())
        }

        /**
         * De-register the specified [input] from this scheduler.
         */
        fun deregisterInput(input: Input, now: Long) {
            // Assign a new input responsible for handling the convergence events
            if (_lastConvergeInput == input) {
                _lastConvergeInput = null
            }

            _activeInputs.remove(input)

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

                parent.onConverge(now)
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
            val parent = parent

            if (parent != null) {
                _lastConverge = now
                parent.onConverge(now)
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
                activationOutput.pull(now)
                return
            } else {
                runScheduler(now)
            }
        }

        /**
         * Synchronously run the scheduler of the multiplexer.
         */
        fun runScheduler(now: Long): Long {
            return try {
                _schedulerActive = true
                doRunScheduler(now)
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
                if (input.isCoupled) {
                    input.capacity = newCapacity
                }
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
        private fun doRunScheduler(now: Long): Long {
            val activeInputs = _activeInputs
            val activeOutputs = _activeOutputs
            var inputArray = _inputArray
            var inputSize = _inputArray.size

            // Update the counters of the scheduler
            updateCounters(now)

            // If there is no work yet, mark the inputs as idle.
            if (inputSize == 0) {
                demand = 0.0
                rate = 0.0
                return Long.MAX_VALUE
            }

            val capacity = capacity
            var availableCapacity = capacity
            var deadline = Long.MAX_VALUE
            var demand = 0.0
            var shouldRebuild = false

            // Pull in the work of the inputs
            for (i in 0 until inputSize) {
                val input = inputArray[i]

                input.pullSync(now)

                // Remove inputs that have finished
                if (!input.isActive) {
                    input.actualRate = 0.0
                    shouldRebuild = true
                } else {
                    demand += input.limit
                    deadline = min(deadline, input.deadline)
                }
            }

            // Slow-path: Rebuild the input array based on the (apparently) updated `activeInputs`
            if (shouldRebuild) {
                inputArray = activeInputs.toTypedArray()
                inputSize = inputArray.size
                _inputArray = inputArray
            }

            val rate = if (demand > capacity) {
                // If the demand is higher than the capacity, we need use max-min fair sharing to distribute the
                // constrained capacity across the inputs.

                // Sort in-place the inputs based on their pushed flow.
                // Profiling shows that it is faster than maintaining some kind of sorted set.
                inputArray.sort()

                // Divide the available output capacity fairly over the inputs using max-min fair sharing
                for (i in 0 until inputSize) {
                    val input = inputArray[i]
                    val availableShare = availableCapacity / (inputSize - i)
                    val grantedRate = min(input.allowedRate, availableShare)

                    availableCapacity -= grantedRate
                    input.actualRate = grantedRate
                }

                capacity - availableCapacity
            } else {
                demand
            }

            this.demand = demand
            if (this.rate != rate) {
                // Only update the outputs if the output rate has changed
                this.rate = rate

                // Divide the requests over the available capacity of the input resources fairly
                for (i in activeOutputs.indices) {
                    val output = activeOutputs[i]
                    val inputCapacity = output.capacity
                    val fraction = inputCapacity / capacity
                    val grantedSpeed = rate * fraction

                    output.push(grantedSpeed)
                }
            }

            return deadline
        }

        /**
         * The previous capacity of the multiplexer.
         */
        private var _previousCapacity = 0.0
        private var _previousUpdate = Long.MIN_VALUE

        /**
         * Update the counters of the scheduler.
         */
        fun updateCounters(now: Long) {
            val previousCapacity = _previousCapacity
            _previousCapacity = capacity

            val previousUpdate = _previousUpdate
            _previousUpdate = now

            val delta = now - previousUpdate
            if (delta <= 0) {
                return
            }

            val deltaS = delta * D_MS_TO_S
            val demand = demand
            val rate = rate

            counters.increment(
                demand = demand * deltaS,
                actual = rate * deltaS,
                remaining = (previousCapacity - rate) * deltaS
            )
        }
    }

    /**
     * An internal [FlowConsumer] implementation for multiplexer inputs.
     */
    private class Input(
        private val engine: FlowEngine,
        private val scheduler: Scheduler,
        @JvmField val isCoupled: Boolean,
        initialCapacity: Double
    ) : FlowConsumer, FlowConsumerLogic, Comparable<Input> {
        /**
         * A flag to indicate that the consumer is active.
         */
        override val isActive: Boolean
            get() = _ctx != null

        /**
         * The demand of the consumer.
         */
        override val demand: Double
            get() = limit

        /**
         * The processing rate of the consumer.
         */
        override val rate: Double
            get() = actualRate

        /**
         * The capacity of the input.
         */
        override var capacity: Double
            get() = _capacity
            set(value) {
                allowedRate = min(limit, value)
                _capacity = value
                _ctx?.capacity = value
            }
        private var _capacity = initialCapacity

        /**
         * The flow counters to track the flow metrics of the consumer.
         */
        override val counters: FlowCounters
            get() = _counters
        private val _counters = MutableFlowCounters()

        /**
         * A flag to enable timers for the input.
         */
        var enableTimers: Boolean = true
            set(value) {
                field = value
                _ctx?.enableTimers = value
            }

        /**
         * A flag to control whether the input should converge.
         */
        var shouldConsumerConverge: Boolean = true
            set(value) {
                field = value
                _ctx?.shouldConsumerConverge = value
            }

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
            get() = _ctx?.deadline ?: Long.MAX_VALUE

        /**
         * The [FlowConsumerContext] that is currently running.
         */
        private var _ctx: FlowConsumerContext? = null

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

        /**
         * Pull the source if necessary.
         */
        fun pullSync(now: Long) {
            _ctx?.pullSync(now)
        }

        /* FlowConsumer */
        override fun startConsumer(source: FlowSource) {
            check(!_isClosed) { "Cannot re-use closed input" }
            check(_ctx == null) { "Consumer is in invalid state" }

            val ctx = engine.newContext(source, this)
            _ctx = ctx

            ctx.capacity = capacity
            scheduler.registerInput(this)

            ctx.start()
        }

        override fun pull() {
            _ctx?.pull()
        }

        override fun cancel() {
            _ctx?.close()
        }

        /* FlowConsumerLogic */
        override fun onPush(
            ctx: FlowConsumerContext,
            now: Long,
            rate: Double
        ) {
            doUpdateCounters(now)

            val allowed = min(rate, capacity)
            limit = rate
            actualRate = allowed
            allowedRate = allowed

            scheduler.trigger(now)
        }

        override fun onFinish(ctx: FlowConsumerContext, now: Long, cause: Throwable?) {
            doUpdateCounters(now)

            limit = 0.0
            actualRate = 0.0
            allowedRate = 0.0

            scheduler.deregisterInput(this, now)

            _ctx = null
        }

        override fun onConverge(ctx: FlowConsumerContext, now: Long) {
            scheduler.convergeInput(this, now)
        }

        /* Comparable */
        override fun compareTo(other: Input): Int = allowedRate.compareTo(other.allowedRate)

        /**
         * The timestamp that the counters where last updated.
         */
        private var _lastUpdate = Long.MIN_VALUE

        /**
         * Helper method to update the flow counters of the multiplexer.
         */
        fun doUpdateCounters(now: Long) {
            val lastUpdate = _lastUpdate
            _lastUpdate = now

            val delta = (now - lastUpdate).coerceAtLeast(0)
            if (delta <= 0L) {
                return
            }

            val actualRate = actualRate

            val deltaS = delta * D_MS_TO_S
            val demand = limit * deltaS
            val actual = actualRate * deltaS
            val remaining = (_capacity - actualRate) * deltaS

            _counters.increment(demand, actual, remaining)
            scheduler.counters.increment(0.0, 0.0, 0.0)
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
        fun pull(now: Long) {
            _conn?.pull(now)
        }

        override fun onStart(conn: FlowConnection, now: Long) {
            assert(_conn == null) { "Source running concurrently" }
            _conn = conn
            capacity = conn.capacity
            isActive = true

            scheduler.registerOutput(this)
        }

        override fun onStop(conn: FlowConnection, now: Long) {
            _conn = null
            capacity = 0.0
            isActive = false

            scheduler.deregisterOutput(this, now)
        }

        override fun onPull(conn: FlowConnection, now: Long): Long {
            val capacity = capacity
            if (capacity != conn.capacity) {
                this.capacity = capacity
                scheduler.updateCapacity()
            }

            return if (_isActivationOutput) {
                // If this output is the activation output, synchronously run the scheduler and return the new deadline
                val deadline = scheduler.runScheduler(now)
                if (deadline == Long.MAX_VALUE) {
                    deadline
                } else {
                    deadline - now
                }
            } else {
                // Output is not the activation output, so trigger activation output and do not install timer for this
                // output (by returning `Long.MAX_VALUE`)
                scheduler.trigger(now)

                Long.MAX_VALUE
            }
        }

        override fun onConverge(conn: FlowConnection, now: Long) {
            if (_isActivationOutput) {
                scheduler.convergeOutput(this, now)
            }
        }

        override fun compareTo(other: Output): Int = capacity.compareTo(other.capacity)
    }
}
