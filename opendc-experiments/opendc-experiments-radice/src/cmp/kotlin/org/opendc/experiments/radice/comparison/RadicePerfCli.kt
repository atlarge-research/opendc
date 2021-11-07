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

@file:JvmName("RadicePerfCli")
package org.opendc.experiments.radice.comparison

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.typesafe.config.ConfigFactory
import me.tongfei.progressbar.ProgressBarBuilder
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.opendc.compute.workload.ComputeWorkload
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.sampleByLoad
import org.opendc.compute.workload.sampleBySize
import org.opendc.experiments.radice.comparison.engine.CloudSimPlusEngine
import org.opendc.experiments.radice.comparison.engine.ExperimentResult
import org.opendc.experiments.radice.comparison.engine.OpenDCEngine
import org.opendc.experiments.radice.comparison.export.PerformanceData
import org.opendc.experiments.radice.comparison.export.parquet.ParquetPerformanceDataWriter
import org.opendc.experiments.radice.scenario.*
import org.opendc.experiments.radice.scenario.topology.*
import org.opendc.experiments.radice.util.ci
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.lang.management.MemoryUsage
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Main method of the application.
 */
fun main(args: Array<String>): Unit = RadicePerfCommand().main(args)

/**
 * Represents the command for the Radice performance comparison experiments.
 */
internal class RadicePerfCommand : CliktCommand(name = "radice-perf") {
    /**
     * The configuration for the experiments.
     */
    private val config = ConfigFactory.load().getConfig("opendc.experiments.radice")

    /**
     * The number of repeats.
     */
    private val repeats by option("-r", "--repeats", help = "number of repeats")
        .int()
        .default(16)

    /**
     * The number of warm-up iterations.
     */
    private val warmup by option("-w", "--warmup", help = "number of warm-up iterations")
        .int()
        .default(4)

    /**
     * The trace to run.
     */
    private val trace by option("--trace", help = "trace to run")
        .convert { WorkloadSpec(it) }
        .default(WorkloadSpec("baseline"))

    /**
     * The seed for seeding the random instances.
     */
    private val seed by option("-s", "--seed", help = "initial seed for randomness")
        .long()
        .default(0)

    /**
     * The topology to use.
     */
    private val topology by option("--topology", help = "topology to run")
        .choice(
            "base" to BASE_TOPOLOGY,
            "exp-vel-ver-het" to CAPELIN_EXP_VEL_VER_HET,
            "exp-vel-ver-hom" to CAPELIN_EXP_VEL_VER_HOM,
            "exp-vol-hor-het" to CAPELIN_EXP_VOL_HOR_HET,
            "exp-vol-hor-hom" to CAPELIN_EXP_VOL_HOR_HOM,
            "exp-vol-ver-het" to CAPELIN_EXP_VOL_VER_HET,
            "exp-vol-ver-hom" to CAPELIN_EXP_VOL_VER_HOM,
            "rep-vel-ver-het" to CAPELIN_REP_VEL_VER_HET,
            "rep-vel-ver-hom" to CAPELIN_REP_VEL_VER_HOM,
            "rep-vol-hor-het" to CAPELIN_REP_VOL_HOR_HET,
            "rep-vol-hor-hom" to CAPELIN_REP_VOL_HOR_HOM,
            "rep-vol-ver-com" to CAPELIN_REP_VOL_VER_COM,
            "rep-vol-ver-het" to CAPELIN_REP_VOL_VER_HET,
            "rep-vol-ver-hom" to CAPELIN_REP_VOL_VER_HOM,
        )
        .default(BASE_TOPOLOGY)

    /**
     * The scale of the topology.
     */
    private val topologyScale by option("--topology-scale", help = "scale of topology")
        .double()
        .default(1.0)

    /**
     * The sampling options.
     */
    private val samplingOptions by SamplingOptions().cooccurring()

    /**
     * The simulation engine to use.
     */
    private val engine by option("-e", "--engine", help = "engine")
        .choice(
            "cloudsim-plus" to CloudSimPlusEngine(),
            "opendc" to OpenDCEngine(),
        )
        .required()

    /**
     * The partitions to use for the invocation
     */
    private val partitions: Map<String, String> by option("-P", "--partitions").associate()

    /**
     * The [ComputeWorkloadLoader] to use for loading the traces.
     */
    private val workloadLoader = ComputeWorkloadLoader(File(config.getString("trace-path")))

