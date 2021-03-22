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

import kotlinx.coroutines.*
import org.opendc.simulator.resources.consumer.SimConsumerBarrier
import java.time.Clock
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A [SimResourceSwitch] implementation that switches resource consumptions over the available resources using max-min
 * fair sharing.
 */
public class SimResourceSwitchMaxMin<R : SimResource>(
    private val clock: Clock,
    private val listener: Listener<R>? = null
) : SimResourceSwitch<R> {
    private val inputConsumers = mutableSetOf<InputConsumer>()
    private val _outputs = mutableSetOf<OutputProvider>()
    override val outputs: Set<SimResourceProvider<R>>
        get() = _outputs

    private val _inputs = mutableSetOf<SimResourceProvider<R>>()
    override val inputs: Set<SimResourceProvider<R>>
        get() = _inputs

    /**
     * The commands to submit to the underlying host.
     */
    private val commands = mutableMapOf<R, SimResourceCommand>()

    /**
     * The active output contexts.
     */
    private val outputContexts: MutableList<OutputContext> = mutableListOf()

    /**
     * The remaining work of all inputs.
     */
    private val totalRemainingWork: Double
        get() = inputConsumers.sumByDouble { it.remainingWork }

    /**
     * The total speed requested by the vCPUs.
     */
    private var totalRequestedSpeed = 0.0

    /**
     * The total amount of work requested by the vCPUs.
     */
    private var totalRequestedWork = 0.0

    /**
     * The total allocated speed for the vCPUs.
     */
    private var totalAllocatedSpeed = 0.0

    /**
     * The total allocated work requested for the vCPUs.
     */
    private var totalAllocatedWork = 0.0

    /**
     * The amount of work that could not be performed due to over-committing resources.
     */
    private var totalOvercommittedWork = 0.0

    /**
     * The amount of work that was lost due to interference.
     */
    private var totalInterferedWork = 0.0

    /**
     * A flag to indicate that the scheduler has submitted work that has not yet been completed.
     */
    private var isDirty: Boolean = false

    /**
     * The scheduler barrier.
     */
    private var barrier: SimConsumerBarrier = SimConsumerBarrier(0)

    /**
     * A flag to indicate that the switch is closed.
     */
    private var isClosed: Boolean = false

    /**
     * Add an output to the switch represented by [resource].
     */
    override fun addOutput(resource: R): SimResourceProvider<R> {
        check(!isClosed) { "Switch has been closed" }

        val provider = OutputProvider(resource)
        _outputs.add(provider)
        return provider
    }

    /**
     * Add the specified [input] to the switch.
     */
    override fun addInput(input: SimResourceProvider<R>) {
        check(!isClosed) { "Switch has been closed" }

        val consumer = InputConsumer(input)
        _inputs.add(input)
        inputConsumers += consumer
    }

    override fun close() {
        isClosed = true
    }

    /**
     * Indicate that the workloads should be re-scheduled.
     */
    private fun schedule() {
        isDirty = true
        interruptAll()
    }

    /**
     * Schedule the work over the physical CPUs.
     */
    private fun doSchedule() {
        // If there is no work yet, mark all inputs as idle.
        if (outputContexts.isEmpty()) {
            commands.replaceAll { _, _ -> SimResourceCommand.Idle() }
            interruptAll()
        }

        val maxUsage = inputs.sumByDouble { it.resource.capacity }
        var duration: Double = Double.MAX_VALUE
        var deadline: Long = Long.MAX_VALUE
        var availableSpeed = maxUsage
        var totalRequestedSpeed = 0.0
        var totalRequestedWork = 0.0

        // Sort the outputs based on their requested usage
        // Profiling shows that it is faster to sort every slice instead of maintaining some kind of sorted set
        outputContexts.sort()

        // Divide the available input capacity fairly across the outputs using max-min fair sharing
        val outputIterator = outputContexts.listIterator()
        var remaining = outputContexts.size
        while (outputIterator.hasNext()) {
            val output = outputIterator.next()
            val availableShare = availableSpeed / remaining--

            when (val command = output.activeCommand) {
                is SimResourceCommand.Idle -> {
                    // Take into account the minimum deadline of this slice before we possible continue
                    deadline = min(deadline, command.deadline)

                    output.actualSpeed = 0.0
                }
                is SimResourceCommand.Consume -> {
                    val grantedSpeed = min(output.allowedSpeed, availableShare)

                    // Take into account the minimum deadline of this slice before we possible continue
                    deadline = min(deadline, command.deadline)

                    // Ignore idle computation
                    if (grantedSpeed <= 0.0 || command.work <= 0.0) {
                        output.actualSpeed = 0.0
                        continue
                    }

                    totalRequestedSpeed += command.limit
                    totalRequestedWork += command.work

                    output.actualSpeed = grantedSpeed
                    availableSpeed -= grantedSpeed

                    // The duration that we want to run is that of the shortest request from an output
                    duration = min(duration, command.work / grantedSpeed)
                }
                SimResourceCommand.Exit -> {
                    // Apparently the output consumer has exited, so remove it from the scheduling queue.
                    outputIterator.remove()
                }
            }
        }

        // Round the duration to milliseconds
        duration = ceil(duration * 1000) / 1000

        assert(deadline >= clock.millis()) { "Deadline already passed" }

        val totalAllocatedSpeed = maxUsage - availableSpeed
        var totalAllocatedWork = 0.0
        availableSpeed = totalAllocatedSpeed

        // Divide the requests over the available capacity of the input resources fairly
        for (input in inputs.sortedByDescending { it.resource.capacity }) {
            val maxResourceUsage = input.resource.capacity
            val fraction = maxResourceUsage / maxUsage
            val grantedSpeed = min(maxResourceUsage, totalAllocatedSpeed * fraction)
            val grantedWork = duration * grantedSpeed

            commands[input.resource] =
                if (grantedWork > 0.0 && grantedSpeed > 0.0)
                    SimResourceCommand.Consume(grantedWork, grantedSpeed, deadline)
                else
                    SimResourceCommand.Idle(deadline)

            totalAllocatedWork += grantedWork
            availableSpeed -= grantedSpeed
        }

        this.totalRequestedSpeed = totalRequestedSpeed
        this.totalRequestedWork = totalRequestedWork
        this.totalAllocatedSpeed = totalAllocatedSpeed
        this.totalAllocatedWork = totalAllocatedWork

        interruptAll()
    }

    /**
     * Flush the progress of the vCPUs.
     */
    private fun flushGuests() {
        val totalRemainingWork = totalRemainingWork

        // Flush all the outputs work
        for (output in outputContexts) {
            output.flush(isIntermediate = true)
        }

        // Report metrics
        listener?.onSliceFinish(
            this,
            totalRequestedWork.toLong(),
            (totalAllocatedWork - totalRemainingWork).toLong(),
            totalOvercommittedWork.toLong(),
            totalInterferedWork.toLong(),
            totalRequestedSpeed,
            totalAllocatedSpeed
        )
        totalInterferedWork = 0.0
        totalOvercommittedWork = 0.0

        // Force all inputs to re-schedule their work.
        doSchedule()
    }

    /**
     * Interrupt all inputs.
     */
    private fun interruptAll() {
        for (input in inputConsumers) {
            input.interrupt()
        }
    }

    /**
     * Event listener for hypervisor events.
     */
    public interface Listener<R : SimResource> {
        /**
         * This method is invoked when a slice is finished.
         */
        public fun onSliceFinish(
            switch: SimResourceSwitchMaxMin<R>,
            requestedWork: Long,
            grantedWork: Long,
            overcommittedWork: Long,
            interferedWork: Long,
            cpuUsage: Double,
            cpuDemand: Double
        )
    }

    /**
     * An internal [SimResourceProvider] implementation for switch outputs.
     */
    private inner class OutputProvider(override val resource: R) : SimResourceProvider<R> {
        /**
         * The [OutputContext] that is currently running.
         */
        private var ctx: OutputContext? = null

        override var state: SimResourceState = SimResourceState.Pending
            internal set

        override fun startConsumer(consumer: SimResourceConsumer<R>) {
            check(state == SimResourceState.Pending) { "Resource cannot be consumed" }

            val ctx = OutputContext(this, resource, consumer)
            this.ctx = ctx
            this.state = SimResourceState.Active
            outputContexts += ctx

            ctx.start()
            schedule()
        }

        override fun close() {
            cancel()

            state = SimResourceState.Stopped
            _outputs.remove(this)
        }

        override fun interrupt() {
            ctx?.interrupt()
        }

        override fun cancel() {
            val ctx = ctx
            if (ctx != null) {
                this.ctx = null
                ctx.stop()
            }

            if (state != SimResourceState.Stopped) {
                state = SimResourceState.Pending
            }
        }
    }

    /**
     * A [SimAbstractResourceContext] for the output resources.
     */
    private inner class OutputContext(
        private val provider: OutputProvider,
        resource: R,
        consumer: SimResourceConsumer<R>
    ) : SimAbstractResourceContext<R>(resource, clock, consumer), Comparable<OutputContext> {
        /**
         * The current command that is processed by the vCPU.
         */
        var activeCommand: SimResourceCommand = SimResourceCommand.Idle()

        /**
         * The processing speed that is allowed by the model constraints.
         */
        var allowedSpeed: Double = 0.0

        /**
         * The actual processing speed.
         */
        var actualSpeed: Double = 0.0

        private fun reportOvercommit() {
            totalOvercommittedWork += remainingWork
        }

        override fun onIdle(deadline: Long) {
            reportOvercommit()

            allowedSpeed = 0.0
            activeCommand = SimResourceCommand.Idle(deadline)
        }

        override fun onConsume(work: Double, limit: Double, deadline: Long) {
            reportOvercommit()

            allowedSpeed = getSpeed(limit)
            activeCommand = SimResourceCommand.Consume(work, limit, deadline)
        }

        override fun onFinish(cause: Throwable?) {
            reportOvercommit()

            activeCommand = SimResourceCommand.Exit
            provider.cancel()

            super.onFinish(cause)
        }

        override fun getRemainingWork(work: Double, speed: Double, duration: Long): Double {
            // Apply performance interference model
            val performanceScore = 1.0

            // Compute the remaining amount of work
            return if (work > 0.0) {
                // Compute the fraction of compute time allocated to the VM
                val fraction = actualSpeed / totalAllocatedSpeed

                // Compute the work that was actually granted to the VM.
                val processingAvailable = max(0.0, totalAllocatedWork - totalRemainingWork) * fraction
                val processed = processingAvailable * performanceScore

                val interferedWork = processingAvailable - processed

                totalInterferedWork += interferedWork

                max(0.0, work - processed)
            } else {
                0.0
            }
        }

        override fun interrupt() {
            // Prevent users from interrupting the CPU while it is constructing its next command, this will only lead
            // to infinite recursion.
            if (isProcessing) {
                return
            }

            super.interrupt()

            // Force the scheduler to re-schedule
            schedule()
        }

        override fun compareTo(other: OutputContext): Int = allowedSpeed.compareTo(other.allowedSpeed)
    }

    /**
     * An internal [SimResourceConsumer] implementation for switch inputs.
     */
    private inner class InputConsumer(val input: SimResourceProvider<R>) : SimResourceConsumer<R> {
        /**
         * The resource context of the consumer.
         */
        private lateinit var ctx: SimResourceContext<R>

        /**
         * The remaining work of this consumer.
         */
        val remainingWork: Double
            get() = ctx.remainingWork

        init {
            barrier = SimConsumerBarrier(barrier.parties + 1)
            input.startConsumer(this@InputConsumer)
        }

        /**
         * Interrupt the consumer
         */
        fun interrupt() {
            ctx.interrupt()
        }

        override fun onStart(ctx: SimResourceContext<R>) {
            this.ctx = ctx
        }

        override fun onNext(ctx: SimResourceContext<R>): SimResourceCommand {
            val isLast = barrier.enter()

            // Flush the progress of the guest after the barrier has been reached.
            if (isLast && isDirty) {
                isDirty = false
                flushGuests()
            }

            return if (isDirty) {
                // Wait for the scheduler determine the work after the barrier has been reached by all CPUs.
                SimResourceCommand.Idle()
            } else {
                // Indicate that the scheduler needs to run next call.
                if (isLast) {
                    isDirty = true
                }

                commands[ctx.resource] ?: SimResourceCommand.Idle()
            }
        }

        override fun onFinish(ctx: SimResourceContext<R>, cause: Throwable?) {
            barrier = SimConsumerBarrier(barrier.parties - 1)
            inputConsumers -= this@InputConsumer
            _inputs -= input

            super.onFinish(ctx, cause)
        }
    }
}
