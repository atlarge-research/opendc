/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.capelin.telemetry.parquet

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.opendc.experiments.capelin.telemetry.RunEvent
import java.io.File

/**
 * A Parquet event writer for [RunEvent]s.
 */
public class ParquetRunEventWriter(path: File, bufferSize: Int) :
    ParquetEventWriter<RunEvent>(path, schema, convert, bufferSize) {

    override fun toString(): String = "run-writer"

    public companion object {
        private val convert: (RunEvent, GenericData.Record) -> Unit = { event, record ->
            val portfolio = event.portfolio
            record.put("portfolio_name", portfolio.name)
            record.put("scenario_id", portfolio.id)
            record.put("run_id", event.repeat)
            record.put("topology", portfolio.topology.name)
            record.put("workload_name", portfolio.workload.name)
            record.put("workload_fraction", portfolio.workload.fraction)
            record.put("workload_sampler", portfolio.workload.samplingStrategy)
            record.put("allocation_policy", portfolio.allocationPolicy)
            record.put("failure_frequency", portfolio.operationalPhenomena.failureFrequency)
            record.put("interference", portfolio.operationalPhenomena.hasInterference)
            record.put("seed", event.repeat)
        }

        private val schema: Schema = SchemaBuilder
            .record("runs")
            .namespace("org.opendc.experiments.sc20")
            .fields()
            .name("portfolio_name").type().stringType().noDefault()
            .name("scenario_id").type().intType().noDefault()
            .name("run_id").type().intType().noDefault()
            .name("topology").type().stringType().noDefault()
            .name("workload_name").type().stringType().noDefault()
            .name("workload_fraction").type().doubleType().noDefault()
            .name("workload_sampler").type().stringType().noDefault()
            .name("allocation_policy").type().stringType().noDefault()
            .name("failure_frequency").type().doubleType().noDefault()
            .name("interference").type().booleanType().noDefault()
            .name("seed").type().intType().noDefault()
            .endRecord()
    }
}
