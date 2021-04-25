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

package org.opendc.harness.dsl

import org.junit.platform.commons.annotation.Testable
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.api.Scenario
import org.opendc.harness.api.Trial
import org.opendc.harness.internal.ParameterDelegate

/**
 * An [Experiment] defines a blueprint for a repeatable experiment consisting of multiple scenarios based on pre-defined
 * experiment parameters.
 *
 * @param name The name of the experiment or `null` to select the class name.
 */
@Testable
public abstract class Experiment(name: String? = null) : Cloneable {
    /**
     * The name of the experiment.
     */
    public val name: String = name ?: javaClass.simpleName

    /**
     * An identifier that uniquely identifies a single point in the design space of this [Experiment].
     */
    public val id: Int
        get() {
            val scenario = scenario ?: throw IllegalStateException("Cannot use id before activation")
            return scenario.id
        }

    /**
     * Convert this experiment to an [ExperimentDefinition].
     */
    public fun toDefinition(): ExperimentDefinition =
        ExperimentDefinition(name, HashSet(delegates.map { it.parameter }), this::run, mapOf("class.name" to javaClass.name))

    /**
     * Perform a single execution of the experiment based on the experiment parameters.
     *
     * @param repeat A number representing the repeat index of an identical scenario.
     */
    protected abstract fun doRun(repeat: Int)

    /**
     * A map to track the parameter delegates registered with this experiment.
     */
    private val delegates: MutableSet<ParameterDelegate<*>> = mutableSetOf()

    /**
     * The current active scenario.
     */
    internal var scenario: Scenario? = null

    /**
     * Perform a single execution of the experiment based on the experiment parameters.
     *
     * This operation will cause the [ParameterProvider]s of this group to be instantiated to some value in its design
     * space.
     *
     * @param trial The experiment trial to run.
     */
    private fun run(trial: Trial) {
        val scenario = trial.scenario

        // XXX We clone the current class to prevent concurrency issues.
        val res = clone() as Experiment
        res.scenario = scenario
        res.doRun(trial.repeat)
    }

    /**
     * Register a delegate for this experiment.
     */
    internal fun register(delegate: ParameterDelegate<*>) {
        delegates += delegate
    }
}
