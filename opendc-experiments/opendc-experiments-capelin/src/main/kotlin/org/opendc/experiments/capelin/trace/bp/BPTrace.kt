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

package org.opendc.experiments.capelin.trace.bp

import org.opendc.trace.TABLE_RESOURCES
import org.opendc.trace.TABLE_RESOURCE_STATES
import org.opendc.trace.Table
import org.opendc.trace.Trace
import java.nio.file.Path

/**
 * A [Trace] in the Bitbrains Parquet format.
 */
public class BPTrace internal constructor(private val path: Path) : Trace {
    override val tables: List<String> = listOf(TABLE_RESOURCES, TABLE_RESOURCE_STATES)

    override fun containsTable(name: String): Boolean =
        name == TABLE_RESOURCES || name == TABLE_RESOURCE_STATES

    override fun getTable(name: String): Table? {
        return when (name) {
            TABLE_RESOURCES -> BPResourceTable(path)
            TABLE_RESOURCE_STATES -> BPResourceStateTable(path)
            else -> null
        }
    }

    override fun toString(): String = "BPTrace[$path]"
}
