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

package org.opendc.cli.run

import org.opendc.cli.progress.ExperimentProgress
import org.opendc.cli.progress.ProgressSink
import org.opendc.cli.progress.ProgressSource
import org.opendc.cli.render.OutputView
import org.opendc.cli.render.RunSummaryView
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.export.OutputFileSpec.HOST
import org.opendc.sdk.model.export.OutputFileSpec.POWER_SOURCE
import org.opendc.sdk.model.export.OutputFileSpec.SERVICE
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.planTaskCounts
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.sink.InMemorySink

/**
 * Runs experiments locally, in-process, through the OpenDC SDK. A [ProgressSink] feeds a shared
 * [ExperimentProgress] during the blocking `simulate()` call, which the dashboard polls; the resulting
 * report is reduced to the render-ready [RunOutcome].
 */
internal class LocalBackend : SimulationBackend {
    override fun prepare(request: RunRequest): SimulationSession {
        val provisioner = FileSystemResourceProvisioner(request.inputRoot)
        val totalTasks =
            request.experiment.planTaskCounts(provisioner).sumOf { it.taskCount.toLong() * it.scenario.runs }
        val progressState = ExperimentProgress(totalTasks)

        val openDc = OpenDC.builder().provisioner(provisioner).output(request.output)
        request.parallelism?.let { openDc.parallelism(it) }
        if (request.wantSummary) openDc.sink(InMemorySink(setOf(HOST, SERVICE, POWER_SOURCE)))
        openDc.sink(ProgressSink(progressState))

        val scenarios = request.experiment.expand()
        val runOverview =
            SimulationOverview(
                name = request.experiment.name.ifEmpty { "(unnamed)" },
                scenarios = scenarios.size,
                runs = scenarios.sumOf { it.runs },
                topologies = request.experiment.topologies.size,
                workloads = request.experiment.workloads.size,
                policies = request.experiment.allocationPolicies.size,
                totalTasks = totalTasks,
                parallelism = request.parallelism ?: Runtime.getRuntime().availableProcessors(),
                output = request.output,
                inputRoot = request.inputRoot,
            )

        return object : SimulationSession {
            override val overview = runOverview
            override val progress: ProgressSource = progressState

            override fun run(): RunOutcome {
                val report = openDc.build().simulate(request.experiment)
                return RunOutcome(
                    summary = if (request.wantSummary) RunSummaryView.from(report) else null,
                    outputs = OutputView.from(report, request.output),
                )
            }
        }
    }
}
