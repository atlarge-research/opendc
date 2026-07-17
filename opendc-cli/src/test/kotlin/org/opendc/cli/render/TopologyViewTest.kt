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

import org.opendc.cli.config.CliConfig
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.serialization.SdkJson
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies the topology view-model maps every topology, counts hosts, and formats CPU/GPU cells. */
class TopologyViewTest {
    private fun tinyExperiment(): ExperimentSpec {
        val file = File(checkNotNull(javaClass.classLoader.getResource("experiments/tiny-experiment.json")).toURI())
        return file.inputStream().use { SdkJson.decodeExperiment(it) }
    }

    @Test
    fun `maps every topology, counts hosts, and formats the CPU cell`() {
        val experiment = tinyExperiment()
        val view = TopologyView.from(experiment, CliConfig.DEFAULTS)

        assertEquals(experiment.topologies.size, view.topologies)
        assertEquals(experiment.topologies.size, view.entries.size)
        assertTrue(view.entries.isNotEmpty())

        val entry = view.entries.first()
        val expectedHosts = experiment.topologies.first().clusters.sumOf { c -> c.count * c.hosts.sumOf { it.count } }
        assertEquals(expectedHosts, entry.hostCount)

        val row = entry.rows.first()
        assertTrue(row.cpu.contains("cores @"), row.cpu)
        assertTrue(row.cpu.contains(CliConfig.DEFAULTS.symbols.times), row.cpu)
    }

    @Test
    fun `renders a GPU-less host with the configured dash`() {
        // The tiny experiment's single host declares no GPU, so its GPU cell is the dash symbol.
        val view = TopologyView.from(tinyExperiment(), CliConfig.DEFAULTS)
        val gpuCells = view.entries.flatMap { it.rows }.map { it.gpu }
        assertTrue(gpuCells.contains(CliConfig.DEFAULTS.symbols.dash), "expected a GPU-less host rendered with the dash")
    }
}
