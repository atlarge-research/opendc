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

package org.opendc.cli.config

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.Theme

/**
 * The externally-configurable *presentation* of the CLI: the strings, symbols, colours and table
 * headers a shipper might re-skin or re-label. It is a plain typed tree (never a string map), loaded
 * from a bundled `opendc-ui.properties` and an optional external override by [CliConfigLoader].
 * [DEFAULTS] is the authoritative compiled-in fallback; the bundled file mirrors it exactly (guarded
 * by a test) so shipping a re-skinned CLI is just editing that file.
 */
internal data class CliConfig(
    val theme: ThemeColors,
    val symbols: Symbols,
    val bar: Bar,
    val panels: Panels,
    val headers: Headers,
    val timing: Timing,
    val logging: Logging,
) {
    companion object {
        val DEFAULTS =
            CliConfig(
                theme = ThemeColors(info = "", muted = "", success = "", danger = ""),
                symbols =
                    Symbols(
                        success = "✓",
                        failure = "✗",
                        bullet = "•",
                        dash = "—",
                        times = "×",
                        barComplete = "█",
                        barPending = "━",
                    ),
                bar = Bar(completedSuffix = " tasks"),
                panels =
                    Panels(
                        simulationTitle = "simulation",
                        progressTitle = "progress",
                        logsTitle = "logs",
                        labels =
                            FactLabels(
                                experiment = "experiment",
                                scenarios = "scenarios",
                                runs = "runs",
                                topologies = "topologies",
                                workloads = "workloads",
                                parallelism = "parallelism",
                                policies = "policies",
                                tasks = "tasks",
                                output = "output",
                                input = "input",
                            ),
                    ),
                headers =
                    Headers(
                        summary =
                            listOf(
                                "Scenario",
                                "Seed",
                                "Tasks",
                                "Completed",
                                "Terminated",
                                "Mean CPU",
                                "Energy [kWh]",
                                "Carbon [kg]",
                            ),
                        topology = listOf("Cluster", "Host", "Count", "CPU", "Memory", "GPU"),
                    ),
                timing = Timing(pollIntervalMs = 100L),
                logging = Logging(ringCapacity = 500, captureLevel = "WARN"),
            )

        /** Loads the effective configuration: bundled defaults overlaid by any external override. */
        fun load(): CliConfig = CliConfigLoader.load()
    }
}

/** Mordant theme roles. A blank value inherits the framework default; a colour name or `#rrggbb` overrides it. */
internal data class ThemeColors(
    val info: String,
    val muted: String,
    val success: String,
    val danger: String,
)

/** Single-glyph symbols used across messages, tables and the progress bar. */
internal data class Symbols(
    val success: String,
    val failure: String,
    val bullet: String,
    val dash: String,
    val times: String,
    val barComplete: String,
    val barPending: String,
)

/** The progress-bar wording: the suffix appended to the completed-count (e.g. " tasks"). */
internal data class Bar(
    val completedSuffix: String,
)

/** Dashboard panel titles and the labels of the constant facts shown in the top panel. */
internal data class Panels(
    val simulationTitle: String,
    val progressTitle: String,
    val logsTitle: String,
    val labels: FactLabels,
)

/** The field labels of the dashboard's simulation-overview panel. */
internal data class FactLabels(
    val experiment: String,
    val scenarios: String,
    val runs: String,
    val topologies: String,
    val workloads: String,
    val parallelism: String,
    val policies: String,
    val tasks: String,
    val output: String,
    val input: String,
)

/** Table column headers, in order. */
internal data class Headers(
    val summary: List<String>,
    val topology: List<String>,
)

/** How often the reporters poll their [org.opendc.cli.progress.ProgressSource] and redraw. */
internal data class Timing(
    val pollIntervalMs: Long,
)

/** How much of the live log stream the dashboard surfaces: the panel depth and the captured level. */
internal data class Logging(
    val ringCapacity: Int,
    val captureLevel: String,
)

/**
 * Builds a Mordant [Theme] from [Theme.Default], overriding only the roles whose configured colour
 * parses. Anything blank or unparseable leaves the framework default in place, so a partial or invalid
 * theme config degrades to the stock appearance rather than throwing.
 */
internal fun CliConfig.toMordantTheme(): Theme =
    Theme(Theme.Default) {
        theme.info.toTextStyleOrNull()?.let { styles["info"] = it }
        theme.muted.toTextStyleOrNull()?.let { styles["muted"] = it }
        theme.success.toTextStyleOrNull()?.let { styles["success"] = it }
        theme.danger.toTextStyleOrNull()?.let { styles["danger"] = it }
    }

/** Resolves a `#rrggbb` hex string or a named ANSI colour to a [TextStyle]; blank/invalid yields null. */
private fun String.toTextStyleOrNull(): TextStyle? {
    if (isBlank()) return null
    return try {
        if (startsWith("#")) TextColors.rgb(this) else TextColors.valueOf(this)
    } catch (_: IllegalArgumentException) {
        null
    }
}
