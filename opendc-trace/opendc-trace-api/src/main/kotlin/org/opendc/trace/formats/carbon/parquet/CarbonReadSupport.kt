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

package org.opendc.trace.formats.carbon.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.InitContext
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.Types
import org.opendc.trace.conv.CARBON_INTENSITY
import org.opendc.trace.conv.CARBON_TIMESTAMP

/**
 * A [ReadSupport] instance for [CarbonFragment] objects.
 *
 * @param projection The projection of the table to read.
 */
internal class CarbonReadSupport(private val projection: List<String>?) : ReadSupport<CarbonFragment>() {
    /**
     * Mapping of table columns to their Parquet column names.
     */
    private val colMap =
        mapOf(
            CARBON_TIMESTAMP to "timestamp",
            CARBON_INTENSITY to "carbon_intensity",
        )

    override fun init(context: InitContext): ReadContext {
        val projectedSchema =
            if (projection != null) {
                Types.buildMessage()
                    .apply {
                        val fieldByName = CARBON_SCHEMA.fields.associateBy { it.name }

                        for (col in projection) {
                            val fieldName = colMap[col] ?: continue
                            addField(fieldByName.getValue(fieldName))
                        }
                    }
                    .named(CARBON_SCHEMA.name)
            } else {
                CARBON_SCHEMA
            }
        return ReadContext(projectedSchema)
    }

    override fun prepareForRead(
        configuration: Configuration,
        keyValueMetaData: Map<String, String>,
        fileSchema: MessageType,
        readContext: ReadContext,
    ): RecordMaterializer<CarbonFragment> = CarbonRecordMaterializer(readContext.requestedSchema)
}
