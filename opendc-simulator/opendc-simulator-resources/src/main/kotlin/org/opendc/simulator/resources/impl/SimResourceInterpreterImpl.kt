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

import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import org.opendc.simulator.resources.*
import java.time.Clock
import java.util.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * A [SimResourceInterpreter] queues all interrupts that occur during execution to be executed after.
 *
 * @param context The coroutine context to use.
 * @param clock The virtual simulation clock.
 */
internal class SimResourceInterpreterImpl(private val context: CoroutineContext, override val clock: Clock) : SimResourceInterpreter {
    /**
     * The [Delay] instance that provides scheduled execution of [Runnable]s.
     */
    @OptIn(InternalCoroutinesApi::class)
    private val delay = requireNotNull(context[ContinuationInterceptor] as? Delay) { "Invalid CoroutineDispatcher: no delay implementation" }

    /**
     * The queue of resource updates that are scheduled for immediate execution.
     */
    private val queue = ArrayDeque<SimResourceContextImpl>()

    /**
     * A priority queue containing the resource updates to be scheduled in the future.
     */
    private val futureQueue = PriorityQueue<Timer>()

    /**
     * The stack of interpreter invocations to occur in the future.
     */
    private val futureInvocations = ArrayDeque<Invocation>()

    /**
     * The systems that have been visited during the interpreter cycle.
     */
    private val visited = linkedSetOf<SimResourceSystem>()

    /**
     * The index in the batch stack.
     */
    private var batchIndex = 0

    /**
     * A flag to indicate that the interpreter is currently active.
     */
    private val isRunning: Boolean
        get() = batchIndex > 0

    /**
     * Update the specified [ctx] synchronously.
     */
    fun scheduleSync(now: Long, ctx: SimResourceContextImpl) {
        ctx.doUpdate(now)

        if (visited.add(ctx)) {
            collectAncestors(ctx, visited)
        }

        // In-case the interpreter is already running in the call-stack, return immediately. The changes will be picked
        // up by the active interpreter.
        if (isRunning) {
            return
        }

        try {
            batchIndex++
            runInterpreter(now)
        } finally {
            batchIndex--
        }
    }

    /**
     * Enqueue the specified [ctx] to be updated immediately during the active interpreter cycle.
     *
     * This method should be used when the state of a resource context is invalidated/interrupted and needs to be
     * re-computed. In case no interpreter is currently active, the interpreter will be started.
     */
    fun scheduleImmediate(now: Long, ctx: SimResourceContextImpl) {
        queue.add(ctx)

        // In-case the interpreter is already running in the call-stack, return immediately. The changes will be picked
        // up by the active interpreter.
        if (isRunning) {
            return
        }

        try {
            batchIndex++
            runInterpreter(now)
        } finally {
            batchIndex--
        }
    }

    /**
     * Schedule the interpreter to run at [target] to update the resource contexts.
     *
     * This method will override earlier calls to this method for the same [ctx].
     *
     * @param now The current virtual timestamp.
     * @param ctx The resource context to which the event applies.
     * @param target The timestamp when the interrupt should happen.
     */
    fun scheduleDelayed(now: Long, ctx: SimResourceContextImpl, target: Long): Timer {
        val futureQueue = futureQueue

        require(target >= now) { "Timestamp must be in the future" }

        val timer = Timer(ctx, target)
        futureQueue.add(timer)

        return timer
    }

    override fun newContext(
        consumer: SimResourceConsumer,
        provider: SimResourceProviderLogic,
        parent: SimResourceSystem?
    ): SimResourceControllableContext = SimResourceContextImpl(parent, this, consumer, provider)

    override fun pushBatch() {
        batchIndex++
    }

    override fun popBatch() {
        try {
            // Flush the work if the platform is not already running
            if (batchIndex == 1 && queue.isNotEmpty()) {
                runInterpreter(clock.millis())
            }
        } finally {
            batchIndex--
        }
    }

