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

package org.opendc.sdk.runner
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

/**
 * How often to sample `jdk.ResidentSetSize` (process RSS). The `profile` config defaults to ~1 s, which
 * gives only a few samples per iteration; 50 ms yields a distribution dense enough to plot. RSS reads are
 * cheap (one OS call), so the overhead is negligible.
 */
private val RSS_SAMPLE_PERIOD: java.time.Duration = java.time.Duration.ofMillis(50)

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
 * **Memory statistics** — after each *measurement* iteration the JFR file is processed for two signals:
 * heap live-set (`heapUsed` from `"After GC"` `jdk.GCHeapSummary` events) and process RSS (`jdk.ResidentSetSize`).
 * At the end of the trial, per-iteration [MemoryStats] for each signal are aggregated (mean ± std-dev of both
 * the per-iteration average and peak), and the full per-sample distributions are written to
 * `build/heap-distribution.csv` and `build/rss-distribution.csv`. The jmh task configuration in
 * [build.gradle.kts](../build.gradle.kts) merges the summary into the final JSON report as a `memoryMetric`
 * field for each benchmark entry.
 *
 * ### Subclassing
 * Concrete benchmark classes should:
 * 1. Annotate the class with the desired JMH mode/time-unit annotations. See [CIBenchmark] as example.
 * 2. Implement one or more `@Benchmark` methods.
 * 3. Add any extra `@Setup` / `@TearDown` methods as needed; the lifecycle callbacks
 *    defined here run at [Level.Iteration] and [Level.Trial] respectively.
 *
 * @see analyzeMemory
 * @see MemoryStats
 */
abstract class OpenDCBenchmark {
    /** Active JFR recording for the current iteration, or `null` between iterations. */
    private var recording: Recording? = null

    /** Destination path for the JFR file; overwritten on every iteration. */
    private val jfrPath = Path.of("build/bench.jfr")

    /** Accumulated memory statistics, one entry per completed measurement iteration. */
    private val memoryResults = mutableListOf<IterationMemory>()

    /**
     * Starts a JFR recording before each iteration.
     *
     * Uses the JDK's built-in `profile` configuration, which captures GC heap
     * summaries, CPU load, thread activity, and other standard events needed for
     * heap analysis. The `jdk.ResidentSetSize` period is overridden to
     * [RSS_SAMPLE_PERIOD], since the profile default (~1 s) yields only a handful
     * of RSS samples per iteration — too coarse for a useful distribution.
     */
    @Setup(Level.Iteration)
    fun setupIteration() {
        recording =
            Recording(Configuration.getConfiguration("profile")).apply {
                enable("jdk.ResidentSetSize").withPeriod(RSS_SAMPLE_PERIOD)
                destination = jfrPath
                start()
            }
    }

    /**
     * Stops the JFR recording after each iteration and, for measurement iterations,
     * parses the resulting file into an [IterationMemory].
     *
     * Warmup iterations are skipped so that only steady-state behaviour is
     * included in the final report.
     *
     * @param params JMH-injected iteration metadata; used to distinguish warmup
     *   from measurement iterations via [IterationParams.type].
     */
    @TearDown(Level.Iteration)
    fun tearDownIteration(params: IterationParams) {
        recording?.stop()
        recording = null
        if (params.type == IterationType.MEASUREMENT) {
            memoryResults.add(analyzeMemory(jfrPath))
        }
    }

    /**
     * Aggregates memory statistics across all measurement iterations and appends a
     * summary row to `build/memory-stats.csv`, plus the pooled per-sample distributions
     * to `build/heap-distribution.csv` and `build/rss-distribution.csv`.
     *
     * The summary CSV columns are:
     * `benchmark,` then for heap and then for rss: `avg_mb, std_avg_mb, peak_mb, std_peak_mb`.
     * A signal with no samples is written as `NaN` in its four columns.
     *
     * If no measurement iterations produced any data, the method returns without writing anything.
     *
     * @param params JMH-injected trial metadata; provides the fully-qualified
     *   benchmark name used as the first CSV column.
     */
    @TearDown(Level.Trial)
    fun tearDownTrial(params: BenchmarkParams) {
        if (memoryResults.isEmpty()) return

        // Per-signal summary: mean and peak are first reduced within each iteration, then the
        // per-iteration values are averaged across iterations (with a std to show run-to-run spread).
        fun summarize(signal: (IterationMemory) -> MemoryStats?): String {
            val stats = memoryResults.mapNotNull(signal)
            if (stats.isEmpty()) return "NaN,NaN,NaN,NaN"
            val avgs = stats.map { it.avgMb }
            val peaks = stats.map { it.peakMb }
            return "${"%.4f".format(avgs.average())}," +
                "${"%.4f".format(avgs.std())}," +
                "${"%.4f".format(peaks.average())}," +
                "${"%.4f".format(peaks.std())}"
        }

        val line =
            "\"${params.benchmark}\"," +
                "${summarize { it.heap }}," +
                "${summarize { it.rss }}\n"

        println(line)

        val statsFile = File("build/memory-stats.csv")
        statsFile.parentFile.mkdirs()
        statsFile.appendText(line)

        // Pooled per-sample distributions across all measurement iterations, so the shape (not just the
        // peak/average) can be plotted. One row per sample: benchmark, value in MB.
        writeDistribution(File("build/heap-distribution.csv"), params.benchmark) { it.heap }
        writeDistribution(File("build/rss-distribution.csv"), params.benchmark) { it.rss }
    }

    private fun writeDistribution(
        file: File,
        benchmark: String,
        signal: (IterationMemory) -> MemoryStats?,
    ) {
        val samples = memoryResults.mapNotNull(signal).flatMap { it.samplesMb }
        if (samples.isEmpty()) return
        file.parentFile.mkdirs()
        file.appendText(samples.joinToString("") { "\"$benchmark\",${"%.4f".format(it)}\n" })
    }
}
