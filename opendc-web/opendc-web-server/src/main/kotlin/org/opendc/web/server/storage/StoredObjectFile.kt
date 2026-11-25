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

import org.apache.parquet.io.InputFile
import org.apache.parquet.io.SeekableInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer

/** A stored object presented to parquet, which reads it by seeking rather than start to finish. */
class StoredObjectFile(private val store: ObjectStore, private val key: String) : InputFile {
    private val length = store.size(key)

    override fun getLength(): Long = length

    override fun newStream(): SeekableInputStream = StoredObjectStream(store, key, length)
}

/**
 * Reads a stored object at whatever offset parquet asks for. A seek drops the open range and the
 * next read opens a new one, which suits how parquet reads: the footer at the end, then the parts
 * of the file the footer pointed at.
 */
private class StoredObjectStream(
    private val store: ObjectStore,
    private val key: String,
    private val length: Long,
) : SeekableInputStream() {
    private var position = 0L
    private var range: InputStream? = null

    override fun getPos(): Long = position

    override fun seek(newPos: Long) {
        if (newPos != position) {
            close()
            position = newPos
        }
    }

    override fun read(): Int {
        val value = opened().read()
        if (value >= 0) position++
        return value
    }

    override fun read(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        val read = opened().read(bytes, offset, length)
        if (read > 0) position += read
        return read
    }

    override fun readFully(bytes: ByteArray) = readFully(bytes, 0, bytes.size)

    override fun readFully(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ) {
        var filled = 0
        while (filled < length) {
            val read = read(bytes, offset + filled, length - filled)
            if (read < 0) throw EOFException("$key ended after $position bytes")
            filled += read
        }
    }

    override fun read(buffer: ByteBuffer): Int {
        val chunk = ByteArray(buffer.remaining())
        val read = read(chunk, 0, chunk.size)
        if (read > 0) buffer.put(chunk, 0, read)
        return read
    }

    override fun readFully(buffer: ByteBuffer) {
        val chunk = ByteArray(buffer.remaining())
        readFully(chunk, 0, chunk.size)
        buffer.put(chunk)
    }

    override fun close() {
        range?.close()
        range = null
    }

    private fun opened(): InputStream = range ?: store.open(key, position, length - position).also { range = it }
}
