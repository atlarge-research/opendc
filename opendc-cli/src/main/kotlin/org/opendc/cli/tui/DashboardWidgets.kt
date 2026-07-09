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

package org.opendc.cli.tui

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.Viewport
import org.opendc.cli.config.CliConfig
import org.opendc.cli.run.SimulationOverview

/** Panel borders (2) plus one column of horizontal padding on each side, subtracted from the terminal width. */
private const val INNER_WIDTH_MARGIN = 4

/** The gap, in columns, between the info panel's scalar columns. */
private const val FACT_COLUMN_PADDING = 3

/**
 * The dashboard widgets below are pure functions of the presentation they need — a [Theme] to style
 * text and an [Int] content width to lay it out — never the whole terminal, so they stay trivially
 * testable and free of terminal I/O. The caller reads those off its live terminal.
 *
 * [dashboardWidget] stacks the given [panels] flush against each other into the whole dashboard.
 */
internal fun dashboardWidget(panels: List<Widget>): Widget =
    verticalLayout {
        spacing = 0
        panels.forEach { cell(it) }
    }

/**
 * The top panel of constant simulation overview, built once. Short scalars are laid out in a borderless
 * grid so their columns line up; the (potentially long) name and paths get their own full-width rows.
 */
internal fun infoPanel(
    theme: Theme,
    width: Int,
    overview: SimulationOverview,
    config: CliConfig,
): Widget {
    val labels = config.panels.labels
    val scalars =
        grid {
            padding = Padding(0, FACT_COLUMN_PADDING, 0, 0)
            row(
                label(theme, labels.scenarios),
                value(theme, overview.scenarios),
                label(theme, labels.topologies),
                value(theme, overview.topologies),
            )
            row(
                label(theme, labels.runs),
                value(theme, overview.runs),
                label(theme, labels.workloads),
                value(theme, overview.workloads),
            )
            row(
                label(theme, labels.parallelism),
                value(theme, overview.parallelism),
                label(theme, labels.policies),
                value(theme, overview.policies),
            )
            row(label(theme, labels.tasks), value(theme, overview.totalTasks))
        }
    val content =
        verticalLayout {
            spacing = 0
            cell(kvLine(theme, labels.experiment, overview.name))
            cell(scalars)
            cell(kvLine(theme, labels.output, overview.output))
            cell(kvLine(theme, labels.input, overview.inputRoot))
        }
    return panel(theme, config.panels.simulationTitle, Viewport(content, width = innerWidth(width), height = null))
}

/** The middle panel holding the (thick, full-width) progress [bar]. */
internal fun progressPanel(
    theme: Theme,
    bar: Widget,
    config: CliConfig,
): Widget = panel(theme, config.panels.progressTitle, bar)

/**
 * The bottom panel showing the last [rows] log lines, newest at the bottom. The [Viewport] pads the
 * box to [rows] rows when short and crops it when full, so the border never jumps, and clips each
 * line to the panel [width] so a long line cannot wrap and push others out of view.
 */
internal fun logsPanel(
    theme: Theme,
    width: Int,
    lines: List<String>,
    rows: Int,
    config: CliConfig,
): Widget {
    val body = Text(lines.takeLast(rows).joinToString("\n"), whitespace = Whitespace.PRE)
    return panel(theme, config.panels.logsTitle, Viewport(body, width = innerWidth(width), height = rows))
}

/** The usable content width inside a full-width panel (terminal width minus borders and padding). */
private fun innerWidth(width: Int): Int = (width - INNER_WIDTH_MARGIN).coerceAtLeast(1)

private fun panel(
    theme: Theme,
    title: String,
    content: Widget,
): Widget =
    Panel(
        content = content,
        title = Text(theme.info(title)),
        expand = true,
        borderType = BorderType.ROUNDED,
    )

private fun label(
    theme: Theme,
    text: String,
): String = theme.muted(text)

private fun value(
    theme: Theme,
    v: Any,
): String = theme.info(v.toString())

private fun kvLine(
    theme: Theme,
    key: String,
    v: Any,
): Widget = Text("${label(theme, key)} ${value(theme, v)}", whitespace = Whitespace.PRE)
