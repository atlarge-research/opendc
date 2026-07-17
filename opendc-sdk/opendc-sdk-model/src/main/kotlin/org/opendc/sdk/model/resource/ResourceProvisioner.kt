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

import java.nio.file.Path

/**
 * Provisions a [ResourceReference] into a locally accessible resource.
 *
 * The SDK model performs no I/O of its own — it only *denotes* external data with a
 * [ResourceReference]. A [ResourceProvisioner] is supplied by a consumer (the CLI, the web
 * module) and is the single place that knows *how* to acquire the bytes: from the local
 * filesystem, a database, or a hosted location (http/ftp/s3/…). Which mechanism applies is
 * derived from the reference — a [UriReference] scheme, or the consumer's own mapping of a
 * [NamedReference] — and never by the SDK.
 *
 * Implementations throw if the reference cannot be provisioned.
 */
public fun interface ResourceProvisioner {
    /**
     * Provisions [reference] and returns a handle to it as a local, readable resource.
     */
    public fun provision(reference: ResourceReference): ProvisionedResource
}

/**
 * A [ResourceReference] made available on the local filesystem for reading.
 *
 * [path] is a real, seekable filesystem location — either a single file (a carbon or failure
 * trace) or a directory (a workload trace bundle of `tasks.parquet` and `fragments.parquet`).
 * It is seekable rather than a one-shot stream because trace readers (Apache Parquet) require
 * random access.
 *
 * A resource fetched from a remote or database backend is materialized to a temporary location
 * and removed on [close]; a resource that already lives on the filesystem closes to a no-op.
 * Consume it within a `use { }` block so any temporary is always released.
 */
public interface ProvisionedResource : AutoCloseable {
    /** The local, seekable filesystem location of the provisioned resource. */
    public val path: Path
}
