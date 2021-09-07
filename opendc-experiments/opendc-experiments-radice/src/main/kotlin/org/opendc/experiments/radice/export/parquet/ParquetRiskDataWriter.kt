/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.experiments.radice.export.parquet

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecordBuilder
import org.opendc.compute.workload.export.parquet.ParquetDataWriter
import org.opendc.experiments.radice.export.RiskData
import org.opendc.trace.util.parquet.TIMESTAMP_SCHEMA
import java.io.File

/**
 * A writer for risk reports in a Parquet format.
 *
 * @param path The path to write the data to.
 */
class ParquetRiskDataWriter(path: File) : ParquetDataWriter<RiskData>(path, SCHEMA) {
    override fun convert(builder: GenericRecordBuilder, data: RiskData) {
        val factor = data.factor
        val point = data.point

        builder["timestamp"] = point.timestamp.toEpochMilli()
        builder["id"] = factor.id
        builder["value"] = point.value
        builder["cost"] = data.cost
    }

    override fun toString(): String = "risk-writer"

    private companion object {
        private val SCHEMA: Schema = SchemaBuilder
            .record("risk")
            .namespace("org.opendc.experiments.radice")
            .fields()
            .name("timestamp").type(TIMESTAMP_SCHEMA).noDefault()
            .requiredString("id")
            .requiredDouble("value")
            .requiredDouble("cost")
            .endRecord()
    }
}
