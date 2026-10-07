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

package org.opendc.sdk.runner.bench

import jdk.jfr.Configuration
import jdk.jfr.Recording
import org.apache.parquet.hadoop.ParquetFileReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.mib
import org.opendc.sdk.model.dsl.scenario
import org.opendc.sdk.model.dsl.watts
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.telemetry.ExportColumnsSpec
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.telemetry.OnlyColumns
import org.opendc.sdk.model.topology.SqrtPowerModelSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.runner.executor.runScenario
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.MetricExporter
import org.opendc.sdk.runner.telemetry.parquet.ComputeExportConfig
import org.opendc.sdk.runner.telemetry.sink.OutputSink
import org.opendc.sdk.runner.telemetry.sink.ParquetSink
import org.opendc.sdk.runner.telemetry.sink.RunContext
import org.opendc.sdk.runner.telemetry.sink.SinkResult
import org.opendc.sdk.runner.telemetry.sink.SinkSession
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.cluster.ClusterSample
import org.opendc.sdk.runner.telemetry.table.datacenter.DataCenterSample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.task.TaskMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.trace.parquet.LocalInputFile
import org.opendc.trace.parquet.exporter.ExportColumn
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.walk

/**
 * Measures how much the telemetry export costs a simulation, in one of three modes per JVM:
 *
 * - `none`: no sink, so nothing is sampled
 * - `discard`: every table the Parquet sink would write is sampled, and the samples are dropped
 * - `parquet`: the samples are written to Parquet
 *
 * Run with `OPENDC_EXPORT_BENCH=<mode>`, and optionally `OPENDC_BENCH_TRACE` (default `borg_day`), `OPENDC_BENCH_ROOT`
 * (default `traces/` at the repository root, which is ignored by Git), `OPENDC_BENCH_JFR=1` and `OPENDC_BENCH_TASK_GPU=1`,
 * which selects every task column, so that the task GPU columns are written although the topology has no GPUs (variant
 * `task-gpu`, the columns before they were left out for topologies without GPUs). Results are appended to
 * `build/export-bench.csv`. Setting `OPENDC_EXPORT_BENCH` also disables the JaCoCo agent on the test task.
 *
 * In `parquet` mode, how each column was encoded and how large it is is written to `build/export-bench-columns-<trace>.csv`.
 *
 * `bench.sh` next to this file runs several modes in a row, optionally pinned to a set of CPUs.
 */
@EnabledIfEnvironmentVariable(named = "OPENDC_EXPORT_BENCH", matches = "none|discard|parquet")
class ExportOverheadMeasurement {
    @Test
    fun measure() {
        val mode = System.getenv("OPENDC_EXPORT_BENCH")
        val trace = System.getenv("OPENDC_BENCH_TRACE") ?: "borg_day"
        val root = System.getenv("OPENDC_BENCH_ROOT")?.let { Path.of(it) } ?: repositoryRoot().resolve("traces")
        val jfr = System.getenv("OPENDC_BENCH_JFR") == "1"
        val taskGpu = System.getenv("OPENDC_BENCH_TASK_GPU") == "1"

        val scenario =
            scenario {
                topology {
                    datacenter {
                        cluster(name = "C01") {
                            host(count = 1534, name = "H01") {
                                cpu(coreCount = 48, coreSpeed = 2100.mhz)
                                memory(size = 100000.mib)
                                cpuPowerModel = SqrtPowerModelSpec(maxPower = 530.watts, idlePower = 200.watts)
                            }
                        }
                    }
                }
                workload(TraceWorkloadSpec(source = NamedReference(trace)))
                exportModel = ExportSpec(printFrequency = null, columns = if (taskGpu) allTaskColumns() else ExportColumnsSpec())
            }

        val output = Files.createTempDirectory("opendc-export-bench")
        val discard = DiscardSink()
        val sinks =
            when (mode) {
                "none" -> emptyList()
                "discard" -> listOf(discard)
                else -> listOf(ParquetSink(output))
            }

        val threads = ManagementFactory.getThreadMXBean()
        val os = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
        val pauseCollectors = ManagementFactory.getGarbageCollectorMXBeans().filter { "Concurrent" !in it.name }

        val recording =
            if (jfr) {
                Recording(Configuration.getConfiguration("profile")).apply {
                    setDestination(Path.of("build/export-bench-$mode.jfr"))
                    System.getenv("OPENDC_BENCH_JFR_SECONDS")?.let { duration = java.time.Duration.ofSeconds(it.toLong()) }
                    start()
                }
            } else {
                null
            }

        val gcBefore = pauseCollectors.sumOf { it.collectionTime }
        val gcCountBefore = pauseCollectors.sumOf { it.collectionCount }
        val processCpuBefore = os.processCpuTime
        val simCpuBefore = threads.currentThreadCpuTime
        val wallBefore = System.nanoTime()

        runScenario(scenario, "export-bench", 0, 0L, sinks, FileSystemResourceProvisioner(root))

        val wallMs = (System.nanoTime() - wallBefore) / 1_000_000
        val simCpuMs = (threads.currentThreadCpuTime - simCpuBefore) / 1_000_000
        val processCpuMs = (os.processCpuTime - processCpuBefore) / 1_000_000
        val gcPauseMs = pauseCollectors.sumOf { it.collectionTime } - gcBefore
        val gcCount = pauseCollectors.sumOf { it.collectionCount } - gcCountBefore

        recording?.stop()
        recording?.close()

        val outputBytes = output.walk().sumOf { Files.size(it) }
        val line =
            listOf(
                trace, mode, wallMs, simCpuMs, wallMs - simCpuMs, processCpuMs, gcPauseMs, gcCount,
                discard.taskCount, discard.rows, discard.taskRows, outputBytes, allowedCpus(),
                if (taskGpu) "task-gpu" else "default",
            ).joinToString(",")

        val results = File("build/export-bench.csv")
        if (!results.exists()) {
            results.writeText(
                "trace,mode,wall_ms,sim_cpu_ms,sim_not_on_cpu_ms,process_cpu_ms,gc_pause_ms,gc_count," +
                    "tasks,rows,task_rows,output_bytes,cpus,variant\n",
            )
        }
        results.appendText("$line\n")
        println(line)

        if (mode == "parquet") {
            writeColumnBreakdown(trace, output.resolve("export-bench/raw-output/0/seed=0"))
        }

        if (mode == "discard") {
            File("build/export-bench-states.txt").appendText("$trace ${discard.taskStates}\n")
        }
    }

