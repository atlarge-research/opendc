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

import mu.KotlinLogging
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
     * The logging instance of this connection.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The capacity of the connection.
     */
    override var capacity: Double = 0.0
        set(value) {
            val oldValue = field

            // Only changes will be propagated
            if (value != oldValue) {
                field = value

                // Do not pull the source if it has not been started yet
                if (_state == State.Active) {
                    pull()
                }
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
     * Flags to control the convergence of the consumer and source.
     */
    override var shouldConsumerConverge: Boolean = false
    override var shouldSourceConverge: Boolean = false

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
     * A flag that indicates to the [FlowEngine] that the context is already enqueued to converge.
     */
    private var _willConverge: Boolean = false

    /**
     * The timestamp of calls to the callbacks.
     */
    private var _lastPull: Long = Long.MIN_VALUE // Last call to `onPull`
    private var _lastPush: Long = Long.MIN_VALUE // Last call to `onPush`
    private var _lastSourceConvergence: Long = Long.MAX_VALUE // Last call to source `onConvergence`
    private var _lastConsumerConvergence: Long = Long.MAX_VALUE // Last call to consumer `onConvergence`

    /**
     * The timers at which the context is scheduled to be interrupted.
     */
    private var _timer: FlowEngineImpl.Timer? = null
    private val _pendingTimers: ArrayDeque<FlowEngineImpl.Timer> = ArrayDeque(5)

    override fun start() {
        check(_state == State.Pending) { "Consumer is already started" }
        engine.batch {
            source.onStart(this, _clock.millis())
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
                doStopSource(now)

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
     * Update the state of the flow connection.
     *
     * @param now The current virtual timestamp.
     * @return A flag to indicate whether the connection has already been updated before convergence.
     */
    fun doUpdate(now: Long): Boolean {
        // The connection will only converge if either the source or the consumer wants the converge callback to be
        // invoked.
        val shouldConverge = shouldSourceConverge || shouldConsumerConverge
        var willConverge = false
        if (shouldConverge) {
            willConverge = _willConverge
            _willConverge = true
        }

        val oldState = _state
        if (oldState != State.Active) {
            return willConverge
        }

        _isUpdateActive = true
        _isImmediateUpdateScheduled = false

        try {
            // Pull the source if (1) `pull` is called or (2) the timer of the source has expired
            val deadline = if (_isPulled || _deadline == now) {
                val lastPull = _lastPull
                val delta = max(0, now - lastPull)

                _isPulled = false
                _lastPull = now

                val duration = source.onPull(this, now, delta)
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
                        val lastPush = _lastPush
                        val delta = max(0, now - lastPush)

                        _lastPush = now

                        logic.onPush(this, now, delta, demand)
                    }
                }
                State.Closed -> doStopSource(now)
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
            doFailSource(now, cause)
        } finally {
            _isUpdateActive = false
        }

        return willConverge
    }

    /**
     * Prune the elapsed timers from this context.
     */
    fun updateTimers() {
        // Invariant: Any pending timer should only point to a future timestamp
        // See also `scheduleDelayed`
        _timer = _pendingTimers.poll()
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
    fun onConverge(now: Long) {
        _willConverge = false

        try {
            if (_state == State.Active && shouldSourceConverge) {
                val delta = max(0, now - _lastSourceConvergence)
                _lastSourceConvergence = now

                source.onConverge(this, now, delta)
            }

            if (shouldConsumerConverge) {
                val delta = max(0, now - _lastConsumerConvergence)
                _lastConsumerConvergence = now

                logic.onConverge(this, now, delta)
            }
        } catch (cause: Throwable) {
            doFailSource(now, cause)
        }
    }

    override fun toString(): String = "FlowConsumerContextImpl[capacity=$capacity,rate=$_rate]"

    /**
     * Stop the [FlowSource].
     */
    private fun doStopSource(now: Long) {
        try {
            source.onStop(this, now, max(0, now - _lastPull))
            doFinishConsumer(now, null)
        } catch (cause: Throwable) {
            doFinishConsumer(now, cause)
            return
        } finally {
            _deadline = Long.MAX_VALUE
            _demand = 0.0
        }
    }

    /**
     * Fail the [FlowSource].
     */
    private fun doFailSource(now: Long, cause: Throwable) {
        try {
            source.onStop(this, now, max(0, now - _lastPull))
        } catch (e: Throwable) {
            e.addSuppressed(cause)
            doFinishConsumer(now, e)
        } finally {
            _deadline = Long.MAX_VALUE
            _demand = 0.0
        }
    }

    /**
     * Finish the consumer.
     */
    private fun doFinishConsumer(now: Long, cause: Throwable?) {
        try {
            logic.onFinish(this, now, max(0, now - _lastPush), cause)
        } catch (e: Throwable) {
            e.addSuppressed(cause)
            logger.error(e) { "Uncaught exception" }
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
        // Ignore any target scheduled at the maximum value
        // This indicates that the sources does not want to register a timer
        if (target == Long.MAX_VALUE) {
            return
        }

        val timer = _timer

        if (timer == null) {
            // No existing timer exists, so schedule a new timer and update the head
            _timer = engine.scheduleDelayed(now, this, target)
        } else if (target < timer.target) {
            // Existing timer is further in the future, so schedule a new timer ahead of it
            _timer = engine.scheduleDelayed(now, this, target)
            _pendingTimers.addFirst(timer)
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
