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
import org.opendc.common.logger.infoNewLine
import org.opendc.common.logger.logger
import org.opendc.compute.simulator.telemetry.OutputFiles
import org.opendc.compute.simulator.telemetry.parquet.ComputeExportConfig
import kotlin.getValue

/**
 * Specification describing how and how often the simulation results are exported.
 *
 * @property exportInterval The interval between two consecutive telemetry exports, in seconds. Must be positive.
 * Default is 5 minutes
 * @property printFrequency How often, in export intervals, a progress line is printed to the log. `null` disables
 * progress printing. When set it must be positive. Default is 24.
 * @property computeExportConfig Configuration of which metrics are written to the output files. Default is all metrics.
 * @property filesToExport The output files that should be written during the simulation. Default is all files.
 */
@Serializable
public data class ExportModelSpec(
    val exportInterval: Long = 5 * 60,
    var printFrequency: Int? = 24,
    val computeExportConfig: ComputeExportConfig = ComputeExportConfig.ALL_COLUMNS,
    val filesToExport: List<OutputFiles> = OutputFiles.entries.toList(),
) {
    /**
     * Lookup that maps every [OutputFiles] entry to whether it should be exported.
     *
     * This is a derived value computed from [filesToExport]: it is intentionally declared outside the
     * primary constructor so it is neither part of the (de)serialized experiment file nor settable by
     * the user. It exists only for the rest of the system to consume.
     */
    public val filesToExportDict: Map<OutputFiles, Boolean> =
        OutputFiles.entries.associateWith { it in filesToExport }

    init {
        LOG.infoNewLine(computeExportConfig.fmt())
    }

    /**
     * Validate the constraints of this export model specification.
     *
     * All violated constraints are collected and reported together, so a user fixing an export model
     * sees every problem at once instead of one per run. When any constraint is violated an
     * [InvalidExportModelException] is thrown; otherwise this returns nothing.
     *
     * @throws InvalidExportModelException if one or more constraints are violated.
     */
    public fun validate() {
        val errors =
            buildList {
                if (exportInterval <= 0) {
                    add("The export interval has to be higher than 0 (currently exportInterval=$exportInterval)")
                }
                val printFrequency = printFrequency
                if (printFrequency != null && printFrequency <= 0) {
                    add("The print frequency has to be higher than 0 (currently printFrequency=$printFrequency)")
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidExportModelException(errors)
        }
    }

    internal companion object {
        private val LOG by logger()
    }
}

/**
 * Exception thrown when an [ExportModelSpec] violates one or more of its constraints.
 *
 * @property errors The human-readable descriptions of every violated constraint.
 */
public class InvalidExportModelException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid export model specification:\n" + errors.joinToString("\n") { "  - $it" },
    )
