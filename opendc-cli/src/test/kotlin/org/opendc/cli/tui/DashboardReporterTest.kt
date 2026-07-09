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
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.opendc.cli.config.CliConfig
import org.opendc.cli.progress.ExperimentProgress
import org.opendc.cli.run.SimulationOverview
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the dashboard host-selection gate and the reporter's start/stop lifecycle — the live path
 * the non-interactive CLI tests can never reach.
 */
class DashboardReporterTest {
    private val overview =
        SimulationOverview(
            name = "demo",
            scenarios = 1,
            runs = 1,
            topologies = 1,
            workloads = 1,
            policies = 1,
            totalTasks = 4,
            parallelism = 1,
            output = Path.of("output"),
            inputRoot = Path.of("."),
        )

    private fun rootAppenderNames(): Set<String> =
        (LogManager.getContext(false) as LoggerContext).configuration
            .getLoggerConfig(LogManager.ROOT_LOGGER_NAME).appenders.keys.toSet()

    @Test
    fun `no dashboard on a non-interactive terminal`() {
        val terminal = Terminal(ansiLevel = AnsiLevel.NONE, width = 80, height = 24, interactive = false)
        assertNull(startDashboard(terminal, ExperimentProgress(4), overview, CliConfig.DEFAULTS))
    }

    @Test
    fun `the dashboard starts on an interactive terminal and restores logging on stop`() {
        val terminal = Terminal(ansiLevel = AnsiLevel.NONE, width = 80, height = 24, interactive = true)
        val before = rootAppenderNames()

        val reporter = startDashboard(terminal, ExperimentProgress(4), overview, CliConfig.DEFAULTS)
        assertNotNull(reporter, "an interactive terminal must host the dashboard")
        try {
            assertTrue(rootAppenderNames().contains("TuiRingBuffer"), "log capture is attached while the dashboard runs")
        } finally {
            reporter.stop()
        }
        assertEquals(before, rootAppenderNames(), "logging is fully restored after stop()")
    }
}
