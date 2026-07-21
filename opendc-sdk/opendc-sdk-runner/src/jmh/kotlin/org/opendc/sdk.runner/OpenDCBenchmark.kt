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

package org.opendc.cli

import jdk.jfr.consumer.RecordingFile
import java.nio.file.Path
import kotlin.math.sqrt

private const val BYTES_PER_MB = 1024.0 * 1024.0

/**
 * Distribution of a single memory signal over one JMH iteration.
 *
 * All values are megabytes.
 *
 * @property avgMb Mean across all samples.
 * @property peakMb Largest single sample.
 * @property stdMb Population standard deviation across all samples.
 * @property sampleCount Number of samples.
 * @property samplesMb Every sample, so callers can pool them into a histogram.
 */
data class MemoryStats(
    val avgMb: Double,
    val peakMb: Double,
    val stdMb: Double,
    val sampleCount: Int,
    val samplesMb: List<Double>,
) {
    companion object {
        /**
         * Build stats from raw byte samples, or `null` if there are none.
         */
        fun of(bytes: List<Long>): MemoryStats? {
            if (bytes.isEmpty()) return null
            val avg = bytes.average()
            val peak = bytes.max().toDouble()
            val std = sqrt(bytes.sumOf { (it - avg).let { d -> d * d } } / bytes.size)
            return MemoryStats(
                avgMb = avg / BYTES_PER_MB,
                peakMb = peak / BYTES_PER_MB,
                stdMb = std / BYTES_PER_MB,
                sampleCount = bytes.size,
                samplesMb = bytes.map { it / BYTES_PER_MB },
            )
        }
    }
}

/**
 * The two memory signals captured for one JMH iteration.
 *
 * @property heap Retained live set: `heapUsed` from `jdk.GCHeapSummary` events tagged `"After GC"`.
 *   Reflects the bytes the application's objects actually keep, and so tracks changes to object sizes
 *   directly. `null` if no GC completed during the iteration.
 * @property rss Process resident set: `size` from `jdk.ResidentSetSize` events. The RAM the JVM process
 *   holds from the OS as a whole (heap + metaspace + code cache + thread stacks + off-heap). Sticky — the
 *   JVM rarely returns memory — so it tracks the high-water mark rather than the current live set. `null`
 *   if the JDK emitted no such events.
 */
data class IterationMemory(
    val heap: MemoryStats?,
    val rss: MemoryStats?,
)

/**
 * Parse a JFR recording into its per-iteration memory distributions.
 *
 * Heap samples are filtered to `"After GC"` `jdk.GCHeapSummary` events so that each value is the live set
 * *after* a collection, not the pre-GC occupancy (which includes all garbage allocated since the previous
 * GC and is therefore dominated by allocation rate rather than retained size).
 *
 * @param jfrPath Path to the `.jfr` recording to analyze.
 */
fun analyzeMemory(jfrPath: Path): IterationMemory {
    val heapBytes = mutableListOf<Long>()
    val rssBytes = mutableListOf<Long>()

    RecordingFile(jfrPath).use { recording ->
        while (recording.hasMoreEvents()) {
            val event = recording.readEvent()
            when (event.eventType.name) {
                "jdk.GCHeapSummary" ->
                    if (event.getString("when") == "After GC") {
                        heapBytes.add(event.getLong("heapUsed"))
                    }
                "jdk.ResidentSetSize" -> rssBytes.add(event.getLong("size"))
            }
        }
    }

    return IterationMemory(MemoryStats.of(heapBytes), MemoryStats.of(rssBytes))
}

/**
 * Standalone entry point for inspecting the heap statistics of an existing JFR file.
 *
 * Reads `build/bench.jfr` (the default output path used by [OpenDCBenchmark]) and
 * prints a human-readable summary to stdout. Useful for quickly inspecting the
 * recording from the most recently completed benchmark iteration without re-running
 * the full benchmark.
 *
 * Exits with an error if the file contains no `jdk.GCHeapSummary` events.
 */
fun main() {
    val path = Path.of("build/bench.jfr")
    val memory = analyzeMemory(path)

    fun report(
        name: String,
        stats: MemoryStats?,
    ) {
        if (stats == null) {
            println("$name: no samples in $path")
            return
        }
        println("$name from $path (${stats.sampleCount} samples):")
        println("  Avg:  ${"%.2f".format(stats.avgMb)} MB")
        println("  Peak: ${"%.2f".format(stats.peakMb)} MB")
        println("  Std:  ${"%.2f".format(stats.stdMb)} MB")
    }

    report("Heap live-set (after GC)", memory.heap)
    report("Process RSS", memory.rss)
}
