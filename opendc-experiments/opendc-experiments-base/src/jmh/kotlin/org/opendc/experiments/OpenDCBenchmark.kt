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

private fun List<Double>.std(): Double {
    val avg = average()
    return sqrt(sumOf { (it - avg) * (it - avg) } / size)
}

abstract class OpenDCBenchmark {

    private var recording: Recording? = null
    private val jfrPath = Path.of("build/bench.jfr")
    private val heapResults = mutableListOf<HeapStats>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            recording?.stop()
            // Force-exit after a short grace period so non-daemon simulation threads
            // don't keep the forked JVM alive after IntelliJ/Gradle cancels the task.
            Thread { Thread.sleep(2_000); Runtime.getRuntime().halt(0) }
                .also { it.isDaemon = true }
                .start()
        })
    }

    @Setup(Level.Iteration)
    fun setupIteration() {
        recording = Recording(Configuration.getConfiguration("profile"))
        recording!!.setDestination(jfrPath)
        recording!!.start()
    }

    @TearDown(Level.Iteration)
    fun tearDownIteration(params: IterationParams) {
        recording?.stop()
        recording = null
        if (params.type == IterationType.MEASUREMENT) {
            analyzeHeap(jfrPath)?.let { heapResults.add(it) }
        }
    }

    @TearDown(Level.Trial)
    fun tearDownTrial(params: BenchmarkParams) {
        if (heapResults.isEmpty()) return

        val avgs = heapResults.map { it.avgMb }
        val maxes = heapResults.map { it.maxMb }

        val line = "\"${params.benchmark}\"," +
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