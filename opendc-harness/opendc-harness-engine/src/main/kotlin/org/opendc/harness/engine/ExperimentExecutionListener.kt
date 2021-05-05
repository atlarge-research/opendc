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

import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.api.Scenario
import org.opendc.harness.api.Trial

/**
 * Listener to be notified of experiment execution events by experiment runners.
 */
public interface ExperimentExecutionListener {
    /**
     * A method that is invoked when an experiment is started.
     *
     * @param experiment The [ExperimentDefinition] that started.
     */
    public fun experimentStarted(experiment: ExperimentDefinition) {}

    /**
     * A method that is invoked when an experiment is finished, regardless of the outcome.
     *
     * @param experiment The [ExperimentDefinition] that finished.
     * @param throwable The exception that was thrown during execution or `null` if the execution completed successfully.
     */
    public fun experimentFinished(experiment: ExperimentDefinition, throwable: Throwable?) {}

    /**
     * A method that is invoked when a scenario is started.
     *
     * @param scenario The scenario that is started.
     */
    public fun scenarioStarted(scenario: Scenario) {}

    /**
     * A method that is invoked when a scenario is finished, regardless of the outcome.
     *
     * @param scenario The [Scenario] that has finished.
     * @param throwable The exception that was thrown during execution or `null` if the execution completed successfully.
     */
    public fun scenarioFinished(scenario: Scenario, throwable: Throwable?) {}

    /**
     * A method that is invoked when a trial is started.
     *
     * @param trial The trial that is started.
     */
    public fun trialStarted(trial: Trial) {}

    /**
     * A method that is invoked when a scenario is finished, regardless of the outcome.
     *
     * @param trial The [Trial] that has finished.
     * @param throwable The exception that was thrown during execution or `null` if the execution completed successfully.
     */
    public fun trialFinished(trial: Trial, throwable: Throwable?) {}
}
