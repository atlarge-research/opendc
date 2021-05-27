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

import kotlin.math.max
import kotlin.math.min

/**
 * A [SimResourceDistributor] that distributes the capacity of a resource over consumers using max-min fair sharing.
 */
public class SimResourceDistributorMaxMin(
    override val input: SimResourceProvider,
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem? = null,
    private val listener: Listener? = null
) : SimResourceDistributor {
    override val outputs: Set<SimResourceProvider>
        get() = _outputs
    private val _outputs = mutableSetOf<Output>()

    /**
     * The active outputs.
     */
    private val activeOutputs: MutableList<Output> = mutableListOf()

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
     * The timestamp of the last report.
     */
    private var lastReport: Long = Long.MIN_VALUE

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

        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            return doNext(ctx.capacity)
        }

        override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
            when (event) {
                SimResourceEvent.Start -> {
                    this.ctx = ctx
                }
                SimResourceEvent.Exit -> {
                    val iterator = _outputs.iterator()
                    while (iterator.hasNext()) {
                        val output = iterator.next()

                        // Remove the output from the outputs to prevent ConcurrentModificationException when removing it
                        // during the call to output.close()
                        iterator.remove()

                        output.close()
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * The total amount of remaining work.
     */
    private val totalRemainingWork: Double
        get() = consumer.remainingWork

    init {
        input.startConsumer(consumer)
    }

    override fun addOutput(capacity: Double): SimResourceProvider {
        check(!isClosed) { "Distributor has been closed" }

        val provider = Output(capacity)
        _outputs.add(provider)
        return provider
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            input.cancel()
        }
    }

    /**
     * Schedule the work over the physical CPUs.
     */
    private fun doSchedule(capacity: Double): SimResourceCommand {
        // If there is no work yet, mark all inputs as idle.
        if (activeOutputs.isEmpty()) {
            return SimResourceCommand.Idle()
        }

        val maxUsage = capacity
        var duration: Double = Double.MAX_VALUE
        var deadline: Long = Long.MAX_VALUE
        var availableSpeed = maxUsage
        var totalRequestedSpeed = 0.0
        var totalRequestedWork = 0.0

        // Flush the work of the outputs
        var outputIterator = activeOutputs.listIterator()
        while (outputIterator.hasNext()) {
            val output = outputIterator.next()

            output.pull()

            if (output.isFinished) {
                // The output consumer has exited, so remove it from the scheduling queue.
                outputIterator.remove()
            }
        }

        // Sort the outputs based on their requested usage
        // Profiling shows that it is faster to sort every slice instead of maintaining some kind of sorted set
        activeOutputs.sort()

        // Divide the available input capacity fairly across the outputs using max-min fair sharing
        outputIterator = activeOutputs.listIterator()
        var remaining = activeOutputs.size
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

        assert(deadline >= interpreter.clock.millis()) { "Deadline already passed" }

        this.totalRequestedSpeed = totalRequestedSpeed
        this.totalRequestedWork = totalRequestedWork
        this.totalAllocatedSpeed = maxUsage - availableSpeed
        this.totalAllocatedWork = min(totalRequestedWork, totalAllocatedSpeed * min((deadline - interpreter.clock.millis()) / 1000.0, duration))

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
        val totalAllocatedWork = totalAllocatedWork.toLong()
        val totalRemainingWork = totalRemainingWork.toLong()
        val totalRequestedSpeed = totalRequestedSpeed
        val totalAllocatedSpeed = totalAllocatedSpeed

        // Force all inputs to re-schedule their work.
        val command = doSchedule(capacity)

        val now = interpreter.clock.millis()
        if (lastReport < now) {
            // Report metrics
            listener?.onSliceFinish(
                this,
                totalRequestedWork,
                totalAllocatedWork - totalRemainingWork,
                totalOvercommittedWork.toLong(),
                totalInterferedWork.toLong(),
                totalAllocatedSpeed,
                totalRequestedSpeed
            )
            lastReport = now
        }

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
    private inner class Output(capacity: Double) : SimAbstractResourceProvider(interpreter, parent, capacity), SimResourceProviderLogic, Comparable<Output> {
        /**
         * The current command that is processed by the resource.
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

        /**
         * A flag to indicate that the output is finished.
         */
        val isFinished
            get() = activeCommand is SimResourceCommand.Exit

        /**
         * The timestamp at which we received the last command.
         */
        private var lastCommandTimestamp: Long = Long.MIN_VALUE

        /* SimAbstractResourceProvider */
        override fun createLogic(): SimResourceProviderLogic = this

        override fun start(ctx: SimResourceControllableContext) {
            activeOutputs += this

            interpreter.batch {
                ctx.start()
                // Interrupt the input to re-schedule the resources
                input.interrupt()
            }
        }

        override fun close() {
            val state = state

            super.close()

            if (state != SimResourceState.Stopped) {
                _outputs.remove(this)
            }
        }

        /* SimResourceProviderLogic */
        override fun onIdle(ctx: SimResourceControllableContext, deadline: Long): Long {
            reportOvercommit(ctx.remainingWork)

            allowedSpeed = 0.0
            activeCommand = SimResourceCommand.Idle(deadline)
            lastCommandTimestamp = ctx.clock.millis()

            return Long.MAX_VALUE
        }

        override fun onConsume(ctx: SimResourceControllableContext, work: Double, limit: Double, deadline: Long): Long {
            reportOvercommit(ctx.remainingWork)

            allowedSpeed = ctx.speed
            activeCommand = SimResourceCommand.Consume(work, limit, deadline)
            lastCommandTimestamp = ctx.clock.millis()

            return Long.MAX_VALUE
        }

        override fun onFinish(ctx: SimResourceControllableContext) {
            reportOvercommit(ctx.remainingWork)

            activeCommand = SimResourceCommand.Exit
            lastCommandTimestamp = ctx.clock.millis()
        }

        override fun getRemainingWork(ctx: SimResourceControllableContext, work: Double, speed: Double, duration: Long): Double {
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

        /* Comparable */
        override fun compareTo(other: Output): Int = allowedSpeed.compareTo(other.allowedSpeed)

        /**
         * Pull the next command if necessary.
         */
        fun pull() {
            val ctx = ctx
            if (ctx != null && lastCommandTimestamp < ctx.clock.millis()) {
                ctx.flush()
            }
        }

        private fun reportOvercommit(remainingWork: Double) {
            totalOvercommittedWork += remainingWork
        }
    }
}
