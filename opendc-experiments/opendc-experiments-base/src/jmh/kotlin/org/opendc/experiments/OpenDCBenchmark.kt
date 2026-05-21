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

import jdk.jfr.Configuration
import jdk.jfr.Recording
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.infra.BenchmarkParams
import org.openjdk.jmh.infra.IterationParams
import org.openjdk.jmh.runner.IterationType
import java.io.File
import java.nio.file.Path
import kotlin.math.sqrt

/** Computes the population standard deviation of the receiver list. */
private fun List<Double>.std(): Double {
    val avg = average()
    return sqrt(sumOf { (it - avg) * (it - avg) } / size)
}

/**
 * Abstract base class for JMH benchmarks in OpenDC.
 *
 * This base class provides:
 *
 * **JFR profiling** — a Java Flight Recorder session using the built-in `profile`
 * configuration is started before every JMH iteration and stopped afterwards.
 * The recording is written to `build/bench.jfr` and overwritten each iteration,
 * so only the most-recent recording is kept on disk.
 *
 * **Heap statistics** — after each *measurement* iteration the JFR file is processed
 * to get the average and peak heap usage. At the end of the trial, per-iteration [HeapStats]
 * are aggregated (mean ± std-dev). The jmh task configuration in [build.gradle.kts](../build.gradle.kts)
 * is set up to merge these heap stats into the final JSON report as a
 * `heapMetric` field for each benchmark entry.
 *
 * ### Subclassing
 * Concrete benchmark classes should:
 * 1. Annotate the class with the desired JMH mode/time-unit annotations. See [CIBenchmark] as example.
 * 2. Implement one or more `@Benchmark` methods.
 * 3. Add any extra `@Setup` / `@TearDown` methods as needed; the lifecycle callbacks
 *    defined here run at [Level.Iteration] and [Level.Trial] respectively.
 *
 * @see analyzeHeap
 * @see HeapStats
 */
abstract class OpenDCBenchmark {
    /** Active JFR recording for the current iteration, or `null` between iterations. */
    private var recording: Recording? = null

    /** Destination path for the JFR file; overwritten on every iteration. */
    private val jfrPath = Path.of("build/bench.jfr")

    /** Accumulated heap statistics, one entry per completed measurement iteration. */
    private val heapResults = mutableListOf<HeapStats>()

    /**
     * Starts a JFR recording before each iteration.
     *
     * Uses the JDK's built-in `profile` configuration, which captures GC heap
     * summaries, CPU load, thread activity, and other standard events needed for
     * heap analysis.
     */
    @Setup(Level.Iteration)
    fun setupIteration() {
        recording = Recording(Configuration.getConfiguration("profile"))
        recording!!.setDestination(jfrPath)
        recording!!.start()
    }

    /**
     * Stops the JFR recording after each iteration and, for measurement iterations,
     * parses the resulting file to extract [HeapStats].
     *
     * Warmup iterations are skipped so that only steady-state heap behaviour is
     * included in the final CSV report.
     *
     * @param params JMH-injected iteration metadata; used to distinguish warmup
     *   from measurement iterations via [IterationParams.type].
     */
    @TearDown(Level.Iteration)
    fun tearDownIteration(params: IterationParams) {
        recording?.stop()
        recording = null
        if (params.type == IterationType.MEASUREMENT) {
            analyzeHeap(jfrPath)?.let { heapResults.add(it) }
        }
    }

    /**
     * Aggregates heap statistics across all measurement iterations and appends a
     * summary row to `build/heap-stats.csv`.
     *
     * The CSV columns are:
     * `benchmark, avg_heap_mb, std_avg_heap_mb, max_heap_mb, std_max_heap_mb`
     *
     * If no measurement iterations produced heap data (e.g., the JFR file was
     * empty), the method returns without writing anything.
     *
     * @param params JMH-injected trial metadata; provides the fully-qualified
     *   benchmark name used as the first CSV column.
     */
    @TearDown(Level.Trial)
    fun tearDownTrial(params: BenchmarkParams) {
        if (heapResults.isEmpty()) return

        val avgs = heapResults.map { it.avgMb }
        val maxes = heapResults.map { it.maxMb }

        val line =
            "\"${params.benchmark}\"," +
                "${"%.4f".format(avgs.average())}," +
                "${"%.4f".format(avgs.std())}," +
                "${"%.4f".format(maxes.average())}," +
                "${"%.4f".format(maxes.std())}\n"

        println(line)

        val heapFile = File("build/heap-stats.csv")
        heapFile.parentFile.mkdirs()
        heapFile.appendText(line)
    }
}
