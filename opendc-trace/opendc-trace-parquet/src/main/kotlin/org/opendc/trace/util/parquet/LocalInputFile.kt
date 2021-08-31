/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.trace.util.parquet

import org.apache.parquet.io.InputFile
import org.apache.parquet.io.SeekableInputStream
import java.io.EOFException
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * An [InputFile] on the local filesystem.
 */
public class LocalInputFile(private val path: Path) : InputFile {
    /**
     * The [FileChannel] used for accessing the input path.
     */
    private val channel = FileChannel.open(path, StandardOpenOption.READ)

    /**
     * Construct a [LocalInputFile] for the specified [file].
     */
    public constructor(file: File) : this(file.toPath())

    override fun getLength(): Long = channel.size()

    override fun newStream(): SeekableInputStream = object : SeekableInputStream() {
        override fun read(buf: ByteBuffer): Int {
            return channel.read(buf)
        }

        override fun read(): Int {
            val single = ByteBuffer.allocate(1)
            var read: Int

            // ReadableByteChannel#read might read zero bytes so continue until we read at least one byte
            do {
                read = channel.read(single)
            } while (read == 0)

            return if (read == -1) {
                read
            } else {
                single.get(0).toInt() and 0xff
            }
        }

        override fun getPos(): Long {
            return channel.position()
        }

        override fun seek(newPos: Long) {
            channel.position(newPos)
        }

        override fun readFully(bytes: ByteArray) {
            readFully(ByteBuffer.wrap(bytes))
        }

        override fun readFully(bytes: ByteArray, start: Int, len: Int) {
            readFully(ByteBuffer.wrap(bytes, start, len))
        }

        override fun readFully(buf: ByteBuffer) {
            var remainder = buf.remaining()
            while (remainder > 0) {
                val read = channel.read(buf)
                remainder -= read

                if (read == -1 && remainder > 0) {
                    throw EOFException()
                }
            }
        }

        override fun close() {
            channel.close()
        }

        override fun toString(): String = "NioSeekableInputStream"
    }

    override fun toString(): String = "LocalInputFile[path=$path]"
}
