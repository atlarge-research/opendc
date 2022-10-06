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

package org.opendc.trace.spi

import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import java.nio.file.Path
import java.util.ServiceLoader

/**
 * A service-provider class for parsing trace formats.
 */
public interface TraceFormat {
    /**
     * The name of the trace format.
     */
    public val name: String

    /**
     * Construct an empty trace at [path].
     *
     * @param path The path where to create the empty trace.
     * @throws IllegalArgumentException If [path] is invalid.
     * @throws UnsupportedOperationException If the table does not support trace creation.
     */
    public fun create(path: Path)

    /**
     * Return the name of the tables available in the trace at the specified [path].
     *
     * @param path The path to the trace.
     * @return The list of tables available in the trace.
     */
    public fun getTables(path: Path): List<String>

    /**
     * Return the details of [table] in the trace at the specified [path].
     *
     * @param path The path to the trace.
     * @param table The name of the table to obtain the details for.
     * @throws IllegalArgumentException If [table] does not exist.
     * @return The [TableDetails] for the specified [table].
     */
    public fun getDetails(path: Path, table: String): TableDetails

    /**
     * Open a [TableReader] for the specified [table].
     *
     * @param path The path to the trace to open.
     * @param table The name of the table to open a [TableReader] for.
     * @param projection The name of the columns to project or `null` if no projection is performed.
     * @throws IllegalArgumentException If [table] does not exist.
     * @return A [TableReader] instance for the table.
     */
    public fun newReader(path: Path, table: String, projection: List<String>?): TableReader

    /**
     * Open a [TableWriter] for the specified [table].
     *
     * @param path The path to the trace to open.
     * @param table The name of the table to open a [TableWriter] for.
     * @throws IllegalArgumentException If [table] does not exist.
     * @throws UnsupportedOperationException If the format does not support writing.
     * @return A [TableWriter] instance for the table.
     */
    public fun newWriter(path: Path, table: String): TableWriter

    /**
     * A helper object for resolving providers.
     */
    public companion object {
        /**
         * Obtain a list of [TraceFormat] that are available in the current thread context.
         */
        @JvmStatic
        public fun getInstalledProviders(): Iterable<TraceFormat> {
            return ServiceLoader.load(TraceFormat::class.java)
        }

        /**
         * Obtain a [TraceFormat] implementation by [name].
         */
        @JvmStatic
        public fun byName(name: String): TraceFormat? {
            val loader = ServiceLoader.load(TraceFormat::class.java)
            return loader.find { it.name == name }
        }
    }
}
