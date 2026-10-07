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

package org.opendc.sdk.runner.integration

import org.apache.parquet.hadoop.ParquetFileReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.hours
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.scenario
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.trace.parquet.LocalInputFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Test suite for writing the Parquet output of a run with several writer threads per file.
 */
class ParquetWriterThreadsTest {
    @Test
    fun `several writer threads write the same rows as one`() {
        val single = simulate(writerThreads = 1)
        val multiple = simulate(writerThreads = 3)

        assertAll(
            { assertEquals(rowCount(single.resolve("task.parquet")), rowCount(multiple.resolve("task.parquet"))) },
            { assertEquals(rowCount(single.resolve("host.parquet")), rowCount(multiple.resolve("host.parquet"))) },
            { assertEquals(emptyList<String>(), multiple.listDirectoryEntries(".*").map { it.name }) { "part files should be removed" } },
        )
    }

    /**
     * Simulate a small trace workload, and return the directory with its Parquet output.
     */
    private fun simulate(writerThreads: Int): Path {
        val output = Files.createTempDirectory("opendc-writer-threads")
        val report =
            OpenDC.builder()
                .provisioner(FileSystemResourceProvisioner(testResourcesRoot))
                .output(output, writerThreads)
                .build()
                .simulate(
                    scenario {
                        topology {
                            datacenter {
                                cluster(name = "C01") {
                                    host(count = 4, name = "H01") {
                                        cpu(coreCount = 64, coreSpeed = 2000.mhz)
                                        memory(size = 1024.gib)
                                    }
                                }
                            }
                        }
                        workload(TraceWorkloadSpec(source = NamedReference("workloadTraces/bitbrains-small")))
                        exportModel = ExportSpec(exportInterval = 1.hours, printFrequency = null)
                    },
                )
        return requireNotNull(report.runs.single().outputPath)
    }

    private fun rowCount(file: Path): Long =
        ParquetFileReader.open(LocalInputFile(file)).use { reader ->
            reader.footer.blocks.sumOf {
                it.rowCount
            }
        }

    private companion object {
        val testResourcesRoot: Path = Path.of(object {}.javaClass.getResource("/workloadTraces")!!.toURI()).parent
    }
}
