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

package org.opendc.harness.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.api.Trial
import org.opendc.harness.engine.scheduler.ExperimentScheduler
import org.opendc.harness.engine.strategy.ExperimentStrategy

/**
 * The [ExperimentEngine] orchestrates the execution of experiments.
 *
 * @property strategy The [ExperimentStrategy] used to explore the experiment design space.
 * @property scheduler The [ExperimentScheduler] to schedule the trials over compute resources.
 * @property listener The [ExperimentExecutionListener] to observe the progress.
 * @property repeats The number of repeats to perform.
 */
public class ExperimentEngine(
    private val strategy: ExperimentStrategy,
    private val scheduler: ExperimentScheduler,
    private val listener: ExperimentExecutionListener,
    private val repeats: Int
) {
    /**
     * Execute the specified [experiment][root].
     *
     * @param root The experiment to execute.
     */
    public suspend fun execute(root: ExperimentDefinition) {
        listener.experimentStarted(root)

        try {
            supervisorScope {
                strategy.generate(root)
                    .asFlow()
                    .map { scenario ->
                        listener.scenarioStarted(scenario)
                        scenario
                    }
                    .buffer(100)
                    .collect { scenario ->
                        launch {
                            val jobs = (0 until repeats).map { repeat ->
                                val worker = scheduler.allocate()
                                launch {
                                    val trial = Trial(scenario, repeat)
                                    try {
                                        listener.trialStarted(trial)
                                        worker.dispatch(trial)
                                        listener.trialFinished(trial, null)
                                    } catch (e: Throwable) {
                                        listener.trialFinished(trial, e)
                                        throw e
                                    }
                                }
                            }

                            try {
                                jobs.joinAll()
                                listener.scenarioFinished(scenario, null)
                            } catch (e: CancellationException) {
                                listener.scenarioFinished(scenario, null)
                                throw e
                            } catch (e: Throwable) {
                                listener.scenarioFinished(scenario, e)
                            }
                        }
                    }
            }

            listener.experimentFinished(root, null)
        } catch (e: Throwable) {
            listener.experimentFinished(root, e)
            throw e
        }
    }

    override fun toString(): String = "ExperimentEngine"
}
