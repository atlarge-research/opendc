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

import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.PositionOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * An [OutputFile] on the local filesystem.
 */
public class LocalOutputFile(private val path: Path) : OutputFile {
    /**
     * Construct a [LocalOutputFile] from the specified [file]
     */
    public constructor(file: File) : this(file.toPath())

    override fun create(blockSizeHint: Long): PositionOutputStream {
        val output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        return NioPositionOutputStream(output)
    }

    override fun createOrOverwrite(blockSizeHint: Long): PositionOutputStream {
        val output = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        return NioPositionOutputStream(output)
    }

    override fun supportsBlockSize(): Boolean = false

    override fun defaultBlockSize(): Long =
        throw UnsupportedOperationException("Local filesystem does not have default block size")

    override fun getPath(): String = path.toString()

    /**
     * Implementation of [PositionOutputStream] for an [OutputStream].
     */
    private class NioPositionOutputStream(private val output: OutputStream) : PositionOutputStream() {
        /**
         * The current position in the file.
         */
        private var _pos = 0L

        override fun getPos(): Long = _pos

        override fun write(b: Int) {
            output.write(b)
            _pos++
        }

        override fun write(b: ByteArray) {
            output.write(b)
            _pos += b.size
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            output.write(b, off, len)
            _pos += len
        }

        override fun flush() {
            output.flush()
        }

        override fun close() {
            output.close()
        }

        override fun toString(): String = "NioPositionOutputStream[output=$output]"
    }
}
