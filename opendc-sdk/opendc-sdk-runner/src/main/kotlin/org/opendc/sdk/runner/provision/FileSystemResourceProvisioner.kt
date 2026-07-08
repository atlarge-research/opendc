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

package org.opendc.sdk.runner.provision

import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.ProvisionedResource
import org.opendc.sdk.model.resource.ResourceProvisioner
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.resource.UriReference
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * A [ResourceProvisioner] that resolves references against the local filesystem.
 *
 * A [NamedReference] is resolved relative to [root]. A [UriReference] is resolved from its `file:`
 * URI, or relative to [root] when it carries no scheme. Any other scheme is rejected — supply a
 * bespoke [ResourceProvisioner] for remote or database backends.
 *
 * @property root The directory logical names and relative URIs are resolved against.
 */
public class FileSystemResourceProvisioner(private val root: Path) : ResourceProvisioner {
    override fun provision(reference: ResourceReference): ProvisionedResource =
        when (reference) {
            is NamedReference -> localResource(root.resolve(reference.name))
            is UriReference -> localResource(reference.toLocalPath())
        }

    private fun UriReference.toLocalPath(): Path {
        val parsed = URI.create(uri)
        val scheme = parsed.scheme
        if (scheme != null && scheme != "file") {
            throw UnsupportedOperationException(
                "FileSystemResourceProvisioner cannot provision '$scheme' URIs; supply a custom ResourceProvisioner",
            )
        }
        return if (scheme == "file") Path.of(parsed) else root.resolve(uri)
    }

    private fun localResource(path: Path): ProvisionedResource {
        if (!Files.exists(path)) throw IllegalArgumentException("Resource not found on filesystem: $path")
        return LocalResource(path)
    }

    private class LocalResource(override val path: Path) : ProvisionedResource {
        override fun close() {}
    }
}
