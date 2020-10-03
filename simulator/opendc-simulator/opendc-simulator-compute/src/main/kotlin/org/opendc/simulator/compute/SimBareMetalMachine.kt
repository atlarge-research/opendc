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
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectInstance
import org.opendc.simulator.compute.workload.SimWorkload
import java.lang.Runnable
import java.time.Clock
import kotlin.coroutines.ContinuationInterceptor
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
     * The current active workload.
     */
    private var activeWorkload: SimWorkload? = null

    /**
     * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload) {
        require(activeWorkload == null) { "Run should not be called concurrently" }

        try {
            activeWorkload = workload
            workload.run(ctx)
        } finally {
            activeWorkload = null
        }
    }

    /**
     * The execution context in which the workload runs.
     */
    private val ctx = object : SimExecutionContext {
        override val machine: SimMachineModel
            get() = this@SimBareMetalMachine.model

        override val clock: Clock
            get() = this@SimBareMetalMachine.clock

        override fun onRun(
            batch: Sequence<SimExecutionContext.Slice>,
            triggerMode: SimExecutionContext.TriggerMode,
            merge: (SimExecutionContext.Slice, SimExecutionContext.Slice) -> SimExecutionContext.Slice
        ): SelectClause0 {
            return object : SelectClause0 {
                @InternalCoroutinesApi
                override fun <R> registerSelectClause0(select: SelectInstance<R>, block: suspend () -> R) {
                    // Do not reset the usage state: we will set it ourselves
                    usageFlush?.dispose()
                    usageFlush = null

                    val queue = batch.iterator()
                    var start = Long.MIN_VALUE
                    var currentWork: SliceWork? = null
                    var currentDisposable: DisposableHandle? = null

                    fun schedule(slice: SimExecutionContext.Slice) {
                        start = clock.millis()

                        val isLastSlice = !queue.hasNext()
                        val work = SliceWork(slice)
                        val candidateDuration = when (triggerMode) {
                            SimExecutionContext.TriggerMode.FIRST -> work.minExit
                            SimExecutionContext.TriggerMode.LAST -> work.maxExit
                            SimExecutionContext.TriggerMode.DEADLINE -> slice.deadline - start
                        }

                        // Check whether the deadline is exceeded during the run of the slice.
                        val duration = min(candidateDuration, slice.deadline - start)

                        val action = Runnable {
                            currentWork = null

                            // Flush all the work that was performed
                            val hasFinished = work.stop(duration)

                            if (!isLastSlice) {
                                val candidateSlice = queue.next()
                                val nextSlice =
                                    // If our previous slice exceeds its deadline, merge it with the next candidate slice
                                    if (hasFinished)
                                        candidateSlice
                                    else
                                        merge(candidateSlice, slice)
                                schedule(nextSlice)
                            } else if (select.trySelect()) {
                                block.startCoroutineCancellable(select.completion)
                            }
                        }

                        // Schedule the flush after the entire slice has finished
                        currentDisposable = delay.invokeOnTimeout(duration, action)

                        // Start the slice work
                        currentWork = work
                        work.start()
                    }

                    // Schedule the first work
                    if (queue.hasNext()) {
                        schedule(queue.next())

                        // A DisposableHandle to flush the work in case the call is cancelled
                        val disposable = DisposableHandle {
                            val end = clock.millis()
                            val duration = end - start

                            currentWork?.stop(duration)
                            currentDisposable?.dispose()

                            // Schedule reset the usage of the machine since the call is returning
                            usageFlush = delay.invokeOnTimeout(1) {
                                usageState.value = 0.0
                                usageFlush = null
                            }
                        }

                        select.disposeOnSelect(disposable)
                    } else if (select.trySelect()) {
                        // No work has been given: select immediately
                        block.startCoroutineCancellable(select.completion)
                    }
                }
            }
        }
    }

    /**
     * The [MutableStateFlow] containing the load of the server.
     */
    private val usageState = MutableStateFlow(0.0)

    /**
     * A disposable to prevent resetting the usage state for subsequent calls to onRun.
     */
    private var usageFlush: DisposableHandle? = null

    /**
     * Cache the [Delay] instance for timing.
     *
     * XXX We need to cache this before the call to [onRun] since doing this in [onRun] is too heavy.
     * XXX Note however that this is an ugly hack which may break in the future.
     */
    @OptIn(InternalCoroutinesApi::class)
    private val delay = coroutineScope.coroutineContext[ContinuationInterceptor] as Delay

    /**
     * A slice to be processed.
     */
    private inner class SliceWork(val slice: SimExecutionContext.Slice) {
        /**
         * The duration after which the first processor finishes processing this slice.
         */
        val minExit: Long

        /**
         * The duration after which the last processor finishes processing this slice.
         */
        val maxExit: Long

        /**
         * A flag to indicate that the slice will exceed the deadline.
         */
        val exceedsDeadline: Boolean
            get() = slice.deadline < maxExit

        /**
         * The total amount of CPU usage.
         */
        val totalUsage: Double

        /**
         * A flag to indicate that this slice is empty.
         */
        val isEmpty: Boolean

        init {
            var totalUsage = 0.0
            var minExit = Long.MAX_VALUE
            var maxExit = 0L
            var nonEmpty = false

            // Determine the duration of the first/last CPU to finish
            for (i in 0 until min(model.cpus.size, slice.burst.size)) {
                val cpu = model.cpus[i]
                val usage = min(slice.limit[i], cpu.frequency)
                val cpuDuration = ceil(slice.burst[i] / usage * 1000).toLong() // Convert from seconds to milliseconds

                totalUsage += usage / cpu.frequency

                if (cpuDuration != 0L) { // We only wait for processor cores with a non-zero burst
                    minExit = min(minExit, cpuDuration)
                    maxExit = max(maxExit, cpuDuration)
                    nonEmpty = true
                }
            }

            this.isEmpty = !nonEmpty
            this.totalUsage = totalUsage
            this.minExit = minExit
            this.maxExit = maxExit
        }

        /**
         * Indicate that the work on the slice has started.
         */
        fun start() {
            usageState.value = totalUsage / model.cpus.size
        }

        /**
         * Flush the work performed on the slice.
         */
        fun stop(duration: Long): Boolean {
            var hasFinished = true

            for (i in 0 until min(model.cpus.size, slice.burst.size)) {
                val usage = min(slice.limit[i], model.cpus[i].frequency)
                val granted = ceil(duration / 1000.0 * usage).toLong()
                val res = max(0, slice.burst[i] - granted)
                slice.burst[i] = res

                if (res != 0L) {
                    hasFinished = false
                }
            }

            return hasFinished
        }
    }
}
