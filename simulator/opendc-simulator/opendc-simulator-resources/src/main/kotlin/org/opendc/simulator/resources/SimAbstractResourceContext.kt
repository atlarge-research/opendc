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
    public abstract fun onFinish()

    /**
     * This method is invoked when the resource consumer throws an exception.
     */
    public abstract fun onFailure(cause: Throwable)

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
        return min(limit, resource.capacity)
    }

    /**
     * Get the remaining work to process after a resource consumption was flushed.
     *
     * @param work The size of the resource consumption.
     * @param speed The speed of consumption.
     * @param duration The duration from the start of the consumption until now.
     * @param isInterrupted A flag to indicate that the resource consumption could not be fully processed due to
     * it being interrupted before it could finish or reach its deadline.
     * @return The amount of work remaining.
     */
    protected open fun getRemainingWork(work: Double, speed: Double, duration: Long, isInterrupted: Boolean): Double {
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
        try {
            isProcessing = true
            latestFlush = clock.millis()

            interpret(consumer.onStart(this))
        } catch (e: Throwable) {
            onFailure(e)
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
            onFinish()
        } catch (e: Throwable) {
            onFailure(e)
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
    public open fun flush(isIntermediate: Boolean = false) {
        val now = clock.millis()

        // Fast path: if the intermediate progress was already flushed at the current instant, we can skip it.
        if (isIntermediate && latestFlush >= now) {
            return
        }

        try {
            val (timestamp, command) = activeCommand ?: return

            isProcessing = true
            activeCommand = null

            val duration = now - timestamp
            assert(duration >= 0) { "Flush in the past" }

            when (command) {
                is SimResourceCommand.Idle -> {
                    // We should only continue processing the next command if:
                    // 1. The resource consumer reached its deadline.
                    // 2. The resource consumer should be interrupted (e.g., someone called .interrupt())
                    if (command.deadline <= now || !isIntermediate) {
                        next(remainingWork = 0.0)
                    }
                }
                is SimResourceCommand.Consume -> {
                    val speed = min(resource.capacity, command.limit)
                    val isInterrupted = !isIntermediate && duration < getDuration(command.work, speed)
                    val remainingWork = getRemainingWork(command.work, speed, duration, isInterrupted)

                    // We should only continue processing the next command if:
                    // 1. The resource consumption was finished.
                    // 2. The resource consumer reached its deadline.
                    // 3. The resource consumer should be interrupted (e.g., someone called .interrupt())
                    if (remainingWork == 0.0 || command.deadline <= now || !isIntermediate) {
                        next(remainingWork)
                    } else {
                        interpret(SimResourceCommand.Consume(remainingWork, command.limit, command.deadline))
                    }
                }
                SimResourceCommand.Exit ->
                    // Flush may not be called when the resource consumer has finished
                    throw IllegalStateException()
            }
        } catch (e: Throwable) {
            onFailure(e)
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
     * The current command that is being processed.
     */
    private var activeCommand: CommandWrapper? = null

    /**
     * The latest timestamp at which the resource was flushed.
     */
    private var latestFlush: Long = Long.MIN_VALUE

    /**
     * Interpret the specified [SimResourceCommand] that was submitted by the resource consumer.
     */
    private fun interpret(command: SimResourceCommand) {
        val now = clock.millis()

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
                onFinish()
            }
        }

        assert(activeCommand == null) { "Concurrent access to current command" }
        activeCommand = CommandWrapper(now, command)
    }

    /**
     * Request the workload for more work.
     */
    private fun next(remainingWork: Double) {
        interpret(consumer.onNext(this, remainingWork))
    }

    /**
     * This class wraps a [command] with the timestamp it was started and possibly the task associated with it.
     */
    private data class CommandWrapper(val timestamp: Long, val command: SimResourceCommand)
}
