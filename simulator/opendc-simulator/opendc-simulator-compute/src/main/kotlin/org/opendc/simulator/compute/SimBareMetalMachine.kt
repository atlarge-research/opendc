/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.simulator.compute

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimResourceCommand
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.utils.TimerScheduler
import java.time.Clock
import java.util.*
import kotlin.coroutines.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A simulated bare-metal machine that is able to run a single workload.
 *
 * A [SimBareMetalMachine] is a stateful object and you should be careful when operating this object concurrently. For
 * example. the class expects only a single concurrent call to [run].
 *
 * @param coroutineScope The [CoroutineScope] to run the simulated workload in.
 * @param clock The virtual clock to track the simulation time.
 * @param model The machine model to simulate.
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
public class SimBareMetalMachine(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
    override val model: SimMachineModel
) : SimMachine {
    /**
     * A [StateFlow] representing the CPU usage of the simulated machine.
     */
    override val usage: StateFlow<Double>
        get() = usageState

    /**
     *  A flag to indicate that the machine is terminated.
     */
    private var isTerminated = false

    /**
     * The [MutableStateFlow] containing the load of the server.
     */
    private val usageState = MutableStateFlow(0.0)

    /**
     * The current active workload.
     */
    private var cont: Continuation<Unit>? = null

    /**
     * The active CPUs of this machine.
     */
    private var cpus: List<Cpu> = emptyList()

    /**
     * The [TimerScheduler] to use for scheduling the interrupts.
     */
    private val scheduler = TimerScheduler<Cpu>(coroutineScope, clock)

    /**
     * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload, meta: Map<String, Any>) {
        require(!isTerminated) { "Machine is terminated" }
        require(cont == null) { "Run should not be called concurrently" }

        val ctx = object : SimExecutionContext {
            override val machine: SimMachineModel
                get() = this@SimBareMetalMachine.model

            override val clock: Clock
                get() = this@SimBareMetalMachine.clock

            override val meta: Map<String, Any>
                get() = meta

            override fun interrupt(cpu: Int) {
                require(cpu < cpus.size) { "Invalid CPU identifier" }
                cpus[cpu].interrupt()
            }
        }

        workload.onStart(ctx)

        return suspendCancellableCoroutine { cont ->
            this.cont = cont
            this.cpus = model.cpus.map { Cpu(ctx, it, workload) }

            for (cpu in cpus) {
                cpu.start()
            }
        }
    }

    /**
     * Terminate the specified bare-metal machine.
     */
    override fun close() {
        isTerminated = true
    }

    /**
     * Update the usage of the machine.
     */
    private fun updateUsage() {
        usageState.value = cpus.sumByDouble { it.speed } / cpus.sumByDouble { it.model.frequency }
    }

    /**
     * This method is invoked when one of the CPUs has exited.
     */
    private fun onCpuExit(cpu: Int) {
        // Check whether all other CPUs have finished
        if (cpus.all { it.hasExited }) {
            val cont = cont
            this.cont = null
            cont?.resume(Unit)
        }
    }

    /**
     * This method is invoked when one of the CPUs failed.
     */
    private fun onCpuFailure(e: Throwable) {
        // Make sure no other tasks will be resumed.
        scheduler.cancelAll()

        // In case the flush fails with an exception, immediately propagate to caller, cancelling all other
        // tasks.
        val cont = cont
        this.cont = null
        cont?.resumeWithException(e)
    }

    /**
     * A physical CPU of the machine.
     */
    private inner class Cpu(val ctx: SimExecutionContext, val model: ProcessingUnit, val workload: SimWorkload) {
        /**
         * The current command.
         */
        private var currentCommand: CommandWrapper? = null

        /**
         * The actual processing speed.
         */
        var speed: Double = 0.0
            set(value) {
                field = value
                updateUsage()
            }

        /**
         * A flag to indicate that the CPU is currently processing a command.
         */
        var isIntermediate: Boolean = false

        /**
         * A flag to indicate that the CPU has exited.
         */
        var hasExited: Boolean = false

        /**
         * Process the specified [SimResourceCommand] for this CPU.
         */
        fun process(command: SimResourceCommand) {
            val timestamp = clock.millis()

            val task = when (command) {
                is SimResourceCommand.Idle -> {
                    speed = 0.0

                    val deadline = command.deadline

                    require(deadline >= timestamp) { "Deadline already passed" }

                    if (deadline != Long.MAX_VALUE) {
                        scheduler.startSingleTimerTo(this, deadline) { flush() }
                    } else {
                        null
                    }
                }
                is SimResourceCommand.Consume -> {
                    val work = command.work
                    val limit = command.limit
                    val deadline = command.deadline

                    require(deadline >= timestamp) { "Deadline already passed" }

                    speed = min(model.frequency, limit)

                    // The required duration to process all the work
                    val finishedAt = timestamp + ceil(work / speed * 1000).toLong()

                    scheduler.startSingleTimerTo(this, min(finishedAt, deadline)) { flush() }
                }
                is SimResourceCommand.Exit -> {
                    speed = 0.0
                    hasExited = true

                    onCpuExit(model.id)

                    null
                }
            }

            assert(currentCommand == null) { "Concurrent access to current command" }
            currentCommand = CommandWrapper(timestamp, command)
        }

        /**
         * Request the workload for more work.
         */
        private fun next(remainingWork: Double) {
            process(workload.onNext(ctx, model.id, remainingWork))
        }

        /**
         * Start the CPU.
         */
        fun start() {
            try {
                isIntermediate = true

                process(workload.onStart(ctx, model.id))
            } catch (e: Throwable) {
                onCpuFailure(e)
            } finally {
                isIntermediate = false
            }
        }

        /**
         * Flush the work performed by the CPU.
         */
        fun flush() {
            try {
                val (timestamp, command) = currentCommand ?: return

                isIntermediate = true
                currentCommand = null

                // Cancel the running task and flush the progress
                scheduler.cancel(this)

                when (command) {
                    is SimResourceCommand.Idle -> next(remainingWork = 0.0)
                    is SimResourceCommand.Consume -> {
                        val duration = clock.millis() - timestamp
                        val remainingWork = if (duration > 0L) {
                            val processed = duration / 1000.0 * speed
                            max(0.0, command.work - processed)
                        } else {
                            0.0
                        }

                        next(remainingWork)
                    }
                    SimResourceCommand.Exit -> throw IllegalStateException()
                }
            } catch (e: Throwable) {
                onCpuFailure(e)
            } finally {
                isIntermediate = false
            }
        }

        /**
         * Interrupt the CPU.
         */
        fun interrupt() {
            // Prevent users from interrupting the CPU while it is constructing its next command, this will only lead
            // to infinite recursion.
            if (isIntermediate) {
                return
            }

            flush()
        }
    }

    /**
     * This class wraps a [command] with the timestamp it was started and possibly the task associated with it.
     */
    private data class CommandWrapper(val timestamp: Long, val command: SimResourceCommand)
}
