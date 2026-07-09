/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.runner

import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.resource.ResourceProvisioner
import org.opendc.sdk.model.workload.Workload
import org.opendc.sdk.runner.internal.ResourceScope
import org.opendc.sdk.runner.internal.toServiceTasks

/**
 * How many tasks a single run of [scenario] will submit, resolved before any simulation runs.
 *
 * @property scenario The scenario the count belongs to.
 * @property taskCount The number of tasks in the scenario's resolved workload.
 */
public data class ScenarioTaskCount(
    public val scenario: Scenario,
    public val taskCount: Int,
)

/**
 * Resolves every scenario's workload against [provisioner] — without running any simulation — and
 * reports how many tasks it will submit. Identical workloads are resolved only once.
 *
 * A workload's task count is otherwise known only once its run has begun (a trace has to be read to
 * be counted), which makes for a progress denominator that only settles as runs open. Call this
 * first to fix a stable, accurate total before [OpenDC.simulate]. Resolving a trace reads it here
 * once, in addition to the per-run load.
 */
public fun Experiment.planTaskCounts(resourceProvisioner: ResourceProvisioner): List<ScenarioTaskCount> {
    val resolved = HashMap<Workload, Int>()
    val scenarios: List<Scenario> = expand()

    return scenarios.map {
        val count =
            resolved.getOrPut(it.workload) {
                ResourceScope(resourceProvisioner).use(it::countTasks)
            }
        ScenarioTaskCount(it, count)
    }
}

public fun Scenario.countTasks(resourceProvisioner: ResourceProvisioner): Int = ResourceScope(resourceProvisioner).use(::countTasks)

internal fun Scenario.countTasks(resourceScope: ResourceScope): Int = workload.toServiceTasks(checkpointModel, resourceScope::resolve).size
