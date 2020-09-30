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

package org.opendc.experiments.sc20.runner.execution

import org.opendc.experiments.sc20.runner.ExperimentDescriptor
import java.io.Closeable

/**
 * A interface for scheduling the execution of experiment trials over compute resources (threads/containers/vms)
 */
interface ExperimentScheduler : Closeable {
    /**
     * Allocate a [Worker] for executing an experiment trial. This method may suspend in case no resources are directly
     * available at the moment.
     *
     * @return The available worker.
     */
    suspend fun allocate(): ExperimentScheduler.Worker

    /**
     * An isolated worker of an [ExperimentScheduler] that is responsible for executing a single experiment trial.
     */
    interface Worker {
        /**
         * Dispatch the specified [ExperimentDescriptor] to execute some time in the future and return the results of
         * the trial.
         *
         * @param descriptor The descriptor to execute.
         * @param context The context to execute the descriptor in.
         */
        suspend operator fun invoke(
            descriptor: ExperimentDescriptor,
            context: ExperimentExecutionContext
        )
    }
}
