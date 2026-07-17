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

package org.opendc.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** End-to-end tests of the three `opendc` subcommands against a tiny, self-contained experiment. */
class CliTest {
    private val tiny = resourcePath("experiments/tiny-experiment.json")
    private val invalid = resourcePath("experiments/invalid-experiment.json")
    private val imports = resourcePath("experiments/imports/experiment.json")

    @Test
    fun `validate accepts a valid experiment`() {
        val result = opendc().test(listOf("validate", tiny))
        assertEquals(0, result.statusCode, result.output)
        assertContains(result.stdout, "valid")
    }

    @Test
    fun `validate rejects an invalid experiment`() {
        val result = opendc().test(listOf("validate", invalid))
        assertEquals(1, result.statusCode)
        assertContains(result.stdout, "coreCount")
    }

    @Test
    fun `show prints the topologies`() {
        val result = opendc().test(listOf("show", tiny))
        assertEquals(0, result.statusCode, result.output)
        assertContains(result.stdout, "Topology")
    }

    @Test
    fun `run simulates and writes parquet and a summary`() {
        val out = createTempDirectory("opendc-cli-run")
        try {
            val result = opendc().test(listOf("run", tiny, "-o", out.toString(), "--no-progress"))
            assertEquals(0, result.statusCode, result.output)
            assertContains(result.stdout, "Tasks")
            val parquet = out.toFile().walkTopDown().filter { it.extension == "parquet" }.toList()
            assertTrue(parquet.isNotEmpty(), "expected parquet output under $out")
        } finally {
            out.toFile().deleteRecursively()
        }
    }

    @Test
    fun `run rejects the not-yet-implemented --api-url with a clean error`() {
        val result = opendc().test(listOf("run", tiny, "--api-url", "http://example.com", "--no-progress"))
        assertEquals(1, result.statusCode)
        assertContains(result.output, "not implemented")
    }

    /** An experiment composed from other files with `importFrom` runs like any other. */
    @Test
    fun `run simulates an experiment that imports its topology and workload`() {
        val out = createTempDirectory("opendc-cli-imports")
        try {
            val result = opendc().test(listOf("run", imports, "-o", out.toString(), "--no-progress"))
            assertEquals(0, result.statusCode, result.output)
            val parquet = out.toFile().walkTopDown().filter { it.extension == "parquet" }.toList()
            assertTrue(parquet.isNotEmpty(), "expected parquet output under $out")
        } finally {
            out.toFile().deleteRecursively()
        }
    }

    /** `show` renders the imported topology, proving the import is resolved before anything reads it. */
    @Test
    fun `show prints a topology pulled in with importFrom`() {
        val result = opendc().test(listOf("show", imports))
        assertEquals(0, result.statusCode, result.output)
        assertContains(result.stdout, "big-host")
    }

    /**
     * A legacy experiment names its topology relative to the directory it is *run from*, as the
     * deprecated runner resolved it. The fixture is therefore written below the working directory and
     * referenced by a working-directory-relative path — the very thing that has to keep working.
     */
    @Test
    fun `--legacy resolves an experiment's paths against the working directory`() {
        withLegacyExperimentBelowWorkingDirectory { experiment ->
            val result = opendc().test(listOf("--legacy", "show", experiment))
            assertEquals(0, result.statusCode, result.output)
            // The topology the experiment only referenced by a relative path was found and inlined.
            assertContains(result.stdout, "C01")
        }
    }

    @Test
    fun `a legacy experiment read without --legacy fails with a clean error`() {
        withLegacyExperimentBelowWorkingDirectory { experiment ->
            val result = opendc().test(listOf("validate", experiment))
            assertEquals(1, result.statusCode)
            assertContains(result.output, "Could not read experiment")
        }
    }

    /**
     * Writes a legacy experiment and its topology under the working directory and hands the [body] the
     * experiment's working-directory-relative path.
     */
    private fun withLegacyExperimentBelowWorkingDirectory(body: (String) -> Unit) {
        val relativeRoot = "build/tmp/legacy-cli-test"
        val root = File(relativeRoot)
        root.deleteRecursively()
        root.mkdirs()
        try {
            File(root, "topology.json").writeText(
                """
                {
                  "clusters": [{
                    "name": "C01",
                    "hosts": [{
                      "name": "H01",
                      "count": 2,
                      "cpu": { "coreCount": 8, "coreSpeed": 2100 },
                      "memory": { "memorySize": 100000 },
                      "cpuPowerModel": { "modelType": "linear", "idlePower": 32.0, "maxPower": 180.0 }
                    }]
                  }]
                }
                """.trimIndent(),
            )
            File(root, "experiment.json").writeText(
                """
                {
                  "name": "legacy",
                  "topologies": [{ "pathToFile": "$relativeRoot/topology.json" }],
                  "workloads": [{ "pathToFile": "workload_traces/surf_week", "type": "ComputeWorkload" }],
                  "exportModels": [{ "exportInterval": 3600 }]
                }
                """.trimIndent(),
            )
            body("$relativeRoot/experiment.json")
        } finally {
            root.deleteRecursively()
        }
    }

    private fun opendc() = OpendcCommand().subcommands(RunCommand(), ValidateCommand(), ShowCommand())

    private fun resourcePath(name: String): String = File(checkNotNull(javaClass.classLoader.getResource(name)).toURI()).absolutePath
}
