/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.runner.execution

import com.atlarge.opendc.experiments.sc20.runner.ExperimentDescriptor
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

/**
 * An [ExperimentScheduler] that runs experiments using a local thread pool.
 *
 * @param parallelism The maximum amount of parallel workers (default is the number of available processors).
 */
class ThreadPoolExperimentScheduler(parallelism: Int = Runtime.getRuntime().availableProcessors() + 1) : ExperimentScheduler {
    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val tickets = Semaphore(parallelism)

    override suspend fun allocate(): ExperimentScheduler.Worker {
        tickets.acquire()
        return object : ExperimentScheduler.Worker {
            override suspend fun invoke(
                descriptor: ExperimentDescriptor,
                context: ExperimentExecutionContext
            ): ExperimentExecutionResult = supervisorScope {
                val listener =
                    object : ExperimentExecutionListener {
                        override fun descriptorRegistered(descriptor: ExperimentDescriptor) {
                            launch { context.listener.descriptorRegistered(descriptor) }
                        }

                        override fun executionFinished(descriptor: ExperimentDescriptor, result: ExperimentExecutionResult) {
                            launch { context.listener.executionFinished(descriptor, result) }
                        }

                        override fun executionStarted(descriptor: ExperimentDescriptor) {
                            launch { context.listener.executionStarted(descriptor) }
                        }
                    }

                val newContext = object : ExperimentExecutionContext by context {
                    override val listener: ExperimentExecutionListener = listener
                }

                try {
                    withContext(dispatcher) {
                        descriptor(newContext)
                        ExperimentExecutionResult.Success
                    }
                } catch (e: Throwable) {
                    ExperimentExecutionResult.Failed(e)
                } finally {
                    tickets.release()
                }
            }
        }
    }

    override fun close() = dispatcher.close()
}
