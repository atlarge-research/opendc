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

import org.apache.calcite.DataContext
import org.apache.calcite.adapter.java.JavaTypeFactory
import org.apache.calcite.linq4j.AbstractEnumerable
import org.apache.calcite.linq4j.Enumerable
import org.apache.calcite.linq4j.Enumerator
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.schema.ScannableTable
import org.apache.calcite.schema.Table
import org.apache.calcite.schema.impl.AbstractTable
import org.apache.calcite.sql.type.SqlTypeName
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Calcite [Table] that exposes an OpenDC [org.opendc.trace.Table] as SQL table.
 */
internal class TraceTable(private val table: org.opendc.trace.Table) : AbstractTable(), ScannableTable {

    private var rowType: RelDataType? = null

    override fun getRowType(typeFactory: RelDataTypeFactory): RelDataType {
        var rowType = rowType
        if (rowType == null) {
            rowType = deduceRowType(typeFactory as JavaTypeFactory)
            this.rowType = rowType
        }

        return rowType
    }

    override fun scan(root: DataContext): Enumerable<Array<Any?>> {
        val cancelFlag = DataContext.Variable.CANCEL_FLAG.get<AtomicBoolean>(root)
        return object : AbstractEnumerable<Array<Any?>>() {
            override fun enumerator(): Enumerator<Array<Any?>> = TraceReaderEnumerator(table.newReader(), table.columns, cancelFlag)
        }
    }

    override fun toString(): String = "TraceTable"

    private fun deduceRowType(typeFactory: JavaTypeFactory): RelDataType {
        val types = mutableListOf<RelDataType>()
        val names = mutableListOf<String>()

        for (column in table.columns) {
            names.add(column.name)
            types.add(
                when (column.type) {
                    Instant::class.java -> typeFactory.createSqlType(SqlTypeName.TIMESTAMP)
                    Duration::class.java -> typeFactory.createSqlType(SqlTypeName.BIGINT)
                    Set::class.java -> typeFactory.createMultisetType(typeFactory.createSqlType(SqlTypeName.ANY), -1)
                    else -> typeFactory.createType(column.type)
                }
            )
        }

        return typeFactory.createStructType(types, names)
    }
}
