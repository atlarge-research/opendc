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

import org.apache.calcite.adapter.enumerable.EnumerableRel
import org.apache.calcite.adapter.enumerable.EnumerableRel.Prefer
import org.apache.calcite.adapter.enumerable.EnumerableRelImplementor
import org.apache.calcite.adapter.enumerable.EnumerableTableScan
import org.apache.calcite.adapter.enumerable.JavaRowFormat
import org.apache.calcite.adapter.enumerable.PhysTypeImpl
import org.apache.calcite.adapter.java.JavaTypeFactory
import org.apache.calcite.linq4j.Enumerable
import org.apache.calcite.linq4j.tree.BlockBuilder
import org.apache.calcite.linq4j.tree.Expressions
import org.apache.calcite.linq4j.tree.Types
import org.apache.calcite.plan.RelOptCluster
import org.apache.calcite.plan.RelOptCost
import org.apache.calcite.plan.RelOptPlanner
import org.apache.calcite.plan.RelOptTable
import org.apache.calcite.plan.RelTraitSet
import org.apache.calcite.prepare.Prepare
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.core.TableModify
import org.apache.calcite.rel.metadata.RelMetadataQuery
import org.apache.calcite.rex.RexNode
import org.apache.calcite.schema.ModifiableTable
import org.apache.calcite.util.BuiltInMethod
import java.lang.reflect.Method

/**
 * A [TableModify] expression that modifies a workload trace.
 */
internal class TraceTableModify(
    cluster: RelOptCluster,
    traitSet: RelTraitSet,
    table: RelOptTable,
    schema: Prepare.CatalogReader,
    input: RelNode,
    operation: Operation,
    updateColumnList: List<String>?,
    sourceExpressionList: List<RexNode>?,
    flattened: Boolean
) : TableModify(cluster, traitSet, table, schema, input, operation, updateColumnList, sourceExpressionList, flattened),
    EnumerableRel {
    init {
        // Make sure the table is modifiable
        table.unwrap(ModifiableTable::class.java) ?: throw AssertionError() // TODO: user error in validator
    }

    override fun copy(traitSet: RelTraitSet, inputs: List<RelNode>?): RelNode {
        return TraceTableModify(
            cluster,
            traitSet,
            table,
            getCatalogReader(),
            sole(inputs),
            operation,
            updateColumnList,
            sourceExpressionList,
            isFlattened
        )
    }

    override fun computeSelfCost(planner: RelOptPlanner, mq: RelMetadataQuery?): RelOptCost {
        // Prefer this plan compared to the standard EnumerableTableModify.
        return super.computeSelfCost(planner, mq)!!.multiplyBy(.1)
    }

    override fun implement(implementor: EnumerableRelImplementor, pref: Prefer): EnumerableRel.Result {
        val builder = BlockBuilder()
        val result = implementor.visitChild(this, 0, getInput() as EnumerableRel, pref)
        val childExp = builder.append("child", result.block)
        val convertedChildExpr = if (getInput().rowType != rowType) {
            val typeFactory = cluster.typeFactory as JavaTypeFactory
            val format = EnumerableTableScan.deduceFormat(table)
            val physType = PhysTypeImpl.of(typeFactory, table.rowType, format)
            val childPhysType = result.physType
            val o = Expressions.parameter(childPhysType.javaRowType, "o")
            val expressionList = List(childPhysType.rowType.fieldCount) { i ->
                childPhysType.fieldReference(o, i, physType.getJavaFieldType(i))
            }

            builder.append(
                "convertedChild",
                Expressions.call(
                    childExp,
                    BuiltInMethod.SELECT.method,
                    Expressions.lambda<org.apache.calcite.linq4j.function.Function<*>>(physType.record(expressionList), o)
                )
            )
        } else {
            childExp
        }

        if (!isInsert) {
            throw UnsupportedOperationException("Deletion and update not supported")
        }

        val expression = table.getExpression(InsertableTable::class.java)
        builder.add(
            Expressions.return_(
                null,
                Expressions.call(
                    BuiltInMethod.SINGLETON_ENUMERABLE.method,
                    Expressions.call(
                        Long::class.java,
                        expression,
                        INSERT_METHOD,
                        convertedChildExpr
                    )
                )
            )
        )

        val rowFormat = if (pref === Prefer.ARRAY) JavaRowFormat.ARRAY else JavaRowFormat.SCALAR
        val physType = PhysTypeImpl.of(implementor.typeFactory, getRowType(), rowFormat)
        return implementor.result(physType, builder.toBlock())
    }

    private companion object {
        /**
         * Reference to [InsertableTable.insert] method.
         */
        val INSERT_METHOD: Method = Types.lookupMethod(InsertableTable::class.java, "insert", Enumerable::class.java)
    }
}
