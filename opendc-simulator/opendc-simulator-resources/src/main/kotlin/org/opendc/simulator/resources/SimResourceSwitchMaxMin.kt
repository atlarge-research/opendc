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
import kotlin.math.max
import kotlin.math.min

/**
 * A [SimResourceSwitch] implementation that switches resource consumptions over the available resources using max-min
 * fair sharing.
 *
 * @param interpreter The interpreter for managing the resource contexts.
 * @param parent The parent resource system of the switch.
 * @param interferenceDomain The interference domain of the switch.
 */
public class SimResourceSwitchMaxMin(
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem? = null,
    private val interferenceDomain: InterferenceDomain? = null
) : SimResourceSwitch {
    /**
     * The output resource providers to which resource consumers can be attached.
     */
    override val outputs: Set<SimResourceProvider>
        get() = _outputs
    private val _outputs = mutableSetOf<Output>()
    private val _activeOutputs: MutableList<Output> = mutableListOf()

    /**
     * The input resources that will be switched between the output providers.
     */
    override val inputs: Set<SimResourceProvider>
        get() = _inputs
    private val _inputs = mutableSetOf<SimResourceProvider>()
    private val _activeInputs = mutableListOf<Input>()

    /**
     * The resource counters of this switch.
     */
    public override val counters: SimResourceCounters
        get() = _counters
    private val _counters = SimResourceCountersImpl()

    /**
     * The actual processing rate of the switch.
     */
    private var _rate = 0.0

    /**
     * The demanded processing rate of the outputs.
     */
    private var _demand = 0.0

    /**
     * The capacity of the switch.
     */
    private var _capacity = 0.0

    /**
     * Flag to indicate that the scheduler is active.
     */
    private var _schedulerActive = false

    /**
     * Add an output to the switch.
     */
    override fun newOutput(key: InterferenceKey?): SimResourceProvider {
        val provider = Output(_capacity, key)
        _outputs.add(provider)
        return provider
    }

    /**
     * Add the specified [input] to the switch.
     */
    override fun addInput(input: SimResourceProvider) {
        val consumer = Input(input)
        if (_inputs.add(input)) {
            _activeInputs.add(consumer)
            input.startConsumer(consumer)
        }
    }

    /**
     * Remove [output] from this switch.
     */
    override fun removeOutput(output: SimResourceProvider) {
        if (!_outputs.remove(output)) {
            return
        }
        // This cast should always succeed since only `Output` instances should be added to _outputs
        (output as Output).close()
    }

    override fun clear() {
        for (input in _activeInputs) {
            input.cancel()
        }
        _activeInputs.clear()

        for (output in _activeOutputs) {
            output.cancel()
        }
        _activeOutputs.clear()
    }

    /**
     * Run the scheduler of the switch.
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
     * Schedule the outputs over the input.
     */
    private fun doSchedule(now: Long) {
        // If there is no work yet, mark the input as idle.
        if (_activeOutputs.isEmpty()) {
            return
        }

        val capacity = _capacity
        var availableCapacity = capacity

        // Pull in the work of the outputs
        val outputIterator = _activeOutputs.listIterator()
        for (output in outputIterator) {
            output.pull(now)

            // Remove outputs that have finished
            if (!output.isActive) {
                outputIterator.remove()
            }
        }

        var demand = 0.0

        // Sort in-place the outputs based on their requested usage.
        // Profiling shows that it is faster than maintaining some kind of sorted set.
        _activeOutputs.sort()

        // Divide the available input capacity fairly across the outputs using max-min fair sharing
        var remaining = _activeOutputs.size
        for (output in _activeOutputs) {
            val availableShare = availableCapacity / remaining--
            val grantedSpeed = min(output.allowedRate, availableShare)

            // Ignore idle computation
            if (grantedSpeed <= 0.0) {
                output.actualRate = 0.0
                continue
            }

            demand += output.limit

            output.actualRate = grantedSpeed
            availableCapacity -= grantedSpeed
        }

        val rate = capacity - availableCapacity

        _demand = demand
        _rate = rate

        // Sort all consumers by their capacity
        _activeInputs.sort()

        // Divide the requests over the available capacity of the input resources fairly
        for (input in _activeInputs) {
            val inputCapacity = input.capacity
            val fraction = inputCapacity / capacity
            val grantedSpeed = rate * fraction

            input.push(grantedSpeed)
        }
    }

    /**
     * Recompute the capacity of the switch.
     */
    private fun updateCapacity() {
        val newCapacity = _activeInputs.sumOf(Input::capacity)

        // No-op if the capacity is unchanged
        if (_capacity == newCapacity) {
            return
        }

        _capacity = newCapacity

        for (output in _outputs) {
            output.capacity = newCapacity
        }
    }

    /**
     * An internal [SimResourceProvider] implementation for switch outputs.
     */
    private inner class Output(capacity: Double, val key: InterferenceKey?) :
        SimAbstractResourceProvider(interpreter, capacity),
        SimResourceProviderLogic,
        Comparable<Output> {
        /**
         * The requested limit.
         */
        @JvmField var limit: Double = 0.0

        /**
         * The actual processing speed.
         */
        @JvmField var actualRate: Double = 0.0

        /**
         * The processing speed that is allowed by the model constraints.
         */
        val allowedRate: Double
            get() = min(capacity, limit)

        /**
         * A flag to indicate that the output is closed.
         */
        private var _isClosed: Boolean = false

        /**
         * The timestamp at which we received the last command.
         */
        private var _lastPull: Long = Long.MIN_VALUE

        /**
         * Close the output.
         *
         * This method is invoked when the user removes an output from the switch.
         */
        fun close() {
            _isClosed = true
            cancel()
        }

        /* SimAbstractResourceProvider */
        override fun createLogic(): SimResourceProviderLogic = this

        override fun start(ctx: SimResourceControllableContext) {
            check(!_isClosed) { "Cannot re-use closed output" }

            _activeOutputs += this
            super.start(ctx)
        }

        /* SimResourceProviderLogic */
        override fun onConsume(
            ctx: SimResourceControllableContext,
            now: Long,
            delta: Long,
            limit: Double,
            duration: Long
        ) {
            doUpdateCounters(delta)

            actualRate = 0.0
            this.limit = limit
            _lastPull = now

            runScheduler(now)
        }

        override fun onConverge(ctx: SimResourceControllableContext, now: Long, delta: Long) {
            parent?.onConverge(now)
        }

        override fun onFinish(ctx: SimResourceControllableContext, now: Long, delta: Long) {
            doUpdateCounters(delta)

            limit = 0.0
            actualRate = 0.0
            _lastPull = now
        }

        /* Comparable */
        override fun compareTo(other: Output): Int = allowedRate.compareTo(other.allowedRate)

        /**
         * Pull the next command if necessary.
         */
        fun pull(now: Long) {
            val ctx = ctx
            if (ctx != null && _lastPull < now) {
                ctx.flush()
            }
        }

        /**
         * Helper method to update the resource counters of the distributor.
         */
        private fun doUpdateCounters(delta: Long) {
            if (delta <= 0L) {
                return
            }

            // Compute the performance penalty due to resource interference
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
     * An internal [SimResourceConsumer] implementation for switch inputs.
     */
    private inner class Input(private val provider: SimResourceProvider) : SimResourceConsumer, Comparable<Input> {
        /**
         * The active [SimResourceContext] of this consumer.
         */
        private var _ctx: SimResourceContext? = null

        /**
         * The capacity of this input.
         */
        val capacity: Double
            get() = _ctx?.capacity ?: 0.0

        /**
         * Push the specified rate to the provider.
         */
        fun push(rate: Double) {
            _ctx?.push(rate)
        }

        /**
         * Cancel this input.
         */
        fun cancel() {
            provider.cancel()
        }

        override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
            runScheduler(now)
            return Long.MAX_VALUE
        }

        override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
            when (event) {
                SimResourceEvent.Start -> {
                    assert(_ctx == null) { "Consumer running concurrently" }
                    _ctx = ctx
                    updateCapacity()
                }
                SimResourceEvent.Exit -> {
                    _ctx = null
                    updateCapacity()
                }
                SimResourceEvent.Capacity -> updateCapacity()
                else -> {}
            }
        }

        override fun compareTo(other: Input): Int = capacity.compareTo(other.capacity)
    }
}