    /**
     * Interpret all actions that are scheduled for the current timestamp.
     */
    private fun runInterpreter(now: Long) {
        val queue = queue
        val futureQueue = futureQueue
        val futureInvocations = futureInvocations
        val visited = visited

        // Remove any entries in the `futureInvocations` queue from the past
        while (true) {
            val head = futureInvocations.peek()
            if (head == null || head.timestamp > now) {
                break
            }
            futureInvocations.poll()
        }

        // Execute all scheduled updates at current timestamp
        while (true) {
            val timer = futureQueue.peek() ?: break
            val ctx = timer.ctx
            val target = timer.target

            assert(target >= now) { "Internal inconsistency: found update of the past" }

            if (target > now) {
                break
            }

            futureQueue.poll()

            ctx.pruneTimers(now)

            if (ctx.shouldUpdate(now)) {
                ctx.doUpdate(now)

                if (visited.add(ctx)) {
                    collectAncestors(ctx, visited)
                }
            } else {
                ctx.tryReschedule(now)
            }
        }

        // Repeat execution of all immediate updates until the system has converged to a steady-state
        // We have to take into account that the onConverge callback can also trigger new actions.
        do {
            // Execute all immediate updates
            while (true) {
                val ctx = queue.poll() ?: break

                if (ctx.shouldUpdate(now)) {
                    ctx.doUpdate(now)

                    if (visited.add(ctx)) {
                        collectAncestors(ctx, visited)
                    }
                }
            }

            for (system in visited) {
                system.onConverge(now)
            }

            visited.clear()
        } while (queue.isNotEmpty())

        // Schedule an interpreter invocation for the next update to occur.
        val headTimer = futureQueue.peek()
        if (headTimer != null) {
            trySchedule(now, futureInvocations, headTimer.target)
        }
    }

    /**
     * Collect all the ancestors of the specified [system].
     */
    private tailrec fun collectAncestors(system: SimResourceSystem, systems: MutableSet<SimResourceSystem>) {
        val parent = system.parent
        if (parent != null) {
            systems.add(parent)
            collectAncestors(parent, systems)
        }
    }

    /**
     * Try to schedule an interpreter invocation at the specified [target].
     *
     * @param now The current virtual timestamp.
     * @param target The virtual timestamp at which the interpreter invocation should happen.
     * @param scheduled The queue of scheduled invocations.
     */
    private fun trySchedule(now: Long, scheduled: ArrayDeque<Invocation>, target: Long) {
        while (true) {
            val invocation = scheduled.peekFirst()
            if (invocation == null || invocation.timestamp > target) {
                // Case 2: A new timer was registered ahead of the other timers.
                // Solution: Schedule a new scheduler invocation
                @OptIn(InternalCoroutinesApi::class)
                val handle = delay.invokeOnTimeout(
                    target - now,
                    {
                        try {
                            batchIndex++
                            runInterpreter(target)
                        } finally {
                            batchIndex--
                        }
                    },
                    context
                )
                scheduled.addFirst(Invocation(target, handle))
                break
            } else if (invocation.timestamp < target) {
                // Case 2: A timer was cancelled and the head of the timer queue is now later than excepted
                // Solution: Cancel the next scheduler invocation
                scheduled.pollFirst()

                invocation.cancel()
            } else {
                break
            }
        }
    }

    /**
     * A future interpreter invocation.
     *
     * This class is used to keep track of the future scheduler invocations created using the [Delay] instance. In case
     * the invocation is not needed anymore, it can be cancelled via [cancel].
     */
    private data class Invocation(
        @JvmField val timestamp: Long,
        @JvmField val handle: DisposableHandle
    ) {
        /**
         * Cancel the interpreter invocation.
         */
        fun cancel() = handle.dispose()
    }

    /**
     * An update call for [ctx] that is scheduled for [target].
     *
     * This class represents an update in the future at [target] requested by [ctx]. A deferred update might be
     * cancelled if the resource context was invalidated in the meantime.
     */
    class Timer(@JvmField val ctx: SimResourceContextImpl, @JvmField val target: Long) : Comparable<Timer> {
        override fun compareTo(other: Timer): Int {
            return target.compareTo(other.target)
        }

        override fun toString(): String = "Timer[ctx=$ctx,timestamp=$target]"
    }
}
