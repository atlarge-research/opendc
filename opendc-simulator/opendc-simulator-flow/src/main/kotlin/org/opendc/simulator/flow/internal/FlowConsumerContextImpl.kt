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
import java.util.*
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
    override var capacity: Double
        get() = _capacity
        set(value) {
            val oldValue = _capacity

            // Only changes will be propagated
            if (value != oldValue) {
                _capacity = value
                pull()
            }
        }
    private var _capacity: Double = 0.0

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
    private var _demand: Double = 0.0 // The current (pending) demand of the source

    /**
     * The deadline of the source.
     */
    override val deadline: Long
        get() = _deadline
    private var _deadline: Long = Long.MAX_VALUE // The deadline of the source's timer

    /**
     * Flags to control the convergence of the consumer and source.
     */
    override var shouldSourceConverge: Boolean
        get() = _flags and ConnConvergeSource == ConnConvergeSource
        set(value) {
            _flags =
                if (value)
                    _flags or ConnConvergeSource
                else
                    _flags and ConnConvergeSource.inv()
        }
    override var shouldConsumerConverge: Boolean
        get() = _flags and ConnConvergeConsumer == ConnConvergeConsumer
        set(value) {
            _flags =
                if (value)
                    _flags or ConnConvergeConsumer
                else
                    _flags and ConnConvergeConsumer.inv()
        }

    /**
     * Flag to control the timers on the [FlowSource]
     */
    override var enableTimers: Boolean
        get() = _flags and ConnDisableTimers != ConnDisableTimers
        set(value) {
            _flags =
                if (!value)
                    _flags or ConnDisableTimers
                else
                    _flags and ConnDisableTimers.inv()
        }

    /**
     * The clock to track simulation time.
     */
    private val _clock = engine.clock

    /**
     * The flags of the flow connection, indicating certain actions.
     */
    private var _flags: Int = 0

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
    private var _timer: Long = Long.MAX_VALUE
    private val _pendingTimers: ArrayDeque<Long> = ArrayDeque(5)

    override fun start() {
        check(_flags and ConnState == ConnPending) { "Consumer is already started" }
        engine.batch {
            val now = _clock.millis()
            source.onStart(this, now)

            // Mark the connection as active and pulled
            val newFlags = (_flags and ConnState.inv()) or ConnActive or ConnPulled
            scheduleImmediate(now, newFlags)
        }
    }

    override fun close() {
        val flags = _flags
        if (flags and ConnState == ConnClosed) {
            return
        }

        // Toggle the close bit. In case no update is active, schedule a new update.
        if (flags and ConnUpdateActive == 0) {
            val now = _clock.millis()
            scheduleImmediate(now, flags or ConnClose)
        } else {
            _flags = flags or ConnClose
        }
    }

    override fun pull(now: Long) {
        val flags = _flags
        if (flags and ConnState != ConnActive) {
            return
        }

        // Mark connection as pulled
        scheduleImmediate(now, flags or ConnPulled)
    }

    override fun pull() {
        pull(_clock.millis())
    }

    override fun pullSync(now: Long) {
        val flags = _flags

        // Do not attempt to flush the connection if the connection is closed or an update is already active
        if (flags and ConnState != ConnActive || flags and ConnUpdateActive != 0) {
            return
        }

        if (flags and (ConnPulled or ConnPushed) != 0 || _deadline == now) {
            engine.scheduleSync(now, this)
        }
    }

    override fun push(rate: Double) {
        if (_demand == rate) {
            return
        }

        _demand = rate

        val flags = _flags

        if (flags and ConnUpdateActive != 0) {
            // If an update is active, it will already get picked up at the end of the update
            _flags = flags or ConnPushed
        } else {
            // Invalidate only if no update is active
            scheduleImmediate(_clock.millis(), flags or ConnPushed)
        }
    }

    /**
     * Update the state of the flow connection.
     *
     * @param now The current virtual timestamp.
     * @param visited The queue of connections that have been visited during the cycle.
     * @param timerQueue The queue of all pending timers.
     * @param isImmediate A flag to indicate that this invocation is an immediate update or a delayed update.
     */
    fun doUpdate(
        now: Long,
        visited: FlowDeque,
        timerQueue: FlowTimerQueue,
        isImmediate: Boolean
    ) {
        var flags = _flags

        // Precondition: The flow connection must be active
        if (flags and ConnState != ConnActive) {
            return
        }

        val deadline = _deadline
        val reachedDeadline = deadline == now
        var newDeadline: Long
        var hasUpdated = false

        try {
            // Pull the source if (1) `pull` is called or (2) the timer of the source has expired
            newDeadline = if (flags and ConnPulled != 0 || reachedDeadline) {
                val lastPull = _lastPull
                val delta = max(0, now - lastPull)

                // Update state before calling into the outside world, so it observes a consistent state
                _lastPull = now
                _flags = (flags and ConnPulled.inv()) or ConnUpdateActive
                hasUpdated = true

                val duration = source.onPull(this, now, delta)

                // IMPORTANT: Re-fetch the flags after the callback might have changed those
                flags = _flags

                if (duration != Long.MAX_VALUE)
                    now + duration
                else
                    duration
            } else {
                deadline
            }

            // Make the new deadline available for the consumer if it has changed
            if (newDeadline != deadline) {
                _deadline = newDeadline
            }

            // Push to the consumer if the rate of the source has changed (after a call to `push`)
            if (flags and ConnPushed != 0) {
                val lastPush = _lastPush
                val delta = max(0, now - lastPush)

                // Update state before calling into the outside world, so it observes a consistent state
                _lastPush = now
                _flags = (flags and ConnPushed.inv()) or ConnUpdateActive
                hasUpdated = true

                logic.onPush(this, now, delta, _demand)

                // IMPORTANT: Re-fetch the flags after the callback might have changed those
                flags = _flags
            }

            // Check whether the source or consumer have tried to close the connection
            if (flags and ConnClose != 0) {
                hasUpdated = true

                // The source has called [FlowConnection.close], so clean up the connection
                doStopSource(now)

                // IMPORTANT: Re-fetch the flags after the callback might have changed those
                // We now also mark the connection as closed
                flags = (_flags and ConnState.inv()) or ConnClosed

                _demand = 0.0
                newDeadline = Long.MAX_VALUE
            }
        } catch (cause: Throwable) {
            hasUpdated = true

            // Clean up the connection
            doFailSource(now, cause)

            // Mark the connection as closed
            flags = (flags and ConnState.inv()) or ConnClosed

            _demand = 0.0
            newDeadline = Long.MAX_VALUE
        }

        // Check whether the connection needs to be added to the visited queue. This is the case when:
        //  (1) An update was performed (either a push or a pull)
        //  (2) Either the source or consumer want to converge, and
        //  (3) Convergence is not already pending (ConnConvergePending)
        if (hasUpdated && flags and (ConnConvergeSource or ConnConvergeConsumer) != 0 && flags and ConnConvergePending == 0) {
            visited.add(this)
            flags = flags or ConnConvergePending
        }

        // Compute the new flow rate of the connection
        // Note: _demand might be changed by [logic.onConsume], so we must re-fetch the value
        _rate = min(_capacity, _demand)

        // Indicate that no update is active anymore and flush the flags
        _flags = flags and ConnUpdateActive.inv() and ConnUpdatePending.inv()

        val pendingTimers = _pendingTimers

        // Prune the head timer if this is a delayed update
        val timer = if (!isImmediate) {
            // Invariant: Any pending timer should only point to a future timestamp
            val timer = pendingTimers.poll() ?: Long.MAX_VALUE
            _timer = timer
            timer
        } else {
            _timer
        }

        // Check whether we need to schedule a new timer for this connection. That is the case when:
        // (1) The deadline is valid (not the maximum value)
        // (2) The connection is active
        // (3) Timers are not disabled for the source
        // (4) The current active timer for the connection points to a later deadline
        if (newDeadline == Long.MAX_VALUE ||
            flags and ConnState != ConnActive ||
            flags and ConnDisableTimers != 0 ||
            (timer != Long.MAX_VALUE && newDeadline >= timer)
        ) {
            // Ignore any deadline scheduled at the maximum value
            // This indicates that the source does not want to register a timer
            return
        }

        // Construct a timer for the new deadline and add it to the global queue of timers
        _timer = newDeadline
        timerQueue.add(this, newDeadline)

        // Slow-path: a timer already exists for this connection, so add it to the queue of pending timers
        if (timer != Long.MAX_VALUE) {
            pendingTimers.addFirst(timer)
        }
    }

    /**
     * This method is invoked when the system converges into a steady state.
     */
    fun onConverge(now: Long) {
        try {
            val flags = _flags

            // The connection is converging now, so unset the convergence pending flag
            _flags = flags and ConnConvergePending.inv()

            // Call the source converge callback if it has enabled convergence
            if (flags and ConnConvergeSource != 0) {
                val delta = max(0, now - _lastSourceConvergence)
                _lastSourceConvergence = now

                source.onConverge(this, now, delta)
            }

            // Call the consumer callback if it has enabled convergence
            if (flags and ConnConvergeConsumer != 0) {
                val delta = max(0, now - _lastConsumerConvergence)
                _lastConsumerConvergence = now

                logic.onConverge(this, now, delta)
            }
        } catch (cause: Throwable) {
            // Invoke the finish callbacks
            doFailSource(now, cause)

            // Mark the connection as closed
            _flags = (_flags and ConnState.inv()) or ConnClosed
            _demand = 0.0
            _deadline = Long.MAX_VALUE
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
    private fun scheduleImmediate(now: Long, flags: Int) {
        // In case an immediate update is already scheduled, no need to do anything
        if (flags and ConnUpdatePending != 0) {
            _flags = flags
            return
        }

        // Mark the connection that there is an update pending
        _flags = flags or ConnUpdatePending

        engine.scheduleImmediate(now, this)
    }
}
