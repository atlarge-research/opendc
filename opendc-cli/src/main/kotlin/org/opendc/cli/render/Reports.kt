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
import org.opendc.cli.config.CliConfig
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.runner.RunResult
import org.opendc.sdk.runner.SimulationReport
import java.nio.file.Path

private const val JOULES_PER_KWH = 3.6e6

/** One row of the per-run summary: the headline metrics of a single run, already reduced to display values. */
internal data class RunSummaryRow(
    val name: String,
    val seed: Long,
    val tasksTotal: Int,
    val tasksCompleted: Int,
    val tasksTerminated: Int,
    val meanCpuUtilization: Double,
    val energyKWh: Double,
    val carbonKg: Double,
)

/**
 * The render-ready per-run summary, decoupled from how the run was produced. A local run builds it
 * from a [SimulationReport]; a future remote run can build the same shape from an API response, so
 * both render identically.
 */
internal data class RunSummaryView(val rows: List<RunSummaryRow>) {
    companion object {
        /** Reduces every run's in-memory metrics into display rows; runs without metrics are skipped. */
        fun from(report: SimulationReport): RunSummaryView =
            RunSummaryView(report.scenarios.flatMap { s -> s.runs.mapNotNull { it.toRow(s.scenario) } })
    }
}

/** Where the run's Parquet output landed, and how many runs it covers. */
internal data class OutputView(val runCount: Int, val outputRoot: Path) {
    companion object {
        fun from(
            report: SimulationReport,
            outputRoot: Path,
        ): OutputView = OutputView(report.runs.size, outputRoot)
    }
}

/** Prints a per-run table of the headline metrics captured in memory during the run. */
internal fun renderSummary(
    terminal: Terminal,
    view: RunSummaryView,
    config: CliConfig,
) {
    if (view.rows.isEmpty()) return
    terminal.println(
        table {
            header { row(*config.headers.summary.toTypedArray()) }
            body {
                for (r in view.rows) {
                    row(
                        r.name,
                        r.seed,
                        r.tasksTotal,
                        r.tasksCompleted,
                        r.tasksTerminated,
                        "%.1f%%".format(r.meanCpuUtilization * 100),
                        "%.3f".format(r.energyKWh),
                        "%.3f".format(r.carbonKg),
                    )
                }
            }
        },
    )
}

/** Reports where the primary Parquet output was written. */
internal fun renderOutputs(
    terminal: Terminal,
    view: OutputView,
) {
    terminal.println(
        "Simulated ${terminal.theme.info(view.runCount.toString())} run(s); " +
            "Parquet results in ${terminal.theme.success(view.outputRoot.toString())}",
    )
}

/** Reduces a single run's in-memory metrics into a display [RunSummaryRow], or null when it has none. */
private fun RunResult.toRow(scenario: Scenario): RunSummaryRow? =
    metrics?.let { m ->
        RunSummaryRow(
            name = scenario.name.ifEmpty { "#${scenario.id}" },
            seed = seed,
            tasksTotal = m.service.maxOfOrNull { it.tasksTotal } ?: 0,
            tasksCompleted = m.service.maxOfOrNull { it.tasksCompleted } ?: 0,
            tasksTerminated = m.service.maxOfOrNull { it.tasksTerminated } ?: 0,
            meanCpuUtilization = if (m.host.isEmpty()) 0.0 else m.host.map { it.cpuUtilization }.average(),
            energyKWh = m.powerSource.sumOf { it.energyUsage } / JOULES_PER_KWH,
            carbonKg = m.powerSource.sumOf { it.carbonEmission } / 1000.0,
        )
    }
