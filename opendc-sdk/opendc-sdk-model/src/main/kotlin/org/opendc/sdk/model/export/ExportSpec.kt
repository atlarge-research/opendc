/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.export

import kotlinx.serialization.Serializable
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

/**
 * Configuration controlling how simulation results are exported.
 *
 * @property exportInterval Wall-clock time between consecutive metric snapshots.
 * @property printFrequency Number of snapshots between progress prints, or `null` to disable printing.
 * @property columns Per-output-file column selections.
 * @property filesToExport Output files to produce.
 */
@Serializable
public data class ExportSpec(
    public val exportInterval: TimeDelta = TimeDelta.ofMin(5),
    public val printFrequency: Int? = 24,
    public val columns: ExportColumnsSpec = ExportColumnsSpec(),
    public val filesToExport: List<OutputFile> = OutputFile.entries.toList(),
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (exportInterval.value <= 0.0) add(ValidationIssue("exportInterval", "must be greater than zero"))
            if (printFrequency != null && printFrequency <= 0) {
                add(ValidationIssue("printFrequency", "must be greater than zero"))
            }
        }
}
