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

package org.opendc.experiments.serverless.trace

import mu.KotlinLogging
import java.io.File
import kotlin.math.max

/**
 * A trace reader for the serverless workload trace used in the OpenDC Serverless thesis.
 */
public class ServerlessTraceReader {
    /**
     * The logger for this portfolio instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * Parse the traces at the specified [path].
     */
    public fun parse(path: File): List<FunctionTrace> {
        return if (path.isFile) {
            listOf(parseSingle(path))
        } else {
            path.walk()
                .filterNot { it.isDirectory }
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
        val id = path.nameWithoutExtension
        var idx = 0

        var timestampCol = 0
        var invocationsCol = 0
        var execTimeCol = 0
        var provCpuCol = 0
        var provMemCol = 0
        var cpuUsageCol = 0
        var memoryUsageCol = 0
        var maxMemory = 0

        path.forEachLine { line ->
            if (line.startsWith("#") && line.isNotBlank()) {
                return@forEachLine
            }

            val values = line.split(",")

            /* Header parsing */
            if (idx++ == 0) {
                val header = values.mapIndexed { col, name -> Pair(name.trim(), col) }.toMap()
                timestampCol = header["Timestamp [ms]"]!!
                invocationsCol = header["Invocations"]!!
                execTimeCol = header["Avg Exec time per Invocation"]!!
                provCpuCol = header["Provisioned CPU [Mhz]"]!!
                provMemCol = header["Provisioned Memory [mb]"]!!
                cpuUsageCol = header["Avg cpu usage per Invocation [Mhz]"]!!
                memoryUsageCol = header["Avg mem usage per Invocation [mb]"]!!
                return@forEachLine
            }

            val timestamp = values[timestampCol].trim().toLong()
            val invocations = values[invocationsCol].trim().toInt()
            val execTime = values[execTimeCol].trim().toLong()
            val provisionedCpu = values[provCpuCol].trim().toInt()
            val provisionedMemory = values[provMemCol].trim().toInt()
            val cpuUsage = values[cpuUsageCol].trim().toDouble()
            val memoryUsage = values[memoryUsageCol].trim().toDouble()

            maxMemory = max(maxMemory, provisionedMemory)

            samples.add(FunctionSample(timestamp, execTime, invocations, provisionedCpu, provisionedMemory, cpuUsage, memoryUsage))
        }

        return FunctionTrace(id, maxMemory, samples)
    }
}
