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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Selects which columns of an output file are written. */
@Serializable
public sealed interface ColumnSelection

/** Writes every available column. */
@Serializable
@SerialName("all")
public data object AllColumns : ColumnSelection

/**
 * Writes only the named columns.
 *
 * @property columns Names of the columns to keep.
 */
@Serializable
@SerialName("only")
public data class OnlyColumns(public val columns: Set<String>) : ColumnSelection

/**
 * Per-output-file column selections.
 *
 * @property host Column selection for host output.
 * @property task Column selection for task output.
 * @property powerSource Column selection for power source output.
 * @property battery Column selection for battery output.
 * @property service Column selection for service output.
 */
@Serializable
public data class ExportColumnsSpec(
    public val host: ColumnSelection = AllColumns,
    public val task: ColumnSelection = AllColumns,
    public val powerSource: ColumnSelection = AllColumns,
    public val battery: ColumnSelection = AllColumns,
    public val service: ColumnSelection = AllColumns,
)
