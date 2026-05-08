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

package org.opendc.experiments

import jdk.jfr.consumer.RecordingFile
import java.nio.file.Path
import kotlin.math.sqrt

/**
 * Heap usage statistics derived from a single JFR recording.
 *
 * All memory values are in megabytes and are computed from the `heapUsed` field
 * of `jdk.GCHeapSummary` events captured during one JMH iteration.
 *
 * @property avgMb Mean heap usage across all GC samples in the recording (MB).
 * @property maxMb Peak heap usage observed in any single GC sample (MB).
 * @property stdMb Population standard deviation of heap usage across all samples (MB).
 * @property sampleCount Number of `jdk.GCHeapSummary` events found in the recording.
 */
data class HeapStats(
    val avgMb: Double,
    val maxMb: Double,
    val stdMb: Double,
    val sampleCount: Int,
)

/**
 * Parses a JFR recording file and computes heap usage statistics from its
 * `jdk.GCHeapSummary` events.
 *
 * Each `jdk.GCHeapSummary` event is emitted by the JVM after a GC cycle and
 * carries the `heapUsed` value (bytes). This function collects all such values,
 * then computes mean, peak, and standard deviation, converting the results from
 * bytes to megabytes.
 *
 * @param jfrPath Path to the `.jfr` recording file to analyze.
 * @return A [HeapStats] instance if at least one `jdk.GCHeapSummary` event was
 *   found, or `null` if the recording contained no such events (e.g., no GC
 *   occurred during the iteration).
 */
fun analyzeHeap(jfrPath: Path): HeapStats? {
    val samples = mutableListOf<Long>()

    RecordingFile(jfrPath).use { recording ->
        while (recording.hasMoreEvents()) {
            val event = recording.readEvent()
            if (event.eventType.name == "jdk.GCHeapSummary") {
                samples.add(event.getLong("heapUsed"))
            }
        }
    }

    if (samples.isEmpty()) return null

    val avg = samples.average()
    val max = samples.max().toDouble()
    val std = sqrt(samples.sumOf { (it - avg).let { d -> d * d } } / samples.size)
    val toMb = 1.0 / (1024.0 * 1024.0)

    return HeapStats(avg * toMb, max * toMb, std * toMb, samples.size)
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
    val stats = analyzeHeap(path) ?: error("No jdk.GCHeapSummary events found in $path")
    println("Heap statistics from $path (${stats.sampleCount} GC samples):")
    println("  Avg: ${"%.2f".format(stats.avgMb)} MB")
    println("  Max: ${"%.2f".format(stats.maxMb)} MB")
    println("  Std: ${"%.2f".format(stats.stdMb)} MB")
}
