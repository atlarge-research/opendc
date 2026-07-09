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

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Text
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Verifies the dashboard widgets render the three panels, show the facts, and clip the log tail. */
class DashboardWidgetsTest {
    private val terminal = Terminal(ansiLevel = AnsiLevel.NONE, width = 80, interactive = false)

    private val facts =
        SimulationFacts(
            name = "demo",
            scenarios = 12,
            runs = 240,
            topologies = 3,
            workloads = 1,
            policies = 4,
            totalTasks = 1234,
            parallelism = 8,
            output = Path.of("output"),
            inputRoot = Path.of("."),
        )

    @Test
    fun `renders three panels with facts and the newest logs`() {
        val logs = (1..50).map { "line$it" }
        val out =
            terminal.render(
                dashboardWidget(
                    listOf(
                        infoPanel(terminal, facts),
                        progressPanel(terminal, Text("BAR")),
                        logsPanel(terminal, logs, rows = 8),
                    ),
                ),
            )

        assertContains(out, "simulation")
        assertContains(out, "progress")
        assertContains(out, "logs")
        assertContains(out, "demo")
        assertContains(out, "1234")
        assertContains(out, "line50") // newest shown
        assertContains(out, "line43") // oldest of the last 8
        assertFalse(out.contains("line42"), "logs beyond the last 8 must be clipped")
    }

    @Test
    fun `logs box keeps a fixed height regardless of line count`() {
        val short = terminal.render(logsPanel(terminal, listOf("only one"), rows = 8))
        val full = terminal.render(logsPanel(terminal, (1..40).map { "line$it" }, rows = 8))
        assertEquals(full.trimEnd().lines().size, short.trimEnd().lines().size)
    }
}
