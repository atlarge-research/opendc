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

package org.opendc.sdk.runner.unit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import java.nio.file.Files

class FileSystemResourceProvisionerTest {
    @Test
    fun `resolves a named reference under the root`() {
        val root = Files.createTempDirectory("prov")
        val file = Files.createFile(root.resolve("trace.parquet"))
        FileSystemResourceProvisioner(root).provision(NamedReference("trace.parquet")).use {
            assertEquals(file, it.path)
        }
    }

    @Test
    fun `resolves a file uri`() {
        val root = Files.createTempDirectory("prov")
        val file = Files.createFile(root.resolve("carbon.parquet"))
        FileSystemResourceProvisioner(root).provision(UriReference(file.toUri().toString())).use {
            assertEquals(file, it.path)
        }
    }

    @Test
    fun `rejects unsupported uri schemes`() {
        val provisioner = FileSystemResourceProvisioner(Files.createTempDirectory("prov"))
        assertThrows<UnsupportedOperationException> { provisioner.provision(UriReference("s3://bucket/trace.parquet")) }
    }

    @Test
    fun `throws when the resource is missing`() {
        val provisioner = FileSystemResourceProvisioner(Files.createTempDirectory("prov"))
        assertThrows<IllegalArgumentException> { provisioner.provision(NamedReference("absent.parquet")) }
    }
}
