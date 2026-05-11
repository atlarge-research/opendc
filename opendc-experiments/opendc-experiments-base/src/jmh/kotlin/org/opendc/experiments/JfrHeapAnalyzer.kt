package org.opendc.experiments

import jdk.jfr.consumer.RecordingFile
import java.nio.file.Path
import kotlin.math.sqrt

data class HeapStats(
    val avgMb: Double,
    val maxMb: Double,
    val stdMb: Double,
    val sampleCount: Int,
)

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

fun main() {
    val path = Path.of("build/bench.jfr")
    val stats = analyzeHeap(path) ?: error("No jdk.GCHeapSummary events found in $path")
    println("Heap statistics from $path (${stats.sampleCount} GC samples):")
    println("  Avg: ${"%.2f".format(stats.avgMb)} MB")
    println("  Max: ${"%.2f".format(stats.maxMb)} MB")
    println("  Std: ${"%.2f".format(stats.stdMb)} MB")
}