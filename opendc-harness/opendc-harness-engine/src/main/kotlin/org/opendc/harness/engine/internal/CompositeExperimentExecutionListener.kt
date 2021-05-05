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

package org.opendc.harness.engine.internal

import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.api.Scenario
import org.opendc.harness.api.Trial
import org.opendc.harness.engine.ExperimentExecutionListener

/**
 * An [ExperimentExecutionListener] that composes multiple other listeners.
 */
public class CompositeExperimentExecutionListener(private val listeners: List<ExperimentExecutionListener>) : ExperimentExecutionListener {
    override fun experimentStarted(experiment: ExperimentDefinition) {
        listeners.forEach { it.experimentStarted(experiment) }
    }

    override fun experimentFinished(experiment: ExperimentDefinition, throwable: Throwable?) {
        listeners.forEach { it.experimentFinished(experiment, throwable) }
    }

    override fun scenarioStarted(scenario: Scenario) {
        listeners.forEach { it.scenarioStarted(scenario) }
    }

    override fun scenarioFinished(scenario: Scenario, throwable: Throwable?) {
        listeners.forEach { it.scenarioFinished(scenario, throwable) }
    }

    override fun trialStarted(trial: Trial) {
        listeners.forEach { it.trialStarted(trial) }
    }

    override fun trialFinished(trial: Trial, throwable: Throwable?) {
        listeners.forEach { it.trialFinished(trial, throwable) }
    }
}
