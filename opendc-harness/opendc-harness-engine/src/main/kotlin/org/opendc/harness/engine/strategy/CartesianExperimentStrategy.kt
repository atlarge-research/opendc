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

package org.opendc.harness.engine.strategy

import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.api.Parameter
import org.opendc.harness.api.Scenario
import org.opendc.harness.engine.internal.ScenarioImpl

/**
 * An [ExperimentStrategy] that takes the cartesian product of the parameters and evaluates every combination.
 */
public object CartesianExperimentStrategy : ExperimentStrategy {
    /**
     * Build the trials of an experiment.
     */
    override fun generate(experiment: ExperimentDefinition): Sequence<Scenario> {
        return experiment.parameters
            .asSequence()
            .map { param -> mapParameter(param).map { value -> listOf(param to value) } }
            .reduce { acc, param ->
                acc.flatMap { x -> param.map { y -> x + y } }
            }
            .mapIndexed { id, values -> ScenarioImpl(id, experiment, values.toMap()) }
    }

    /**
     * Instantiate a parameter and return a sequence of possible values.
     */
    private fun <T> mapParameter(param: Parameter<T>): Sequence<T> {
        return when (param) {
            is Parameter.Generic<T> -> param.values.asSequence()
        }
    }
}
