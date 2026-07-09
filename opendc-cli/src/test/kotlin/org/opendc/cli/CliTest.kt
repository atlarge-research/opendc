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

    private fun opendc() = OpendcCommand().subcommands(RunCommand(), ValidateCommand(), ShowCommand())

    private fun resourcePath(name: String): String = File(checkNotNull(javaClass.classLoader.getResource(name)).toURI()).absolutePath
}
