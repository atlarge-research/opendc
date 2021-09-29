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

package org.opendc.simulator.resources.impl

import org.opendc.simulator.resources.*
import java.time.Clock
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of a [SimResourceContext] managing the communication between resources and resource consumers.
 */
internal class SimResourceContextImpl(
    private val interpreter: SimResourceInterpreterImpl,
    private val consumer: SimResourceConsumer,
    private val logic: SimResourceProviderLogic
) : SimResourceControllableContext {
    /**
     * The clock of the context.
     */
    override val clock: Clock
        get() = _clock
    private val _clock = interpreter.clock

    /**
     * The capacity of the resource.
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
     * A flag to indicate the state of the context.
     */
    private var _state = State.Pending

    /**
     * The current processing speed of the resource.
     */
    override val speed: Double
        get() = _rate
    private var _rate = 0.0

    /**
     * The current resource processing demand.
     */
    override val demand: Double
        get() = _limit

    /**
     * The current state of the resource context.
     */
    private var _limit: Double = 0.0
    private var _activeLimit: Double = 0.0
    private var _deadline: Long = Long.MIN_VALUE

    /**
     * A flag to indicate that an update is active.
     */
    private var _updateActive = false

    /**
     * The update flag indicating why the update was triggered.
     */
    private var _flag: Int = 0

    /**
     * The timestamp of calls to the callbacks.
     */
    private var _lastUpdate: Long = Long.MIN_VALUE
    private var _lastConvergence: Long = Long.MAX_VALUE

    /**
     * The timers at which the context is scheduled to be interrupted.
     */
    private val _timers: ArrayDeque<SimResourceInterpreterImpl.Timer> = ArrayDeque()

    override fun start() {
        check(_state == State.Pending) { "Consumer is already started" }
        interpreter.batch {
            consumer.onEvent(this, SimResourceEvent.Start)
            _state = State.Active
            interrupt()
        }
    }

    override fun close() {
        if (_state == State.Stopped) {
            return
        }

        interpreter.batch {
            _state = State.Stopped
            if (!_updateActive) {
                val now = clock.millis()
                val delta = max(0, now - _lastUpdate)
                doStop(now, delta)

                // FIX: Make sure the context converges
                _flag = _flag or FLAG_INVALIDATE
                scheduleUpdate(clock.millis())
            }
        }
    }

    override fun interrupt() {
        if (_state == State.Stopped) {
            return
        }

        _flag = _flag or FLAG_INTERRUPT
        scheduleUpdate(clock.millis())
    }

    override fun invalidate() {
        if (_state == State.Stopped) {
            return
        }

        _flag = _flag or FLAG_INVALIDATE
        scheduleUpdate(clock.millis())
    }

    override fun flush() {
        if (_state == State.Stopped) {
            return
        }

        interpreter.scheduleSync(clock.millis(), this)
    }

    override fun push(rate: Double) {
        if (_limit == rate) {
            return
        }

        _limit = rate

        // Invalidate only if the active limit is change and no update is active
        // If an update is active, it will already get picked up at the end of the update
        if (_activeLimit != rate && !_updateActive) {
            invalidate()
        }
    }

    /**
     * Determine whether the state of the resource context should be updated.
     */
    fun shouldUpdate(timestamp: Long): Boolean {
        // Either the resource context is flagged or there is a pending update at this timestamp
        return _flag != 0 || _limit != _activeLimit || _deadline == timestamp
    }

    /**
     * Update the state of the resource context.
     */
    fun doUpdate(now: Long) {
        val oldState = _state
        if (oldState != State.Active) {
            return
        }

        val lastUpdate = _lastUpdate

        _lastUpdate = now
        _updateActive = true

        val delta = max(0, now - lastUpdate)

        try {
            val duration = consumer.onNext(this, now, delta)
            val newDeadline = if (duration != Long.MAX_VALUE) now + duration else duration

            // Reset update flags
            _flag = 0

            // Check whether the state has changed after [consumer.onNext]
            when (_state) {
                State.Active -> {
                    logic.onConsume(this, now, delta, _limit, duration)

                    // Schedule an update at the new deadline
                    scheduleUpdate(now, newDeadline)
                }
                State.Stopped -> doStop(now, delta)
                State.Pending -> throw IllegalStateException("Illegal transition to pending state")
            }

            // Note: pending limit might be changed by [logic.onConsume], so re-fetch the value
            val newLimit = _limit

            // Flush the changes to the flow
            _activeLimit = newLimit
            _deadline = newDeadline
            _rate = min(capacity, newLimit)
        } catch (cause: Throwable) {
            doFail(now, delta, cause)
        } finally {
            _updateActive = false
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
            scheduleUpdate(now, deadline)
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
                consumer.onEvent(this, SimResourceEvent.Run)
            }

            logic.onConverge(this, timestamp, delta)
        } catch (cause: Throwable) {
            doFail(timestamp, max(0, timestamp - _lastUpdate), cause)
        }
    }

    override fun toString(): String = "SimResourceContextImpl[capacity=$capacity,rate=$_rate]"

    /**
     * Stop the resource context.
     */
    private fun doStop(now: Long, delta: Long) {
        try {
            consumer.onEvent(this, SimResourceEvent.Exit)
            logic.onFinish(this, now, delta)
        } catch (cause: Throwable) {
            doFail(now, delta, cause)
        } finally {
            _deadline = Long.MAX_VALUE
            _limit = 0.0
        }
    }

    /**
     * Fail the resource consumer.
     */
    private fun doFail(now: Long, delta: Long, cause: Throwable) {
        try {
            consumer.onFailure(this, cause)
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

        interpreter.batch {
            // Inform the consumer of the capacity change. This might already trigger an interrupt.
            consumer.onEvent(this, SimResourceEvent.Capacity)

            interrupt()
        }
    }

    /**
     * Schedule an update for this resource context.
     */
    private fun scheduleUpdate(now: Long) {
        interpreter.scheduleImmediate(now, this)
    }

    /**
     * Schedule a delayed update for this resource context.
     */
    private fun scheduleUpdate(now: Long, target: Long) {
        val timers = _timers
        if (target != Long.MAX_VALUE && (timers.isEmpty() || target < timers.peek().target)) {
            timers.addFirst(interpreter.scheduleDelayed(now, this, target))
        }
    }

    /**
     * The state of a resource context.
     */
    private enum class State {
        /**
         * The resource context is pending and the resource is waiting to be consumed.
         */
        Pending,

        /**
         * The resource context is active and the resource is currently being consumed.
         */
        Active,

        /**
         * The resource context is stopped and the resource cannot be consumed anymore.
         */
        Stopped
    }

    /**
     * A flag to indicate that the context should be invalidated.
     */
    private val FLAG_INVALIDATE = 0b01

    /**
     * A flag to indicate that the context should be interrupted.
     */
    private val FLAG_INTERRUPT = 0b10
}
