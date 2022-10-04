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

package org.opendc.experiments.faas

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.dataformat.csv.CsvFactory
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import mu.KotlinLogging
import java.io.File
import kotlin.math.max

/**
 * A trace reader for the serverless workload trace used in the OpenDC Serverless thesis.
 */
public class ServerlessTraceReader {
    /**
     * The logger for this trace reader instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The [CsvFactory] used to create the parser.
     */
    private val factory = CsvFactory()
        .enable(CsvParser.Feature.ALLOW_COMMENTS)
        .enable(CsvParser.Feature.TRIM_SPACES)

    /**
     * Parse the traces at the specified [path].
     */
    public fun parse(path: File): List<FunctionTrace> {
        return if (path.isFile) {
            listOf(parseSingle(path))
        } else {
            path.walk()
                .filterNot { it.isDirectory }
                .sorted()
                .map { file ->
                    logger.info { "Parsing $file" }
                    parseSingle(file)
                }
                .toList()
        }
    }

    /**
     * Parse a single trace.
     */
    private fun parseSingle(path: File): FunctionTrace {
        val samples = mutableListOf<FunctionSample>()

        val parser = factory.createParser(path)
        parser.schema = schema

        var id = ""
        var timestamp = 0L
        var invocations = 0
        var execTime = 0L
        var provisionedCpu = 0
        var provisionedMem = 0
        var cpuUsage = 0.0
        var memUsage = 0.0
        var maxMemory = 0

        while (!parser.isClosed) {
            val token = parser.nextValue()
            if (token == JsonToken.END_OBJECT) {
                maxMemory = max(maxMemory, provisionedMem)
                samples.add(FunctionSample(timestamp, execTime, invocations, provisionedCpu, provisionedMem, cpuUsage, memUsage))

                id = ""
                timestamp = 0
                invocations = 0
                execTime = 0
                provisionedCpu = 0
                provisionedMem = 0
                cpuUsage = 0.0
                memUsage = 0.0

                continue
            }

            when (parser.currentName) {
                "Timestamp [ms]" -> timestamp = parser.valueAsLong
                "Invocations" -> invocations = parser.valueAsInt
                "Avg Exec time per Invocation" -> execTime = parser.valueAsLong
                "Provisioned CPU [Mhz]" -> provisionedCpu = parser.valueAsInt
                "Provisioned Memory [mb]" -> provisionedMem = parser.valueAsInt
                "Avg cpu usage per Invocation [Mhz]" -> cpuUsage = parser.valueAsDouble
                "Avg mem usage per Invocation [mb]" -> memUsage = parser.valueAsDouble
                "name" -> id = parser.text
            }
        }

        return FunctionTrace(id, maxMemory, samples)
    }

    private companion object {
        /**
         * The [CsvSchema] that is used to parse the trace.
         */
        val schema = CsvSchema.builder()
            .addColumn("Timestamp [ms]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Invocations", CsvSchema.ColumnType.NUMBER)
            .addColumn("Avg Exec time per Invocation", CsvSchema.ColumnType.NUMBER)
            .addColumn("Provisioned CPU [Mhz]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Provisioned Memory [mb]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Avg cpu usage per Invocation [Mhz]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Avg mem usage per Invocation [mb]", CsvSchema.ColumnType.NUMBER)
            .addColumn("name", CsvSchema.ColumnType.STRING)
            .setAllowComments(true)
            .setUseHeader(true)
            .build()
    }
}
