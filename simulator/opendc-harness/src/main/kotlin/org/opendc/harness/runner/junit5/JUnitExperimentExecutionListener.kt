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

package org.opendc.harness.runner.junit5

import org.junit.platform.engine.*
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.api.Scenario
import org.opendc.harness.api.Trial
import org.opendc.harness.engine.ExperimentExecutionListener
import java.util.*

/**
 * An [ExperimentExecutionListener] that notifies JUnit platform of the progress of the experiment trials.
 */
public class JUnitExperimentExecutionListener(
    private val listener: EngineExecutionListener,
    private val root: EngineDescriptor
) : ExperimentExecutionListener {
    /**
     * The current active experiments.
     */
    private val experiments = mutableMapOf<ExperimentDefinition, TestDescriptor>()

    /**
     * The current active scenarios.
     */
    private val scenarios = mutableMapOf<Scenario, TestDescriptor>()

    /**
     * The current active trials.
     */
    private val trials = mutableMapOf<Trial, TestDescriptor>()

    override fun experimentStarted(experiment: ExperimentDefinition) {
        val descriptor = experiment.toDescriptor(root)
        root.addChild(descriptor)
        experiments[experiment] = descriptor

        listener.dynamicTestRegistered(descriptor)
        listener.executionStarted(descriptor)
    }

    override fun experimentFinished(experiment: ExperimentDefinition, throwable: Throwable?) {
        val descriptor = experiments.remove(experiment)

        if (throwable != null) {
            listener.executionFinished(descriptor, TestExecutionResult.failed(throwable))
        } else {
            listener.executionFinished(descriptor, TestExecutionResult.successful())
        }
    }

    override fun scenarioStarted(scenario: Scenario) {
        val parent = experiments[scenario.experiment] ?: return
        val descriptor = scenario.toDescriptor(parent)
        parent.addChild(descriptor)
        scenarios[scenario] = descriptor

        listener.dynamicTestRegistered(descriptor)
        listener.executionStarted(descriptor)
    }

    override fun scenarioFinished(scenario: Scenario, throwable: Throwable?) {
        val descriptor = scenarios.remove(scenario)

        if (throwable != null) {
            listener.executionFinished(descriptor, TestExecutionResult.failed(throwable))
        } else {
            listener.executionFinished(descriptor, TestExecutionResult.successful())
        }
    }

    override fun trialStarted(trial: Trial) {
        val parent = scenarios[trial.scenario] ?: return
        val descriptor = trial.toDescriptor(parent)
        parent.addChild(descriptor)
        trials[trial] = descriptor

        listener.dynamicTestRegistered(descriptor)
        listener.executionStarted(descriptor)
    }

    override fun trialFinished(trial: Trial, throwable: Throwable?) {
        val descriptor = trials.remove(trial)

        if (throwable != null) {
            listener.executionFinished(descriptor, TestExecutionResult.failed(throwable))
        } else {
            listener.executionFinished(descriptor, TestExecutionResult.successful())
        }
    }

    /**
     * Create a [TestDescriptor] for an [ExperimentDefinition].
     */
    private fun ExperimentDefinition.toDescriptor(parent: TestDescriptor): TestDescriptor {
        return object : AbstractTestDescriptor(parent.uniqueId.append("experiment", name), name) {
            override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

            override fun mayRegisterTests(): Boolean = true

            override fun toString(): String = "ExperimentDescriptor"

            override fun getSource(): Optional<TestSource> {
                val cls = meta["class.name"] as? String
                return if (cls != null)
                    Optional.of(ClassSource.from(cls))
                else
                    Optional.empty()
            }
        }
    }

    /**
     * Create a [TestDescriptor] for a [Scenario].
     */
    private fun Scenario.toDescriptor(parent: TestDescriptor): TestDescriptor {
        return object : AbstractTestDescriptor(parent.uniqueId.append("scenario", id.toString()), "Scenario $id") {
            override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER_AND_TEST

            override fun mayRegisterTests(): Boolean = true

            override fun toString(): String = "ScenarioDescriptor"
        }
    }

    /**
     * Create a [TestDescriptor] for a [Trial].
     */
    private fun Trial.toDescriptor(parent: TestDescriptor): TestDescriptor {
        return object : AbstractTestDescriptor(parent.uniqueId.append("repeat", repeat.toString()), "Repeat $repeat") {
            override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

            override fun mayRegisterTests(): Boolean = false

            override fun toString(): String = "TrialDescriptor"
        }
    }
}
