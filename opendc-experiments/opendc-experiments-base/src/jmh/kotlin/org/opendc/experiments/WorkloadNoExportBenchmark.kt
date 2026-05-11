package org.opendc.experiments

import org.opendc.experiments.base.runner.ExperimentCommand
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class WorkloadNoExportBenchmark : OpenDCBenchmark() {

    @Benchmark
    fun surfWeekBenchmark() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScalingNoExport/surf_week.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun surfMonthBenchmark() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScalingNoExport/surf_month.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun surfHalfYearBenchmark() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScalingNoExport/surf_halfyear.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun marconiBenchmark() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScalingNoExport/marconi.json"))
        File("output").deleteRecursively()
    }

//    @Benchmark
//    fun borgBenchmark() {
//        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScalingNoExport/borg.json"))
//        File("output").deleteRecursively()
//    }

    @Benchmark
    fun solvinityBenchmark() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/workloadScalingNoExport/solvinity.json"))
        File("output").deleteRecursively()
    }
}
