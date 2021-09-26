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
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of a [SimResourceContext] managing the communication between resources and resource consumers.
 */
internal class SimResourceContextImpl(
    override val parent: SimResourceSystem?,
    private val interpreter: SimResourceInterpreterImpl,
    private val consumer: SimResourceConsumer,
    private val logic: SimResourceProviderLogic
) : SimResourceControllableContext, SimResourceSystem {
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
    override val state: SimResourceState
        get() = _state
    private var _state = SimResourceState.Pending

    /**
     * The current processing speed of the resource.
     */
    override val speed: Double
        get() = _speed
    private var _speed = 0.0

    /**
     * The current resource processing demand.
     */
    override val demand: Double
        get() = _limit

    /**
     * The current state of the resource context.
     */
    private var _timestamp: Long = Long.MIN_VALUE
    private var _limit: Double = 0.0
    private var _duration: Long = Long.MAX_VALUE
    private var _deadline: Long = Long.MAX_VALUE

    /**
     * The update flag indicating why the update was triggered.
     */
    private var _flag: Int = 0

    /**
     * The current pending update.
     */
    private var _pendingUpdate: SimResourceInterpreterImpl.Update? = null

    override fun start() {
        check(_state == SimResourceState.Pending) { "Consumer is already started" }
        interpreter.batch {
            consumer.onEvent(this, SimResourceEvent.Start)
            _state = SimResourceState.Active
            interrupt()
        }
    }

    override fun close() {
        if (_state != SimResourceState.Stopped) {
            interpreter.batch {
                _state = SimResourceState.Stopped
                doStop()
            }
        }
    }

    override fun interrupt() {
        if (_state == SimResourceState.Stopped) {
            return
        }

        _flag = _flag or FLAG_INTERRUPT
        scheduleUpdate()
    }

    override fun invalidate() {
        if (_state == SimResourceState.Stopped) {
            return
        }

        _flag = _flag or FLAG_INVALIDATE
        scheduleUpdate()
    }

    override fun flush() {
        if (_state == SimResourceState.Stopped) {
            return
        }

        interpreter.scheduleSync(this)
    }

    /**
     * Determine whether the state of the resource context should be updated.
     */
    fun requiresUpdate(timestamp: Long): Boolean {
        // Either the resource context is flagged or there is a pending update at this timestamp
        return _flag != 0 || _pendingUpdate?.timestamp == timestamp
    }

    /**
     * Update the state of the resource context.
     */
    fun doUpdate(timestamp: Long) {
        try {
            val oldState = _state
            val newState = doUpdate(timestamp, oldState)

            _state = newState
            _flag = 0

            when (newState) {
                SimResourceState.Pending ->
                    if (oldState != SimResourceState.Pending) {
                        throw IllegalStateException("Illegal transition to pending state")
                    }
                SimResourceState.Stopped ->
                    if (oldState != SimResourceState.Stopped) {
                        doStop()
                    }
                else -> {}
            }
        } catch (cause: Throwable) {
            doFail(cause)
        } finally {
            _timestamp = timestamp
        }
    }

    override fun onConverge(timestamp: Long) {
        if (_state == SimResourceState.Active) {
            consumer.onEvent(this, SimResourceEvent.Run)
        }
    }

    override fun toString(): String = "SimResourceContextImpl[capacity=$capacity]"

    /**
     * Update the state of the resource context.
     */
    private fun doUpdate(timestamp: Long, state: SimResourceState): SimResourceState {
        return when (state) {
            // Resource context is not active, so its state will not update
            SimResourceState.Pending, SimResourceState.Stopped -> state
            SimResourceState.Active -> {
                val isInterrupted = _flag and FLAG_INTERRUPT != 0
                val reachedDeadline = _deadline <= timestamp
                val delta = max(0, timestamp - _timestamp)

                // Update the resource counters only if there is some progress
                if (timestamp > _timestamp) {
                    logic.onUpdate(this, delta, _limit, reachedDeadline)
                }

                // We should only continue processing the next command if:
                // 1. The resource consumption was finished.
                // 2. The resource capacity cannot satisfy the demand.
                // 3. The resource consumer should be interrupted (e.g., someone called .interrupt())
                if (reachedDeadline || isInterrupted) {
                    when (val command = consumer.onNext(this, timestamp, delta)) {
                        is SimResourceCommand.Consume -> interpretConsume(timestamp, command.limit, command.duration)
                        is SimResourceCommand.Exit -> interpretExit()
                    }
                } else {
                    interpretConsume(timestamp, _limit, _duration - delta)
                }
            }
        }
    }

    /**
     * Stop the resource context.
     */
    private fun doStop() {
        try {
            consumer.onEvent(this, SimResourceEvent.Exit)
            logic.onFinish(this)
        } catch (cause: Throwable) {
            doFail(cause)
        }
    }

    /**
     * Fail the resource consumer.
     */
    private fun doFail(cause: Throwable) {
        try {
            consumer.onFailure(this, cause)
        } catch (e: Throwable) {
            e.addSuppressed(cause)
            e.printStackTrace()
        }

        logic.onFinish(this)
    }

    /**
     * Interpret the [SimResourceCommand.Consume] command.
     */
    private fun interpretConsume(now: Long, limit: Double, duration: Long): SimResourceState {
        _speed = min(capacity, limit)
        _limit = limit
        _duration = duration

        val timestamp = logic.onConsume(this, now, limit, duration)

        _deadline = timestamp

        scheduleUpdate(timestamp)

        return SimResourceState.Active
    }

    /**
     * Interpret the [SimResourceCommand.Exit] command.
     */
    private fun interpretExit(): SimResourceState {
        _speed = 0.0
        _limit = 0.0
        _duration = Long.MAX_VALUE
        _deadline = Long.MAX_VALUE

        return SimResourceState.Stopped
    }

    /**
     * Indicate that the capacity of the resource has changed.
     */
    private fun onCapacityChange() {
        // Do not inform the consumer if it has not been started yet
        if (state != SimResourceState.Active) {
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
    private fun scheduleUpdate() {
        // Cancel the pending update
        val pendingUpdate = _pendingUpdate
        if (pendingUpdate != null) {
            _pendingUpdate = null
            pendingUpdate.cancel()
        }

        interpreter.scheduleImmediate(this)
    }

    /**
     * Schedule a delayed update for this resource context.
     */
    private fun scheduleUpdate(timestamp: Long) {
        val pendingUpdate = _pendingUpdate
        if (pendingUpdate != null) {
            if (pendingUpdate.timestamp == timestamp) {
                // Fast-path: A pending update for the same timestamp already exists
                return
            } else {
                // Cancel the old pending update
                _pendingUpdate = null
                pendingUpdate.cancel()
            }
        }

        if (timestamp != Long.MAX_VALUE) {
            _pendingUpdate = interpreter.scheduleDelayed(this, timestamp)
        }
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
