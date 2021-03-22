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

import java.time.Clock
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Partial implementation of a [SimResourceContext] managing the communication between resources and resource consumers.
 */
public abstract class SimAbstractResourceContext<R : SimResource>(
    override val resource: R,
    override val clock: Clock,
    private val consumer: SimResourceConsumer<R>
) : SimResourceContext<R> {
    /**
     * The capacity of the resource.
     */
    override val capacity: Double
        get() = resource.capacity

    /**
     * The amount of work still remaining at this instant.
     */
    override val remainingWork: Double
        get() {
            val activeCommand = activeCommand ?: return 0.0
            return computeRemainingWork(activeCommand, clock.millis())
        }

    /**
     * This method is invoked when the resource will idle until the specified [deadline].
     */
    public abstract fun onIdle(deadline: Long)

    /**
     * This method is invoked when the resource will be consumed until the specified [work] was processed or the
     * [deadline] was reached.
     */
    public abstract fun onConsume(work: Double, limit: Double, deadline: Long)

    /**
     * This method is invoked when the resource consumer has finished.
     */
    public open fun onFinish(cause: Throwable?) {
        consumer.onFinish(this, cause)
    }

    /**
     * Compute the duration that a resource consumption will take with the specified [speed].
     */
    protected open fun getDuration(work: Double, speed: Double): Long {
        return ceil(work / speed * 1000).toLong()
    }

    /**
     * Compute the speed at which the resource may be consumed.
     */
    protected open fun getSpeed(limit: Double): Double {
        return min(limit, capacity)
    }

    /**
     * Get the remaining work to process after a resource consumption.
     *
     * @param work The size of the resource consumption.
     * @param speed The speed of consumption.
     * @param duration The duration from the start of the consumption until now.
     * @return The amount of work remaining.
     */
    protected open fun getRemainingWork(work: Double, speed: Double, duration: Long): Double {
        return if (duration > 0L) {
            val processed = duration / 1000.0 * speed
            max(0.0, work - processed)
        } else {
            0.0
        }
    }

    /**
     * Start the consumer.
     */
    public fun start() {
        check(state == SimResourceState.Pending) { "Consumer is already started" }

        val now = clock.millis()

        state = SimResourceState.Active
        isProcessing = true
        latestFlush = now

        try {
            consumer.onStart(this)
            activeCommand = interpret(consumer.onNext(this), now)
        } catch (cause: Throwable) {
            doStop(cause)
        } finally {
            isProcessing = false
        }
    }

    /**
     * Immediately stop the consumer.
     */
    public fun stop() {
        try {
            isProcessing = true
            latestFlush = clock.millis()

            flush(isIntermediate = true)
            doStop(null)
        } catch (cause: Throwable) {
            doStop(cause)
        } finally {
            isProcessing = false
        }
    }

    /**
     * Flush the current active resource consumption.
     *
     * @param isIntermediate A flag to indicate that the intermediate progress of the resource consumer should be
     * flushed, but without interrupting the resource consumer to submit a new command. If false, the resource consumer
     * will be asked to deliver a new command and is essentially interrupted.
     */
    public fun flush(isIntermediate: Boolean = false) {
        // Flush is no-op when the consumer is finished or not yet started
        if (state != SimResourceState.Active) {
            return
        }

        val now = clock.millis()

        // Fast path: if the intermediate progress was already flushed at the current instant, we can skip it.
        if (isIntermediate && latestFlush >= now) {
            return
        }

        try {
            val activeCommand = activeCommand ?: return
            val (timestamp, command) = activeCommand

            // Note: accessor is reliant on activeCommand being set
            val remainingWork = remainingWork

            isProcessing = true

            val duration = now - timestamp
            assert(duration >= 0) { "Flush in the past" }

            this.activeCommand = when (command) {
                is SimResourceCommand.Idle -> {
                    // We should only continue processing the next command if:
                    // 1. The resource consumer reached its deadline.
                    // 2. The resource consumer should be interrupted (e.g., someone called .interrupt())
                    if (command.deadline <= now || !isIntermediate) {
                        next(now)
                    } else {
                        activeCommand
                    }
                }
                is SimResourceCommand.Consume -> {
                    // We should only continue processing the next command if:
                    // 1. The resource consumption was finished.
                    // 2. The resource consumer reached its deadline.
                    // 3. The resource consumer should be interrupted (e.g., someone called .interrupt())
                    if (remainingWork == 0.0 || command.deadline <= now || !isIntermediate) {
                        next(now)
                    } else {
                        interpret(SimResourceCommand.Consume(remainingWork, command.limit, command.deadline), now)
                    }
                }
                SimResourceCommand.Exit ->
                    // Flush may not be called when the resource consumer has finished
                    throw IllegalStateException()
            }
        } catch (cause: Throwable) {
            doStop(cause)
        } finally {
            latestFlush = now
            isProcessing = false
        }
    }

    override fun interrupt() {
        // Prevent users from interrupting the resource while they are constructing their next command, as this will
        // only lead to infinite recursion.
        if (isProcessing) {
            return
        }

        flush()
    }

    override fun toString(): String = "SimAbstractResourceContext[resource=$resource]"

    /**
     * A flag to indicate that the resource is currently processing a command.
     */
    protected var isProcessing: Boolean = false

    /**
     * A flag to indicate the state of the context.
     */
    private var state: SimResourceState = SimResourceState.Pending

    /**
     * The current command that is being processed.
     */
    private var activeCommand: CommandWrapper? = null

    /**
     * The latest timestamp at which the resource was flushed.
     */
    private var latestFlush: Long = Long.MIN_VALUE

    /**
     * Finish the consumer and resource provider.
     */
    private fun doStop(cause: Throwable?) {
        val state = state
        this.state = SimResourceState.Stopped

        if (state == SimResourceState.Active) {
            activeCommand = null
            onFinish(cause)
        }
    }

    /**
     * Interpret the specified [SimResourceCommand] that was submitted by the resource consumer.
     */
    private fun interpret(command: SimResourceCommand, now: Long): CommandWrapper? {
        when (command) {
            is SimResourceCommand.Idle -> {
                val deadline = command.deadline

                require(deadline >= now) { "Deadline already passed" }

                onIdle(deadline)
            }
            is SimResourceCommand.Consume -> {
                val work = command.work
                val limit = command.limit
                val deadline = command.deadline

                require(deadline >= now) { "Deadline already passed" }

                onConsume(work, limit, deadline)
            }
            is SimResourceCommand.Exit -> {
                doStop(null)
                // No need to set the next active command
                return null
            }
        }

        return CommandWrapper(now, command)
    }

    /**
     * Request the workload for more work.
     */
    private fun next(now: Long): CommandWrapper? = interpret(consumer.onNext(this), now)

    /**
     * Compute the remaining work based on the specified [wrapper] and [timestamp][now].
     */
    private fun computeRemainingWork(wrapper: CommandWrapper, now: Long): Double {
        val (timestamp, command) = wrapper
        val duration = now - timestamp
        return when (command) {
            is SimResourceCommand.Consume -> {
                val speed = getSpeed(command.limit)
                getRemainingWork(command.work, speed, duration)
            }
            is SimResourceCommand.Idle, SimResourceCommand.Exit -> 0.0
        }
    }

    /**
     * This class wraps a [command] with the timestamp it was started and possibly the task associated with it.
     */
    private data class CommandWrapper(val timestamp: Long, val command: SimResourceCommand)
}
