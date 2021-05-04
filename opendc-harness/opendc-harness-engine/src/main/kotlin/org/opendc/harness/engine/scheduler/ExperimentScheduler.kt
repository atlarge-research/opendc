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

package org.opendc.harness.engine.scheduler

import org.opendc.harness.api.Trial

/**
 * The [ExperimentScheduler] is responsible for scheduling the execution of experiment runs over some set of compute
 * resources (e.g., threads or even multiple machines).
 */
public interface ExperimentScheduler : AutoCloseable {
    /**
     * Allocate a [Worker] for executing an experiment trial. This method may suspend in case no resources are directly
     * available at the moment.
     *
     * @return The available worker.
     */
    public suspend fun allocate(): Worker

    /**
     * An isolated worker of an [ExperimentScheduler] that is responsible for conducting a single experiment trial.
     */
    public interface Worker {
        /**
         * Dispatch an experiment trial immediately to one of the available compute resources and block execution until
         * the trial has finished.
         *
         * @param trial The trial to dispatch.
         */
        public suspend fun dispatch(trial: Trial)
    }
}