    /**
     * Disable writing output.
     */
    private val disableOutput by option("--disable-output", help = "disable output").flag()

    override fun run() {
        val engine = engine
        val pb = ProgressBarBuilder()
            .setInitialMax(warmup.toLong())
            .setTaskName("Simulating...")
            .build()
        var pbIsClosed = false

        val writer = if (!disableOutput) {
            val partition = partitions.map { (k, v) -> "$k=$v" }.joinToString("/")
            val outputDir = File(config.getString("output-path"), "perf/$partition/engine=${engine.id}")
            outputDir.mkdirs()
            ParquetPerformanceDataWriter(outputDir.resolve("data.parquet"))
        } else {
            null
        }

        val seed = seed
        val samplingOptions = samplingOptions
        val workload = samplingOptions?.strategy?.invoke(trace.source) ?: trace.source
        val random = Random(seed)
        val topology = topology.scale(topology.id, topologyScale)

        try {
            val runtimeStats = DescriptiveStatistics()
            val memStats = DescriptiveStatistics()
            val powerStats = DescriptiveStatistics()

            val memoryPools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }

            echo("Starting $warmup warm-up experiments")

            repeat(warmup) { warmup ->
                engine.runScenario(workload.resolve(workloadLoader, random), topology, seed - warmup - 1)
                pb.step()
            }

            echo("Starting $repeats experiments")
            pb.stepTo(0)
            pb.maxHint(repeats.toLong())

            repeat(repeats) { repeat ->
                val vms = workload.resolve(workloadLoader, random)

                // Perform garbage collection before starting the actual measurements
                System.gc()

                // Reset the peak memory usage to measure only the peak memory usage induced by running the replication
                for (pool in memoryPools) {
                    pool.resetPeakUsage()
                }

                val result: ExperimentResult
                val duration = measureTimeMillis {
                    result = engine.runScenario(vms, topology, seed + repeat)
                }

                // Perform garbage collection before collecting memory statistics
                System.gc()

                val memPeakUsage = memoryPools.reduce { it.peakUsage }
                val memUsage = memoryPools.reduce { it.usage }
                val perf = PerformanceData(repeat, duration, memUsage, memPeakUsage)
                writer?.write(perf)

                runtimeStats.addValue(duration.toDouble())
                memStats.addValue(perf.memoryPeakUsage.used / 1_000_000.0)

                val totalPower = result.powerConsumption.values.sum()
                powerStats.addValue(totalPower / 3_600_000)

                pb.step()
                pb.extraMessage = "Mem Usage: %.2f MB ± %.2f".format(memStats.mean, memStats.ci(0.95))
            }

            // Close ProgressBar before outputting results, so they are not hidden behind the progress bar
            pb.close()
            pbIsClosed = true

            echo("Runtime: %.2f ms ± %.2f".format(runtimeStats.mean, runtimeStats.ci(0.95)))
            echo("Memory Usage: %.2f MB ± %.2f".format(memStats.mean, memStats.ci(0.95)))
            echo("Electricity Usage: %.2f kWh ± %.2f".format(powerStats.mean, powerStats.ci(0.95)))
        } finally {
            if (!pbIsClosed) {
                pb.close()
            }
            writer?.close()
        }
    }

    /**
     * Options for sampling the workload trace.
     */
    private class SamplingOptions : OptionGroup() {
        /**
         * The fraction of VMs to sample
         */
        val fraction by option("--sampling-fraction", help = "fraction of the workload to sample")
            .double()
            .restrictTo(0.0001, 1.0)
            .default(1.0)

        /**
         * The strategy for sampling the trace.
         */
        val strategy by option("--sampling-strategy", help = "strategy for sampling the workload")
            .choice(
                "load" to { workload: ComputeWorkload -> workload.sampleByLoad(fraction) },
                "size" to { it.sampleBySize(fraction) },
            )
            .required()
    }

    /**
     * Reduce the specified list of [MemoryUsage] objects into a single [MemoryUsage] object.
     */
    private inline fun <T> Collection<T>.reduce(transform: (T) -> MemoryUsage): MemoryUsage {
        var init = 0L
        var used = 0L
        var committed = 0L
        var max = 0L

        for (value in this) {
            val usage = transform(value)
            init += usage.init
            used += usage.used
            committed += usage.committed
            max += usage.max
        }

        return MemoryUsage(init, used, committed, max)
    }
}
