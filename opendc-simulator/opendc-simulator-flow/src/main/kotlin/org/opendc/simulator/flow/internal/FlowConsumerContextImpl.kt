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

package org.opendc.simulator.flow.internal

import org.opendc.simulator.flow.*
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of a [FlowConnection] managing the communication between flow sources and consumers.
 */
internal class FlowConsumerContextImpl(
    private val engine: FlowEngineImpl,
    private val source: FlowSource,
    private val logic: FlowConsumerLogic
) : FlowConsumerContext {
    /**
     * The capacity of the connection.
     */
    override var capacity: Double = 0.0
        set(value) {
            val oldValue = field

            // Only changes will be propagated
            if (value != oldValue) {
                field = value
                onCapacityChange()
            }
        }

    /**
     * The current processing rate of the connection.
     */
    override val rate: Double
        get() = _rate
    private var _rate = 0.0

    /**
     * The current flow processing demand.
     */
    override val demand: Double
        get() = _demand

    /**
     * The clock to track simulation time.
     */
    private val _clock = engine.clock

    /**
     * A flag to indicate the state of the connection.
     */
    private var _state = State.Pending

    /**
     * The current state of the connection.
     */
    private var _demand: Double = 0.0 // The current (pending) demand of the source
    private var _activeDemand: Double = 0.0 // The previous demand of the source
    private var _deadline: Long = Long.MAX_VALUE // The deadline of the source's timer

    /**
     * A flag to indicate that the source should be pulled.
     */
    private var _isPulled = false

    /**
     * A flag to indicate that an update is active.
     */
    private var _isUpdateActive = false

    /**
     * A flag to indicate that an immediate update is scheduled.
     */
    private var _isImmediateUpdateScheduled = false

    /**
     * The timestamp of calls to the callbacks.
     */
    private var _lastPull: Long = Long.MIN_VALUE // Last call to `onPull`
    private var _lastPush: Long = Long.MIN_VALUE // Last call to `onPush`
    private var _lastConvergence: Long = Long.MAX_VALUE // Last call to `onConvergence`

    /**
     * The timers at which the context is scheduled to be interrupted.
     */
    private val _timers: ArrayDeque<FlowEngineImpl.Timer> = ArrayDeque()

    override fun start() {
        check(_state == State.Pending) { "Consumer is already started" }
        engine.batch {
            source.onEvent(this, _clock.millis(), FlowEvent.Start)
            _state = State.Active
            pull()
        }
    }

    override fun close() {
        if (_state == State.Closed) {
            return
        }

        engine.batch {
            _state = State.Closed
            if (!_isUpdateActive) {
                val now = _clock.millis()
                val delta = max(0, now - _lastPull)
                doStop(now, delta)

                // FIX: Make sure the context converges
                pull()
            }
        }
    }

    override fun pull() {
        if (_state == State.Closed) {
            return
        }

        _isPulled = true
        scheduleImmediate()
    }

    override fun flush() {
        // Do not attempt to flush the connection if the connection is closed or an update is already active
        if (_state == State.Closed || _isUpdateActive) {
            return
        }

        engine.scheduleSync(_clock.millis(), this)
    }

    override fun push(rate: Double) {
        if (_demand == rate) {
            return
        }

        _demand = rate

        // Invalidate only if the active demand is changed and no update is active
        // If an update is active, it will already get picked up at the end of the update
        if (_activeDemand != rate && !_isUpdateActive) {
            scheduleImmediate()
        }
    }

    /**
     * Determine whether the state of the flow connection should be updated.
     */
    fun shouldUpdate(timestamp: Long): Boolean {
        // The flow connection should be updated for three reasons:
        //  (1) The source should be pulled (after a call to `pull`)
        //  (2) The demand of the source has changed (after a call to `push`)
        //  (3) The timer of the source expired
        return _isPulled || _demand != _activeDemand || _deadline == timestamp
    }

    /**
     * Update the state of the resource context.
     */
    fun doUpdate(now: Long) {
        val oldState = _state
        if (oldState != State.Active) {
            return
        }

        _isUpdateActive = true
        _isImmediateUpdateScheduled = false

        val lastPush = _lastPush
        val pushDelta = max(0, now - lastPush)

        try {
            // Pull the source if (1) `pull` is called or (2) the timer of the source has expired
            val deadline = if (_isPulled || _deadline == now) {
                val lastPull = _lastPull
                val pullDelta = max(0, now - lastPull)

                _isPulled = false
                _lastPull = now

                val duration = source.onPull(this, now, pullDelta)
                if (duration != Long.MAX_VALUE)
                    now + duration
                else
                    duration
            } else {
                _deadline
            }

            // Check whether the state has changed after [consumer.onNext]
            when (_state) {
                State.Active -> {
                    val demand = _demand
                    if (demand != _activeDemand) {
                        _lastPush = now

                        logic.onPush(this, now, pushDelta, demand)
                    }
                }
                State.Closed -> doStop(now, pushDelta)
                State.Pending -> throw IllegalStateException("Illegal transition to pending state")
            }

            // Note: pending limit might be changed by [logic.onConsume], so re-fetch the value
            val newLimit = _demand

            // Flush the changes to the flow
            _activeDemand = newLimit
            _deadline = deadline
            _rate = min(capacity, newLimit)

            // Schedule an update at the new deadline
            scheduleDelayed(now, deadline)
        } catch (cause: Throwable) {
            doFail(now, pushDelta, cause)
        } finally {
            _isUpdateActive = false
        }
    }

    /**
     * Prune the elapsed timers from this context.
     */
    fun pruneTimers(now: Long) {
        val timers = _timers
        while (true) {
            val head = timers.peek()
            if (head == null || head.target > now) {
                break
            }
            timers.poll()
        }
    }

    /**
     * Try to re-schedule the resource context in case it was skipped.
     */
    fun tryReschedule(now: Long) {
        val deadline = _deadline
        if (deadline > now && deadline != Long.MAX_VALUE) {
            scheduleDelayed(now, deadline)
        }
    }

    /**
     * This method is invoked when the system converges into a steady state.
     */
    fun onConverge(timestamp: Long) {
        val delta = max(0, timestamp - _lastConvergence)
        _lastConvergence = timestamp

        try {
            if (_state == State.Active) {
                source.onEvent(this, timestamp, FlowEvent.Converge)
            }

            logic.onConverge(this, timestamp, delta)
        } catch (cause: Throwable) {
            doFail(timestamp, max(0, timestamp - _lastPull), cause)
        }
    }

    override fun toString(): String = "FlowConsumerContextImpl[capacity=$capacity,rate=$_rate]"

    /**
     * Stop the resource context.
     */
    private fun doStop(now: Long, delta: Long) {
        try {
            source.onEvent(this, now, FlowEvent.Exit)
            logic.onFinish(this, now, delta)
        } catch (cause: Throwable) {
            doFail(now, delta, cause)
        } finally {
            _deadline = Long.MAX_VALUE
            _demand = 0.0
        }
    }

    /**
     * Fail the resource consumer.
     */
    private fun doFail(now: Long, delta: Long, cause: Throwable) {
        try {
            source.onFailure(this, cause)
        } catch (e: Throwable) {
            e.addSuppressed(cause)
            e.printStackTrace()
        }

        logic.onFinish(this, now, delta)
    }

    /**
     * Indicate that the capacity of the resource has changed.
     */
    private fun onCapacityChange() {
        // Do not inform the consumer if it has not been started yet
        if (_state != State.Active) {
            return
        }

        engine.batch {
            // Inform the consumer of the capacity change. This might already trigger an interrupt.
            source.onEvent(this, _clock.millis(), FlowEvent.Capacity)

            pull()
        }
    }

    /**
     * Schedule an immediate update for this connection.
     */
    private fun scheduleImmediate() {
        // In case an immediate update is already scheduled, no need to do anything
        if (_isImmediateUpdateScheduled) {
            return
        }

        _isImmediateUpdateScheduled = true

        val now = _clock.millis()
        engine.scheduleImmediate(now, this)
    }

    /**
     * Schedule a delayed update for this resource context.
     */
    private fun scheduleDelayed(now: Long, target: Long) {
        val timers = _timers
        if (target != Long.MAX_VALUE && (timers.isEmpty() || target < timers.peek().target)) {
            timers.addFirst(engine.scheduleDelayed(now, this, target))
        }
    }

    /**
     * The state of a flow connection.
     */
    private enum class State {
        /**
         * The connection is pending and the consumer is waiting to consume the source.
         */
        Pending,

        /**
         * The connection is active and the source is currently being consumed.
         */
        Active,

        /**
         * The connection is closed and the source cannot be consumed through this connection anymore.
         */
        Closed
    }
}
