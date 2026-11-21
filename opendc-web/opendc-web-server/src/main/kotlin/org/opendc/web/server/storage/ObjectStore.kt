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

package org.opendc.web.server.storage

import java.io.InputStream
import java.time.Duration

/** A stretch of a file, and the signed target that takes exactly that stretch. */
data class PartTarget(
    val url: String,
    val offset: Long,
    val length: Long,
)

/** Where the browser should send the bytes of one table: straight to storage, or through the server. */
sealed interface UploadTarget {
    /** The browser writes [parts] to storage itself, in parallel; a small file has one part. */
    data class Direct(val parts: List<PartTarget>) : UploadTarget

    data object ThroughServer : UploadTarget
}

/**
 * Storage for the large files a deployment keeps: trace tables and experiment parquet, addressed by
 * a key the caller chooses.
 */
interface ObjectStore : AutoCloseable {
    /** Stores [content] under [key], returning how much of it there was. */
    fun put(
        key: String,
        content: InputStream,
    ): Long

    /** Reads an object whole. The caller closes the stream. */
    fun open(key: String): InputStream

    /** Reads [length] bytes from [offset], so a parquet footer is read without fetching the whole file. */
    fun open(
        key: String,
        offset: Long,
        length: Long,
    ): InputStream

    fun size(key: String): Long

    fun exists(key: String): Boolean

    /** Every object under [prefix], in order, leaving out objects still being written. */
    fun list(prefix: String): List<String>

    /** Removes an object, along with any upload of it still in flight. Safe to call twice. */
    fun delete(key: String)

    /** Where a browser should send the [sizeBytes] bytes of [key]. */
    fun uploadTarget(
        key: String,
        sizeBytes: Long,
    ): UploadTarget

    /**
     * A URL that reads [key] for [lifetime], for a launcher that holds no credentials of its own. Its
     * scheme tells the launcher whether the bytes are on its own disk or remote.
     */
    fun readUrl(
        key: String,
        lifetime: Duration,
    ): String

    /** A URL that writes [key] in one PUT for [lifetime]. Nothing about the content is signed. */
    fun writeUrl(
        key: String,
        lifetime: Duration,
    ): String

    /** Removes every object under [prefix] and the prefix itself. Safe to call twice. */
    fun deletePrefix(prefix: String)

    /**
     * Assembles an upload that was sent in parts, and says whether [key] is now there and whole. An
     * upload with parts missing is discarded rather than joined into a corrupt object.
     */
    fun completeUpload(key: String): Boolean
}
