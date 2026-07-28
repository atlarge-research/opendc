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

@file:Suppress("DEPRECATION")

package org.opendc.sdk.runner

import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.harness.createTopology
import org.opendc.sdk.runner.harness.runBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class TaskScalingBenchmark : OpenDCBenchmark() {
    fun generateWorkload(numTasks: Int): List<TaskSpec> {
        val submissionTime = "2022-01-01T00:00:00"
        val submitMs = LocalDateTime.parse(submissionTime).toInstant(ZoneOffset.UTC).toEpochMilli()
        val taskDuration = 10 * 60 * 1000
        val cpuUsage = 1000
        val gpuUsage = 0.0

        val workload = mutableListOf<TaskSpec>()
        for (i in 0 until numTasks) {
            workload.add(
                TaskSpec(
                    id = i,
                    name = "$i",
                    submissionTime = TimeDelta.ofMillis(submitMs),
                    duration = TimeDelta.ofMillis(taskDuration),
                    cpuCoreCount = 1,
                    cpuCapacity = Frequency.ofMHz(1000),
                    memory = DataSize.ofMiB(10000.0),
                    fragments =
                        listOf(
                            TaskFragmentSpec(
                                TimeDelta.ofMillis(taskDuration),
                                Frequency.ofMHz(cpuUsage),
                                Frequency.ofMHz(gpuUsage),
                            ),
                        ),
                ),
            )
        }

        return workload
    }

    fun generateTopology(numHosts: Int): List<ClusterSpec> {
    }

    @Benchmark
    fun testBenchmark() {
        val workload = generateWorkload(5000)

        println("Creating Topology")
        val topology = createTopology("batteries/experiment1.json")
        val monitor = runBenchmark(topology, workload)
        println("DONE")
    }

//    @Benchmark
//    fun surfMonthBenchmark() {
//        RunCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScaling/surf_month.json"))
//        File("output").deleteRecursively()
//    }
//
//    @Benchmark
//    fun surfHalfYearBenchmark() {
//        RunCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScaling/surf_halfyear.json"))
//        File("output").deleteRecursively()
//    }
}
