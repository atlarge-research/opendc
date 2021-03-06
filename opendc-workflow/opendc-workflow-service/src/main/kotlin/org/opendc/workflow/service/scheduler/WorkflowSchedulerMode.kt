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

package org.opendc.workflow.service.scheduler

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opendc.workflow.service.internal.WorkflowServiceImpl

/**
 * The operating mode of a workflow scheduler.
 */
public sealed class WorkflowSchedulerMode : StagePolicy<WorkflowSchedulerMode.Logic> {
    /**
     * The logic for operating the cycles of a workflow scheduler.
     */
    public interface Logic {
        /**
         * Request a new scheduling cycle to be performed.
         */
        public fun requestCycle()
    }

    /**
     * An interactive scheduler immediately triggers a new scheduling cycle when a workflow is received.
     */
    public object Interactive : WorkflowSchedulerMode() {
        override fun invoke(scheduler: WorkflowServiceImpl): Logic = object : Logic {
            override fun requestCycle() {
                scheduler.scope.launch { scheduler.schedule() }
            }
        }

        override fun toString(): String = "Interactive"
    }

    /**
     * A batch scheduler triggers a scheduling cycle every time quantum if needed.
     */
    public data class Batch(val quantum: Long) : WorkflowSchedulerMode() {
        private var next: kotlinx.coroutines.Job? = null

        override fun invoke(scheduler: WorkflowServiceImpl): Logic = object : Logic {
            override fun requestCycle() {
                if (next == null) {
                    // In batch mode, we assume that the scheduler runs at a fixed slot every time
                    // quantum (e.g t=0, t=60, t=120). We calculate here the delay until the next scheduling slot.
                    val delay = quantum - (scheduler.clock.millis() % quantum)

                    val job = scheduler.scope.launch {
                        delay(delay)
                        next = null
                        scheduler.schedule()
                    }
                    next = job
                }
            }
        }

        override fun toString(): String = "Batch($quantum)"
    }

    /**
     * A scheduling cycle is triggered at a random point in time.
     */
    public data class Random(private val random: java.util.Random = java.util.Random(123)) : WorkflowSchedulerMode() {
        private var next: kotlinx.coroutines.Job? = null

        override fun invoke(scheduler: WorkflowServiceImpl): Logic = object : Logic {
            override fun requestCycle() {
                if (next == null) {
                    val delay = random.nextInt(200).toLong()

                    val job = scheduler.scope.launch {
                        delay(delay)
                        next = null
                        scheduler.schedule()
                    }
                    next = job
                }
            }
        }

        override fun toString(): String = "Random"
    }
}
