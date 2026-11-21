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
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration

/** A directory of objects, one file per key, for development and tests. */
class LocalObjectStore(private val root: Path) : ObjectStore {
    override fun put(
        key: String,
        content: InputStream,
    ): Long {
        val target = fileOf(key)
        Files.createDirectories(target.parent)
        // Written beside its destination and renamed, so a broken transfer leaves no half a file.
        val spool = Files.createTempFile(target.parent, ".incoming-", ".part")
        try {
            val size = Files.newOutputStream(spool).use { content.copyTo(it) }
            Files.move(spool, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return size
        } finally {
            Files.deleteIfExists(spool)
        }
    }

    override fun open(key: String): InputStream = Files.newInputStream(fileOf(key))

    override fun open(
        key: String,
        offset: Long,
        length: Long,
    ): InputStream {
        val channel = Files.newByteChannel(fileOf(key))
        channel.position(offset)
        return Channels.newInputStream(channel).let { stream ->
            object : InputStream() {
                private var left = length

                override fun read(): Int = if (left-- <= 0) -1 else stream.read()

                override fun read(
                    buffer: ByteArray,
                    off: Int,
                    len: Int,
                ): Int {
                    if (left <= 0) return -1
                    val read = stream.read(buffer, off, minOf(len.toLong(), left).toInt())
                    if (read > 0) left -= read
                    return read
                }

                override fun close() = stream.close()
            }
        }
    }

    override fun size(key: String): Long = Files.size(fileOf(key))

    override fun exists(key: String): Boolean = Files.exists(fileOf(key))

    /** The files under the directory [prefix] names, skipping the dot-named spools of transfers in flight. */
    override fun list(prefix: String): List<String> {
        val base = fileOf(prefix)
        if (!Files.isDirectory(base)) {
            return if (Files.exists(base)) listOf(prefix) else emptyList()
        }
        return Files
            .walk(base)
            .use { paths -> paths.toList() }
            .filter { Files.isRegularFile(it) && !it.fileName.toString().startsWith(".") }
            .map { root.relativize(it).joinToString("/") }
            .sorted()
    }

    override fun delete(key: String) {
        Files.deleteIfExists(fileOf(key))
    }

    override fun uploadTarget(
        key: String,
        sizeBytes: Long,
    ): UploadTarget = UploadTarget.ThroughServer

    override fun completeUpload(key: String): Boolean = exists(key)

    /** A launcher on this machine reads the file itself, so nothing is signed and nothing expires. */
    override fun readUrl(
        key: String,
        lifetime: Duration,
    ): String = fileOf(key).toAbsolutePath().toUri().toString()

    override fun writeUrl(
        key: String,
        lifetime: Duration,
    ): String = fileOf(key).toAbsolutePath().toUri().toString()

    override fun deletePrefix(prefix: String) {
        fileOf(prefix).toFile().deleteRecursively()
    }

    override fun close() {}

    private fun fileOf(key: String): Path = root.resolve(key)
}
