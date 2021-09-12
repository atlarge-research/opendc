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

package org.opendc.trace.bitbrains

import com.fasterxml.jackson.dataformat.csv.CsvFactory
import org.opendc.trace.*
import java.nio.file.Path

/**
 * [Trace] implementation for the Bitbrains format.
 */
public class BitbrainsTrace internal constructor(private val factory: CsvFactory, private val path: Path) : Trace {
    override val tables: List<String> = listOf(TABLE_RESOURCES, TABLE_RESOURCE_STATES)

    override fun containsTable(name: String): Boolean = tables.contains(name)

    override fun getTable(name: String): Table? {
        return when (name) {
            TABLE_RESOURCES -> BitbrainsResourceTable(factory, path)
            TABLE_RESOURCE_STATES -> BitbrainsResourceStateTable(factory, path)
            else -> null
        }
    }

    override fun toString(): String = "BitbrainsTrace[$path]"
}
