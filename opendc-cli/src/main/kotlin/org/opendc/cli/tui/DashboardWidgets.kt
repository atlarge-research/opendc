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
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.Viewport

/** Stacks the given [panels] flush against each other into the whole dashboard. */
internal fun dashboardWidget(panels: List<Widget>): Widget =
    verticalLayout {
        spacing = 0
        panels.forEach { cell(it) }
    }

/**
 * The top panel of constant simulation facts, built once. Short scalars are laid out in a borderless
 * grid so their columns line up; the (potentially long) name and paths get their own full-width rows.
 */
internal fun infoPanel(
    terminal: Terminal,
    facts: SimulationFacts,
): Widget {
    val scalars =
        grid {
            padding = Padding(0, 3, 0, 0)
            row(
                label(terminal, "scenarios"),
                value(terminal, facts.scenarios),
                label(terminal, "topologies"),
                value(terminal, facts.topologies),
            )
            row(label(terminal, "runs"), value(terminal, facts.runs), label(terminal, "workloads"), value(terminal, facts.workloads))
            row(
                label(terminal, "parallelism"),
                value(terminal, facts.parallelism),
                label(terminal, "policies"),
                value(terminal, facts.policies),
            )
            row(label(terminal, "tasks"), value(terminal, facts.totalTasks))
        }
    val content =
        verticalLayout {
            spacing = 0
            cell(kvLine(terminal, "experiment", facts.name))
            cell(scalars)
            cell(kvLine(terminal, "output", facts.output))
            cell(kvLine(terminal, "input", facts.inputRoot))
        }
    return panel(terminal, "simulation", Viewport(content, width = innerWidth(terminal), height = null))
}

/** The middle panel holding the (thick, full-width) progress [bar]. */
internal fun progressPanel(
    terminal: Terminal,
    bar: Widget,
): Widget = panel(terminal, "progress", bar)

/**
 * The bottom panel showing the last [rows] log lines, newest at the bottom. The [Viewport] pads the
 * box to [rows] rows when short and crops it when full, so the border never jumps, and clips each
 * line to the panel width so a long line cannot wrap and push others out of view.
 */
internal fun logsPanel(
    terminal: Terminal,
    lines: List<String>,
    rows: Int,
): Widget {
    val body = Text(lines.takeLast(rows).joinToString("\n"), whitespace = Whitespace.PRE)
    return panel(terminal, "logs", Viewport(body, width = innerWidth(terminal), height = rows))
}

/** The usable content width inside a full-width panel (terminal width minus borders and padding). */
private fun innerWidth(terminal: Terminal): Int = (terminal.size.width - 4).coerceAtLeast(1)

private fun panel(
    terminal: Terminal,
    title: String,
    content: Widget,
): Widget =
    Panel(
        content = content,
        title = Text(terminal.theme.info(title)),
        expand = true,
        borderType = BorderType.ROUNDED,
    )

private fun label(
    terminal: Terminal,
    text: String,
): String = terminal.theme.muted(text)

private fun value(
    terminal: Terminal,
    v: Any,
): String = terminal.theme.info(v.toString())

private fun kvLine(
    terminal: Terminal,
    key: String,
    v: Any,
): Widget = Text("${label(terminal, key)} ${value(terminal, v)}", whitespace = Whitespace.PRE)
