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

package org.opendc.sdk.python

import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.Type
import org.opendc.trace.formats.carbon.parquet.CARBON_SCHEMA
import org.opendc.trace.formats.failure.parquet.FAILURE_SCHEMA
import org.opendc.trace.formats.workload.parquet.FRAGMENT_SCHEMA
import org.opendc.trace.formats.workload.parquet.TASK_SCHEMA
import org.opendc.trace.parquet.LocalOutputFile
import java.nio.file.Path

/**
 * The tables of the input traces, with the parquet [schema] their readers request and the [required] columns they need a
 * value in on every row. Python casts a DataFrame to this schema before it writes a table, so it never restates one.
 */
enum class TraceTable(val schema: MessageType, val required: List<String>) {
    TASK(TASK_SCHEMA, requiredFieldsOf(TASK_SCHEMA)),
    FRAGMENT(FRAGMENT_SCHEMA, requiredFieldsOf(FRAGMENT_SCHEMA)),

    // The readers of these two use every column, although their schemas mark each optional
    CARBON(CARBON_SCHEMA, CARBON_SCHEMA.fields.map { it.name }),
    FAILURE(FAILURE_SCHEMA, FAILURE_SCHEMA.fields.map { it.name }),
    ;

    /** Writes a parquet file with no rows at [target], from which pyarrow reads the schema. */
    fun writeEmpty(target: Path) {
        ExampleParquetWriter.builder(LocalOutputFile(target)).withType(schema).build().close()
    }
}

private fun requiredFieldsOf(schema: MessageType): List<String> =
    schema.fields.filter { it.isRepetition(Type.Repetition.REQUIRED) }.map { it.name }
