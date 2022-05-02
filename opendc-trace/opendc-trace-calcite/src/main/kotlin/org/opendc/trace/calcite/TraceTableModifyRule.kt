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

import org.apache.calcite.adapter.enumerable.EnumerableConvention
import org.apache.calcite.plan.Convention
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.convert.ConverterRule
import org.apache.calcite.rel.core.TableModify
import org.apache.calcite.rel.logical.LogicalTableModify
import org.apache.calcite.schema.ModifiableTable

/**
 * A [ConverterRule] from a [LogicalTableModify] to a [TraceTableModify].
 */
internal class TraceTableModifyRule(config: Config) : ConverterRule(config) {
    override fun convert(rel: RelNode): RelNode? {
        val modify = rel as TableModify
        val table = modify.table!!

        // Make sure that the table is modifiable
        if (table.unwrap(ModifiableTable::class.java) == null) {
            return null
        }

        val traitSet = modify.traitSet.replace(EnumerableConvention.INSTANCE)
        return TraceTableModify(
            modify.cluster, traitSet,
            table,
            modify.catalogReader,
            convert(modify.input, traitSet),
            modify.operation,
            modify.updateColumnList,
            modify.sourceExpressionList,
            modify.isFlattened
        )
    }

    companion object {
        /** Default configuration.  */
        val DEFAULT: Config = Config.INSTANCE
            .withConversion(LogicalTableModify::class.java, Convention.NONE, EnumerableConvention.INSTANCE, "TraceTableModificationRule")
            .withRuleFactory { config: Config -> TraceTableModifyRule(config) }
    }
}
