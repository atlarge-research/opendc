/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.experiments.base.experiment.specs

import kotlinx.serialization.Serializable
import org.opendc.compute.simulator.telemetry.OutputFiles
import org.opendc.compute.simulator.telemetry.parquet.ComputeExportConfig

/**
 * specification describing how the results should be exported
 *
 * @property exportInterval The interval of exporting results in s. Should be higher than 0.0
 */
@Serializable
public data class ExportModelSpec(
    val exportInterval: Long = 5 * 60,
    val computeExportConfig: ComputeExportConfig = ComputeExportConfig.ALL_COLUMNS,
    val filesToExport: List<OutputFiles> = OutputFiles.entries.toList(),
    var filesToExportDict: MutableMap<OutputFiles, Boolean> = OutputFiles.entries.associateWith { false }.toMutableMap(),
    var printFrequency: Int? = 24,
) {
    init {
        require(exportInterval > 0) { "The Export interval has to be higher than 0" }

        // Create a dictionary with each output file to false.
        // Set each file in [filesToExport] to true in the dictionary.
        for (file in filesToExport) {
            filesToExportDict[file] = true
        }
    }
}
