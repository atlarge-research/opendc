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

import org.opendc.sdk.model.export.OutputFileSpec
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.SimulationReport
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.sink.InMemorySink
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies the per-run summary view-model is built from a real report and its derived metrics are correct. */
class SummaryViewTest {
    private fun tinyReport(): SimulationReport {
        val file = File(checkNotNull(javaClass.classLoader.getResource("experiments/tiny-experiment.json")).toURI())
        val experiment = file.inputStream().use { SdkJson.decodeExperiment(it) }
        return OpenDC.builder()
            .provisioner(FileSystemResourceProvisioner(file.absoluteFile.parentFile.toPath()))
            .sink(InMemorySink(setOf(OutputFileSpec.HOST, OutputFileSpec.SERVICE, OutputFileSpec.POWER_SOURCE)))
            .parallelism(1)
            .build()
            .simulate(experiment)
    }

    @Test
    fun `builds one row per run with a non-blank name`() {
        val report = tinyReport()
        val view = RunSummaryView.from(report)

        assertEquals(report.runs.count { it.metrics != null }, view.rows.size)
        assertTrue(view.rows.isNotEmpty(), "the tiny experiment produces at least one run")
        assertTrue(view.rows.all { it.name.isNotBlank() }, "every row is named (scenario name or #id fallback)")
    }

    @Test
    fun `derives energy in kWh and carbon in kg from the raw power metrics`() {
        val report = tinyReport()
        val view = RunSummaryView.from(report)

        val runsWithMetrics = report.runs.mapNotNull { it.metrics }
        val expectedEnergy = runsWithMetrics.sumOf { m -> m.powerSource.sumOf { it.energyUsage } } / 3.6e6
        val expectedCarbon = runsWithMetrics.sumOf { m -> m.powerSource.sumOf { it.carbonEmission } } / 1000.0

        assertEquals(expectedEnergy, view.rows.sumOf { it.energyKWh }, 1e-9)
        assertEquals(expectedCarbon, view.rows.sumOf { it.carbonKg }, 1e-9)
    }
}
