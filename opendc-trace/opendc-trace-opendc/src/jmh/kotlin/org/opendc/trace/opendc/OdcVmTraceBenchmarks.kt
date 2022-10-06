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

package org.opendc.trace.opendc

import org.opendc.trace.conv.INTERFERENCE_GROUP_SCORE
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.TABLE_INTERFERENCE_GROUPS
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.spi.TraceFormat
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Benchmarks for parsing traces in the OpenDC vm format.
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
class OdcVmTraceBenchmarks {
    private lateinit var path: Path
    private lateinit var format: TraceFormat

    @Setup
    fun setUp() {
        path = Path.of("../../opendc-experiments/opendc-experiments-capelin/src/test/resources/trace/bitbrains-small")
        format = OdcVmTraceFormat()
    }

    @Benchmark
    fun benchmarkResourcesReader(bh: Blackhole) {
        val reader = format.newReader(path, TABLE_RESOURCES, null)
        try {
            val idColumn = reader.resolve(RESOURCE_ID)
            while (reader.nextRow()) {
                bh.consume(reader.getString(idColumn))
            }
        } finally {
            reader.close()
        }
    }

    @Benchmark
    fun benchmarkResourceStatesReader(bh: Blackhole) {
        val reader = format.newReader(path, TABLE_RESOURCE_STATES, null)
        try {
            val idColumn = reader.resolve(RESOURCE_ID)
            while (reader.nextRow()) {
                bh.consume(reader.getString(idColumn))
            }
        } finally {
            reader.close()
        }
    }

    @Benchmark
    fun benchmarkInterferenceGroupReader(bh: Blackhole) {
        val reader = format.newReader(path, TABLE_INTERFERENCE_GROUPS, null)
        try {
            val scoreColumn = reader.resolve(INTERFERENCE_GROUP_SCORE)
            while (reader.nextRow()) {
                bh.consume(reader.getDouble(scoreColumn))
            }
        } finally {
            reader.close()
        }
    }
}
