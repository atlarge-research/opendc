/*
 * Copyright (c) 2022 AtLarge Research
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

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.OutputFile
import java.nio.file.Path

/**
 * Helper class for writing Parquet records to local disk.
 */
public class LocalParquetWriter {
    /**
     * A [ParquetWriter.Builder] implementation supporting custom [OutputFile]s and [WriteSupport] implementations.
     */
    public class Builder<T> internal constructor(
        output: OutputFile,
        private val writeSupport: WriteSupport<T>
    ) : ParquetWriter.Builder<T, Builder<T>>(output) {
        override fun self(): Builder<T> = this

        override fun getWriteSupport(conf: Configuration): WriteSupport<T> = writeSupport
    }

    public companion object {
        /**
         * Create a [Builder] instance that writes a Parquet file at the specified [path].
         */
        @JvmStatic
        public fun <T> builder(path: Path, writeSupport: WriteSupport<T>): Builder<T> =
            Builder(LocalOutputFile(path), writeSupport)
    }
}
