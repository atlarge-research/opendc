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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.engine.internal.CompositeExperimentExecutionListener
import org.opendc.harness.engine.scheduler.ExperimentScheduler
import org.opendc.harness.engine.scheduler.ThreadPoolExperimentScheduler
import org.opendc.harness.engine.strategy.CartesianExperimentStrategy
import org.opendc.harness.engine.strategy.ExperimentStrategy

/**
 * A builder class for conducting experiments via the [ExperimentEngine].
 */
public class ExperimentEngineLauncher private constructor(
    private val strategy: ExperimentStrategy?,
    private val scheduler: ExperimentScheduler?,
    private val listeners: List<ExperimentExecutionListener>,
    private val repeats: Int
) {
    /**
     * Construct an [ExperimentEngineLauncher] instance.
     */
    public constructor() : this(null, null, emptyList(), 1)

    /**
     * Create an [ExperimentEngineLauncher] with the specified [strategy].
     */
    public fun withScheduler(strategy: ExperimentStrategy): ExperimentEngineLauncher {
        return ExperimentEngineLauncher(strategy, scheduler, listeners, repeats)
    }

    /**
     * Create an [ExperimentEngineLauncher] with the specified [scheduler].
     */
    public fun withScheduler(scheduler: ExperimentScheduler): ExperimentEngineLauncher {
        return ExperimentEngineLauncher(strategy, scheduler, listeners, repeats)
    }

    /**
     * Create an [ExperimentEngineLauncher] with the specified [listener] added.
     */
    public fun withListener(listener: ExperimentExecutionListener): ExperimentEngineLauncher {
        return ExperimentEngineLauncher(strategy, scheduler, listeners + listener, repeats)
    }

    /**
     * Create an [ExperimentEngineLauncher] with the specified number of repeats.
     */
    public fun withRepeats(repeats: Int): ExperimentEngineLauncher {
        require(repeats > 0) { "Invalid number of repeats; must be greater than zero. " }
        return ExperimentEngineLauncher(strategy, scheduler, listeners, repeats)
    }

    /**
     * Launch the specified experiments via the [ExperimentEngine] and block execution until finished.
     */
    public suspend fun run(experiments: Flow<ExperimentDefinition>) {
        val engine = ExperimentEngine(createStrategy(), createScheduler(), createListener(), repeats)
        experiments.collect { experiment -> engine.execute(experiment) }
    }

    /**
     * Launch the specified experiments via the [ExperimentEngine] and block the current thread until finished.
     */
    public fun runBlocking(experiments: Flow<ExperimentDefinition>) {
        runBlocking {
            run(experiments)
        }
    }

    /**
     * Return a string representation of this instance.
     */
    public override fun toString(): String = "ExperimentEngineLauncher"

    /**
     * Create the [ExperimentStrategy] that explores the experiment design space.
     */
    private fun createStrategy(): ExperimentStrategy {
        return strategy ?: CartesianExperimentStrategy
    }

    /**
     * Create the [ExperimentScheduler] that schedules the trials over the compute resources.
     */
    private fun createScheduler(): ExperimentScheduler {
        return scheduler ?: ThreadPoolExperimentScheduler(Runtime.getRuntime().availableProcessors())
    }

    /**
     * Create the [ExperimentExecutionListener] that listens the to the execution of the experiments.
     */
    private fun createListener(): ExperimentExecutionListener {
        require(listeners.isNotEmpty()) { "No listeners registered." }
        return CompositeExperimentExecutionListener(listeners)
    }
}