    /**
     * Write how each column of the Parquet files in [runDirectory] was encoded and how large it is to
     * `build/export-bench-columns-<trace>.csv`, with one line per column summed over its row groups. A column chunk with
     * both dictionary-encoded and other pages started with a dictionary and fell back when the dictionary grew too large.
     */
    private fun writeColumnBreakdown(
        trace: String,
        runDirectory: Path,
    ) {
        val lines =
            mutableListOf("file,column,row_groups,dictionary,fell_back,no_dictionary,data_encodings,compressed_bytes,uncompressed_bytes")
        val files = runDirectory.walk().filter { it.toString().endsWith(".parquet") }.sorted()
        for (file in files) {
            val footer = ParquetFileReader.open(LocalInputFile(file)).use { it.footer }
            val chunksByColumn = footer.blocks.flatMap { it.columns }.groupBy { it.path.toDotString() }
            for ((column, chunks) in chunksByColumn) {
                val stats = chunks.mapNotNull { it.encodingStats }
                val fellBack = stats.count { it.hasDictionaryEncodedPages() && it.hasNonDictionaryEncodedPages() }
                val dictionary = stats.count { it.hasDictionaryEncodedPages() && !it.hasNonDictionaryEncodedPages() }
                lines +=
                    listOf(
                        runDirectory.relativize(file),
                        column,
                        chunks.size,
                        dictionary,
                        fellBack,
                        chunks.size - dictionary - fellBack,
                        stats.flatMap { it.dataEncodings }.toSortedSet().joinToString(";"),
                        chunks.sumOf { it.totalSize },
                        chunks.sumOf { it.totalUncompressedSize },
                    ).joinToString(",")
            }
        }
        File("build/export-bench-columns-$trace.csv").writeText(lines.joinToString("\n", postfix = "\n"))
    }

    /**
     * A selection of every task column, which forces the task GPU columns.
     */
    private fun allTaskColumns(): ExportColumnsSpec {
        ComputeExportConfig.loadDfltColumns()
        return ExportColumnsSpec(task = OnlyColumns(ExportColumn.getAllLoadedColumns<TaskSample>().map { it.name }.toSet()))
    }

    /**
     * The CPUs this JVM may run on (e.g. `0-11` when pinned to the performance cores), or `?` if unknown.
     */
    private fun allowedCpus(): String =
        runCatching {
            File("/proc/self/status").readLines()
                .first { it.startsWith("Cpus_allowed_list:") }
                .substringAfter(':')
                .trim()
                .replace(',', ';')
        }.getOrDefault("?")

    /**
     * The root of the repository, found by walking up from the working directory to the Gradle settings file.
     */
    private fun repositoryRoot(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }

    /**
     * A sink that samples every table the Parquet sink would write, and drops the samples.
     */
    private class DiscardSink : OutputSink {
        var taskCount = 0
        var rows = 0L
        var taskRows = 0L
        val taskStates = sortedMapOf<String, Long>()

        override fun open(context: RunContext): SinkSession {
            taskCount = context.taskCount
            return object : SinkSession {
                override val monitor: MetricExporter =
                    object : MetricExporter {
                        override fun export(reader: BatterySample) {
                            rows++
                        }

                        override fun export(reader: ClusterSample) {
                            rows++
                        }

                        override fun export(reader: DataCenterSample) {
                            rows++
                        }

                        override fun export(reader: HostSample) {
                            rows++
                        }

                        override fun export(reader: PowerSourceSample) {
                            rows++
                        }

                        override fun export(reader: ServiceSample) {
                            rows++
                        }

                        override fun export(reader: TaskSample) {
                            rows++
                            taskRows++
                            taskStates.merge(reader.taskState?.name ?: "null", 1L, Long::plus)
                        }

                        override fun export(meta: TaskMeta) {
                            rows++
                        }
                    }

                override val tables = context.export.filesToExport.filterValues { it }.keys

                override fun result(): SinkResult? = null
            }
        }
    }
}
