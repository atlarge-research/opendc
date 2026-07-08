/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.resource

import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the [ResourceProvisioner] I/O seam: a consumer-supplied provisioner turns a
 * [ResourceReference] into a local [ProvisionedResource], and its [AutoCloseable] contract
 * releases any materialized temporary on close.
 */
class ResourceProvisionerTest {
    @Test
    fun `provisioner resolves a named reference to a local path under a root`() {
        val root = Files.createTempDirectory("traces")
        val provisioner = ResourceProvisioner { ref -> localResource(root.resolve((ref as NamedReference).name)) }

        provisioner.provision(NamedReference("bitbrains")).use { resource ->
            assertEquals(root.resolve("bitbrains"), resource.path)
        }

        Files.deleteIfExists(root)
    }

    @Test
    fun `provisioner resolves a file uri reference to a local path`() {
        val provisioner = ResourceProvisioner { ref -> localResource(Path.of(URI((ref as UriReference).uri))) }

        provisioner.provision(UriReference("file:///traces/failures.parquet")).use { resource ->
            assertEquals(Path.of("/traces/failures.parquet"), resource.path)
        }
    }

    @Test
    fun `closing a provisioned resource releases its temporary`() {
        val temp = Files.createTempFile("trace", ".parquet")
        val provisioner = ResourceProvisioner { temporaryResource(temp) }

        assertTrue(temp.exists())
        provisioner.provision(NamedReference("x")).use { assertEquals(temp, it.path) }

        assertFalse(temp.exists(), "expected the temporary to be deleted on close")
    }

    @Test
    fun `provisioner resolves a carbon reference to a single readable file`() {
        val file = Files.createTempFile("carbon", ".parquet")
        Files.writeString(file, "intensity")
        val provisioner = ResourceProvisioner { localResource(file) }

        provisioner.provision(NamedReference("carbon-trace")).use { resource ->
            assertTrue(Files.isRegularFile(resource.path), "a carbon trace provisions to a single file")
            assertEquals("intensity", Files.readString(resource.path))
        }

        Files.deleteIfExists(file)
    }

    @Test
    fun `provisioner resolves a workload reference to a directory bundle`() {
        val dir = Files.createTempDirectory("workload")
        Files.createFile(dir.resolve("tasks.parquet"))
        Files.createFile(dir.resolve("fragments.parquet"))
        val provisioner = ResourceProvisioner { localResource(dir) }

        provisioner.provision(NamedReference("bitbrains")).use { resource ->
            assertTrue(Files.isDirectory(resource.path), "a workload trace provisions to a directory bundle")
            assertTrue(Files.exists(resource.path.resolve("tasks.parquet")))
            assertTrue(Files.exists(resource.path.resolve("fragments.parquet")))
        }

        Files.deleteIfExists(dir.resolve("tasks.parquet"))
        Files.deleteIfExists(dir.resolve("fragments.parquet"))
        Files.deleteIfExists(dir)
    }

    private fun localResource(at: Path): ProvisionedResource =
        object : ProvisionedResource {
            override val path: Path = at

            override fun close() = Unit
        }

    private fun temporaryResource(temp: Path): ProvisionedResource =
        object : ProvisionedResource {
            override val path: Path = temp

            override fun close() {
                Files.deleteIfExists(temp)
            }
        }
}
