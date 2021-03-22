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
import kotlin.math.max
import kotlin.math.min

/**
 * A [SimResourceDistributor] that distributes the capacity of a resource over consumers using max-min fair sharing.
 */
public class SimResourceDistributorMaxMin(
    override val input: SimResourceProvider,
    private val clock: Clock,
    private val listener: Listener? = null
) : SimResourceDistributor {
    override val outputs: Set<SimResourceProvider>
        get() = _outputs
    private val _outputs = mutableSetOf<OutputProvider>()

    /**
     * The active output contexts.
     */
    private val outputContexts: MutableList<OutputContext> = mutableListOf()

    /**
     * The total speed requested by the output resources.
     */
    private var totalRequestedSpeed = 0.0

    /**
     * The total amount of work requested by the output resources.
     */
    private var totalRequestedWork = 0.0

    /**
     * The total allocated speed for the output resources.
     */
    private var totalAllocatedSpeed = 0.0

    /**
     * The total allocated work requested for the output resources.
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
     * A flag to indicate that the switch is closed.
     */
    private var isClosed: Boolean = false

    /**
     * An internal [SimResourceConsumer] implementation for switch inputs.
     */
    private val consumer = object : SimResourceConsumer {
        /**
         * The resource context of the consumer.
         */
        private lateinit var ctx: SimResourceContext

        val remainingWork: Double
            get() = ctx.remainingWork

        override fun onStart(ctx: SimResourceContext) {
            this.ctx = ctx
        }

        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            return doNext(ctx.capacity)
        }

        override fun onFinish(ctx: SimResourceContext, cause: Throwable?) {
            super.onFinish(ctx, cause)

            val iterator = _outputs.iterator()
            while (iterator.hasNext()) {
                val output = iterator.next()

                // Remove the output from the outputs to prevent ConcurrentModificationException when removing it
                // during the call tou output.close()
                iterator.remove()

                output.close()
            }
        }
    }

    /**
     * The total amount of remaining work.
     */
    private val totalRemainingWork: Double
        get() = consumer.remainingWork

    override fun addOutput(capacity: Double): SimResourceProvider {
        check(!isClosed) { "Distributor has been closed" }

        val provider = OutputProvider(capacity)
        _outputs.add(provider)
        return provider
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            input.cancel()
        }
    }

    init {
        input.startConsumer(consumer)
    }

    /**
     * Indicate that the workloads should be re-scheduled.
     */
    private fun schedule() {
        input.interrupt()
    }

    /**
     * Schedule the work over the physical CPUs.
     */
    private fun doSchedule(capacity: Double): SimResourceCommand {
        // If there is no work yet, mark all inputs as idle.
        if (outputContexts.isEmpty()) {
            return SimResourceCommand.Idle()
        }

        val maxUsage = capacity
        var duration: Double = Double.MAX_VALUE
        var deadline: Long = Long.MAX_VALUE
        var availableSpeed = maxUsage
        var totalRequestedSpeed = 0.0
        var totalRequestedWork = 0.0

        // Flush the work of the outputs
        var outputIterator = outputContexts.listIterator()
        while (outputIterator.hasNext()) {
            val output = outputIterator.next()

            output.flush(isIntermediate = true)

            if (output.activeCommand == SimResourceCommand.Exit) {
                // Apparently the output consumer has exited, so remove it from the scheduling queue.
                outputIterator.remove()
            }
        }

        // Sort the outputs based on their requested usage
        // Profiling shows that it is faster to sort every slice instead of maintaining some kind of sorted set
        outputContexts.sort()

        // Divide the available input capacity fairly across the outputs using max-min fair sharing
        outputIterator = outputContexts.listIterator()
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
                SimResourceCommand.Exit -> assert(false) { "Did not expect output to be stopped" }
            }
        }

        assert(deadline >= clock.millis()) { "Deadline already passed" }

        this.totalRequestedSpeed = totalRequestedSpeed
        this.totalRequestedWork = totalRequestedWork
        this.totalAllocatedSpeed = maxUsage - availableSpeed
        this.totalAllocatedWork = min(totalRequestedWork, totalAllocatedSpeed * duration)

        return if (totalAllocatedWork > 0.0 && totalAllocatedSpeed > 0.0)
            SimResourceCommand.Consume(totalAllocatedWork, totalAllocatedSpeed, deadline)
        else
            SimResourceCommand.Idle(deadline)
    }

    /**
     * Obtain the next command to perform.
     */
    private fun doNext(capacity: Double): SimResourceCommand {
        val totalRequestedWork = totalRequestedWork.toLong()
        val totalRemainingWork = totalRemainingWork.toLong()
        val totalAllocatedWork = totalAllocatedWork.toLong()
        val totalRequestedSpeed = totalRequestedSpeed
        val totalAllocatedSpeed = totalAllocatedSpeed

        // Force all inputs to re-schedule their work.
        val command = doSchedule(capacity)

        // Report metrics
        listener?.onSliceFinish(
            this,
            totalRequestedWork,
            totalAllocatedWork - totalRemainingWork,
            totalOvercommittedWork.toLong(),
            totalInterferedWork.toLong(),
            totalRequestedSpeed,
            totalAllocatedSpeed,
        )

        totalInterferedWork = 0.0
        totalOvercommittedWork = 0.0

        return command
    }

    /**
     * Event listener for hypervisor events.
     */
    public interface Listener {
        /**
         * This method is invoked when a slice is finished.
         */
        public fun onSliceFinish(
            switch: SimResourceDistributor,
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
    private inner class OutputProvider(val capacity: Double) : SimResourceProvider {
        /**
         * The [OutputContext] that is currently running.
         */
        private var ctx: OutputContext? = null

        override var state: SimResourceState = SimResourceState.Pending
            internal set

        override fun startConsumer(consumer: SimResourceConsumer) {
            check(state == SimResourceState.Pending) { "Resource cannot be consumed" }

            val ctx = OutputContext(this, consumer)
            this.ctx = ctx
            this.state = SimResourceState.Active
            outputContexts += ctx

            ctx.start()
            schedule()
        }

        override fun close() {
            cancel()

            if (state != SimResourceState.Stopped) {
                state = SimResourceState.Stopped
                _outputs.remove(this)
            }
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
        consumer: SimResourceConsumer
    ) : SimAbstractResourceContext(clock, consumer), Comparable<OutputContext> {
        override val capacity: Double
            get() = provider.capacity

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
            val remainingWork = remainingWork
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
}
