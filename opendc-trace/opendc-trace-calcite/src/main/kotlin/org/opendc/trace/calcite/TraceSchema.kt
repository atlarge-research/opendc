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

package org.opendc.trace.calcite

import org.apache.calcite.schema.Schema
import org.apache.calcite.schema.Table
import org.apache.calcite.schema.impl.AbstractSchema
import org.opendc.trace.Trace

/**
 * A Calcite [Schema] that exposes an OpenDC [Trace] into multiple SQL tables.
 *
 * @param trace The [Trace] to create a schema for.
 */
public class TraceSchema(private val trace: Trace) : AbstractSchema() {
    /**
     * The [Table]s that belong to this schema.
     */
    private val tables: Map<String, TraceTable> by lazy {
        trace.tables.associateWith {
            val table = checkNotNull(trace.getTable(it)) { "Unexpected null table" }
            TraceTable(table)
        }
    }

    override fun getTableMap(): Map<String, Table> = tables
}
