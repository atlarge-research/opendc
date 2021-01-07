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

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.opendc.harness.api.Trial
import java.util.concurrent.Executors

/**
 * An [ExperimentScheduler] that runs experiment trials using a local thread pool.
 *
 * @param parallelism The maximum amount of concurrent workers.
 */
public class ThreadPoolExperimentScheduler(parallelism: Int) : ExperimentScheduler {
    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val tickets = Semaphore(parallelism)

    override suspend fun allocate(): ExperimentScheduler.Worker {
        tickets.acquire()
        return object : ExperimentScheduler.Worker {
            override suspend fun dispatch(trial: Trial) {
                try {
                    withContext(dispatcher) {
                        trial.scenario.experiment.evaluator(trial)
                    }
                } finally {
                    tickets.release()
                }
            }
        }
    }

    override fun close(): Unit = dispatcher.close()

    override fun toString(): String = "ThreadPoolScheduler"
}
