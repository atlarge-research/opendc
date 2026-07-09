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

package org.opendc.cli.render

import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.runner.RunResult
import org.opendc.sdk.runner.SimulationReport
import org.opendc.sdk.runner.sink.CollectedMetrics
import java.nio.file.Path

private const val JOULES_PER_KWH = 3.6e6

/** The headline metrics of a single run, derived from its in-memory [CollectedMetrics]. */
private class RunSummary(scenario: Scenario, val seed: Long, metrics: CollectedMetrics) {
    val name: String = scenario.name.ifEmpty { "#${scenario.id}" }
    val tasksTotal = metrics.service.maxOfOrNull { it.tasksTotal } ?: 0
    val tasksCompleted = metrics.service.maxOfOrNull { it.tasksCompleted } ?: 0
    val tasksTerminated = metrics.service.maxOfOrNull { it.tasksTerminated } ?: 0
    val meanCpuUtilization = if (metrics.host.isEmpty()) 0.0 else metrics.host.map { it.cpuUtilization }.average()
    val energyKWh = metrics.powerSource.sumOf { it.energyUsage } / JOULES_PER_KWH
    val carbonKg = metrics.powerSource.sumOf { it.carbonEmission } / 1000.0
}

/** Prints a per-run table of the headline metrics captured in memory during the run. */
internal fun renderSummary(
    terminal: Terminal,
    report: SimulationReport,
) {
    val summaries = report.scenarios.flatMap { s -> s.runs.mapNotNull { it.summarize(s.scenario) } }
    if (summaries.isEmpty()) return

    terminal.println(
        table {
            header { row("Scenario", "Seed", "Tasks", "Completed", "Terminated", "Mean CPU", "Energy [kWh]", "Carbon [kg]") }
            body {
                for (s in summaries) {
                    row(
                        s.name,
                        s.seed,
                        s.tasksTotal,
                        s.tasksCompleted,
                        s.tasksTerminated,
                        "%.1f%%".format(s.meanCpuUtilization * 100),
                        "%.3f".format(s.energyKWh),
                        "%.3f".format(s.carbonKg),
                    )
                }
            }
        },
    )
}

/** Reports where the primary Parquet output was written. */
internal fun renderOutputs(
    terminal: Terminal,
    report: SimulationReport,
    outputRoot: Path,
) {
    val runs = report.runs.size
    terminal.println(
        "Simulated ${terminal.theme.info(runs.toString())} run(s); Parquet results in ${terminal.theme.success(outputRoot.toString())}",
    )
}

private fun RunResult.summarize(scenario: Scenario): RunSummary? = metrics?.let { RunSummary(scenario, seed, it) }
