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
import org.apache.calcite.adapter.java.AbstractQueryableTable
import org.apache.calcite.adapter.java.JavaTypeFactory
import org.apache.calcite.linq4j.AbstractEnumerable
import org.apache.calcite.linq4j.Enumerable
import org.apache.calcite.linq4j.Enumerator
import org.apache.calcite.linq4j.QueryProvider
import org.apache.calcite.linq4j.Queryable
import org.apache.calcite.plan.RelOptCluster
import org.apache.calcite.plan.RelOptTable
import org.apache.calcite.prepare.Prepare.CatalogReader
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.core.TableModify
import org.apache.calcite.rel.logical.LogicalTableModify
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.rex.RexNode
import org.apache.calcite.schema.ModifiableTable
import org.apache.calcite.schema.ProjectableFilterableTable
import org.apache.calcite.schema.SchemaPlus
import org.apache.calcite.schema.impl.AbstractTableQueryable
import org.apache.calcite.sql.type.SqlTypeName
import org.opendc.trace.TableColumnType
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Calcite [Table] that exposes an OpenDC [org.opendc.trace.Table] as SQL table.
 */
internal class TraceTable(private val table: org.opendc.trace.Table) :
    AbstractQueryableTable(Array<Any?>::class.java),
    ProjectableFilterableTable,
    ModifiableTable,
    InsertableTable {
    private var rowType: RelDataType? = null

    override fun getRowType(typeFactory: RelDataTypeFactory): RelDataType {
        var rowType = rowType
        if (rowType == null) {
            rowType = deduceRowType(typeFactory as JavaTypeFactory)
            this.rowType = rowType
        }

        return rowType
    }

    override fun scan(root: DataContext, filters: MutableList<RexNode>, projects: IntArray?): Enumerable<Array<Any?>> {
        // Filters are currently not supported by the OpenDC trace API. By keeping the filters in the list, Calcite
        // assumes that they are declined and will perform the filters itself.

        val projection = projects?.map { table.columns[it] }
        val cancelFlag = DataContext.Variable.CANCEL_FLAG.get<AtomicBoolean>(root)
        return object : AbstractEnumerable<Array<Any?>>() {
            override fun enumerator(): Enumerator<Array<Any?>> =
                TraceReaderEnumerator(table.newReader(projection?.map { it.name }), projection ?: table.columns, cancelFlag)
        }
    }

    override fun insert(rows: Enumerable<Array<Any?>>): Long {
        val table = table
        val columns = table.columns
        val writer = table.newWriter()
        val columnIndices = columns.map { writer.resolve(it.name) }.toIntArray()
        var rowCount = 0L

        try {
            for (row in rows) {
                writer.startRow()

                for ((index, value) in row.withIndex()) {
                    if (value == null) {
                        continue
                    }
                    val columnType = columns[index].type
                    val columnIndex = columnIndices[index]
                    when (columnType) {
                        is TableColumnType.Boolean -> writer.setBoolean(columnIndex, value as Boolean)
                        is TableColumnType.Int -> writer.setInt(columnIndex, value as Int)
                        is TableColumnType.Long -> writer.setLong(columnIndex, value as Long)
                        is TableColumnType.Float -> writer.setFloat(columnIndex, value as Float)
                        is TableColumnType.Double -> writer.setDouble(columnIndex, value as Double)
                        is TableColumnType.String -> writer.setString(columnIndex, value as String)
                        is TableColumnType.UUID -> {
                            val bb = ByteBuffer.wrap(value as ByteArray)
                            writer.setUUID(columnIndex, UUID(bb.getLong(), bb.getLong()))
                        }
                        is TableColumnType.Instant -> writer.setInstant(columnIndex, Instant.ofEpochMilli(value as Long))
                        is TableColumnType.Duration -> writer.setDuration(columnIndex, Duration.ofMillis(value as Long))
                        is TableColumnType.List -> writer.setList(columnIndex, value as List<*>)
                        is TableColumnType.Set -> writer.setSet(columnIndex, (value as List<*>).toSet())
                        is TableColumnType.Map -> writer.setMap(columnIndex, value as Map<*, *>)
                    }
                }

                writer.endRow()

                rowCount++
            }
        } finally {
            writer.close()
        }

        return rowCount
    }

    override fun <T> asQueryable(queryProvider: QueryProvider, schema: SchemaPlus, tableName: String): Queryable<T> {
        return object : AbstractTableQueryable<T>(queryProvider, schema, this@TraceTable, tableName) {
            override fun enumerator(): Enumerator<T> {
                val cancelFlag = AtomicBoolean(false)
                return TraceReaderEnumerator(
                    this@TraceTable.table.newReader(),
                    this@TraceTable.table.columns,
                    cancelFlag
                )
            }

            override fun toString(): String = "TraceTableQueryable[table=$tableName]"
        }
    }

    override fun getModifiableCollection(): MutableCollection<Any?>? = null

    override fun toModificationRel(
        cluster: RelOptCluster,
        table: RelOptTable,
        catalogReader: CatalogReader,
        child: RelNode,
        operation: TableModify.Operation,
        updateColumnList: MutableList<String>?,
        sourceExpressionList: MutableList<RexNode>?,
        flattened: Boolean
    ): TableModify {
        cluster.planner.addRule(TraceTableModifyRule.DEFAULT.toRule())

        return LogicalTableModify.create(
            table,
            catalogReader,
            child,
            operation,
            updateColumnList,
            sourceExpressionList,
            flattened
        )
    }

    override fun toString(): String = "TraceTable"

    private fun deduceRowType(typeFactory: JavaTypeFactory): RelDataType {
        val types = mutableListOf<RelDataType>()
        val names = mutableListOf<String>()

        for (column in table.columns) {
            names.add(column.name)
            types.add(mapType(typeFactory, column.type))
        }

        return typeFactory.createStructType(types, names)
    }

    private fun mapType(typeFactory: JavaTypeFactory, type: TableColumnType): RelDataType {
        return when (type) {
            is TableColumnType.Boolean -> typeFactory.createSqlType(SqlTypeName.BOOLEAN)
            is TableColumnType.Int -> typeFactory.createSqlType(SqlTypeName.INTEGER)
            is TableColumnType.Long -> typeFactory.createSqlType(SqlTypeName.BIGINT)
            is TableColumnType.Float -> typeFactory.createSqlType(SqlTypeName.FLOAT)
            is TableColumnType.Double -> typeFactory.createSqlType(SqlTypeName.DOUBLE)
            is TableColumnType.String -> typeFactory.createSqlType(SqlTypeName.VARCHAR)
            is TableColumnType.UUID -> typeFactory.createSqlType(SqlTypeName.BINARY, 16)
            is TableColumnType.Instant -> typeFactory.createSqlType(SqlTypeName.TIMESTAMP)
            is TableColumnType.Duration -> typeFactory.createSqlType(SqlTypeName.BIGINT)
            is TableColumnType.List -> typeFactory.createArrayType(mapType(typeFactory, type.elementType), -1)
            is TableColumnType.Set -> typeFactory.createMultisetType(mapType(typeFactory, type.elementType), -1)
            is TableColumnType.Map -> typeFactory.createMapType(mapType(typeFactory, type.keyType), mapType(typeFactory, type.valueType))
        }
    }
}
