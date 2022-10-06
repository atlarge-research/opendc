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

package org.opendc.trace.internal

import org.opendc.trace.Table
import org.opendc.trace.Trace
import org.opendc.trace.spi.TraceFormat
import java.nio.file.Path
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

/**
 * Internal implementation of the [Trace] interface.
 */
internal class TraceImpl(val format: TraceFormat, val path: Path) : Trace {
    /**
     * A map containing the [TableImpl] instances associated with the trace.
     */
    private val tableMap = ConcurrentHashMap<String, TableImpl>()

    override val tables: List<String> = format.getTables(path)

    init {
        for (table in tables) {
            tableMap.computeIfAbsent(table) { TableImpl(this, it) }
        }
    }

    override fun containsTable(name: String): Boolean = tableMap.containsKey(name)

    override fun getTable(name: String): Table? = tableMap[name]

    override fun hashCode(): Int = Objects.hash(format, path)

    override fun equals(other: Any?): Boolean = other is TraceImpl && format == other.format && path == other.path
}
