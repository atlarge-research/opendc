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

package org.opendc.trace

import org.opendc.trace.internal.TraceImpl
import org.opendc.trace.spi.TraceFormat
import java.io.File
import java.nio.file.Path

/**
 * A trace is a collection of related tables that characterize a workload.
 */
public interface Trace {
    /**
     * The list of table names in the workload trace.
     */
    public val tables: List<String>

    /**
     * Determine if the trace contains a table with the specified [name].
     */
    public fun containsTable(name: String): Boolean

    /**
     * Obtain a [Table] with the specified [name].
     */
    public fun getTable(name: String): Table?

    public companion object {
        /**
         * Open a [Trace] at the specified [path] in the given [format].
         *
         * @param path The path to the trace.
         * @param format The format of the trace to open.
         * @throws IllegalArgumentException if [format] is not supported.
         */
        @JvmStatic
        public fun open(path: File, format: String): Trace = open(path.toPath(), format)

        /**
         * Open a [Trace] at the specified [path] in the given [format].
         *
         * @param path The [Path] to the trace.
         * @param format The format of the trace to open.
         * @throws IllegalArgumentException if [format] is not supported.
         */
        @JvmStatic
        public fun open(path: Path, format: String): Trace {
            val provider = requireNotNull(TraceFormat.byName(format)) { "Unknown format $format" }
            return TraceImpl(provider, path)
        }

        /**
         * Create a [Trace] at the specified [path] in the given [format].
         *
         * @param path The [Path] to the trace.
         * @param format The format of the trace to create.
         */
        @JvmStatic
        public fun create(path: File, format: String): Trace = create(path.toPath(), format)

        /**
         * Create a [Trace] at the specified [path] in the given [format].
         *
         * @param path The [Path] to the trace.
         * @param format The format of the trace to create.
         */
        @JvmStatic
        public fun create(path: Path, format: String): Trace {
            val provider = requireNotNull(TraceFormat.byName(format)) { "Unknown format $format" }
            provider.create(path)
            return TraceImpl(provider, path)
        }
    }
}
