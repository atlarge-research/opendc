/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.trace.formats.failure.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.InitContext
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Types
import org.opendc.trace.conv.FAILURE_DURATION
import org.opendc.trace.conv.FAILURE_INTENSITY
import org.opendc.trace.conv.FAILURE_START

/**
 * A [ReadSupport] instance for [Task] objects.
 *
 * @param projection The projection of the table to read.
 */
internal class FailureReadSupport(private val projection: List<String>?) : ReadSupport<FailureFragment>() {
    /**
     * Mapping of table columns to their Parquet column names.
     */
    private val colMap =
        mapOf(
            FAILURE_START to "start",
            FAILURE_DURATION to "duration",
            FAILURE_INTENSITY to "intensity"
        )

    override fun init(context: InitContext): ReadContext {
        val projectedSchema =
            if (projection != null) {
                Types.buildMessage()
                    .apply {
                        val fieldByName = READ_SCHEMA.fields.associateBy { it.name }

                        for (col in projection) {
                            val fieldName = colMap[col] ?: continue
                            addField(fieldByName.getValue(fieldName))
                        }
                    }
                    .named(READ_SCHEMA.name)
            } else {
                READ_SCHEMA
            }
        return ReadContext(projectedSchema)
    }

    override fun prepareForRead(
        configuration: Configuration,
        keyValueMetaData: Map<String, String>,
        fileSchema: MessageType,
        readContext: ReadContext,
    ): RecordMaterializer<FailureFragment> = FailureRecordMaterializer(readContext.requestedSchema)

    companion object {
        /**
         * Parquet read schema for the "tasks" table in the trace.
         */
        @JvmStatic
        val READ_SCHEMA: MessageType =
            Types.buildMessage()
                .addFields(
                    Types
                        .optional(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("failure_start"),
                    Types
                        .optional(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("failure_duration"),
                    Types
                        .optional(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("failure_intensity"),
                )
                .named("failure_fragment")
    }
}
