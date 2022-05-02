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

import org.apache.calcite.model.ModelHandler
import org.apache.calcite.schema.Schema
import org.apache.calcite.schema.SchemaFactory
import org.apache.calcite.schema.SchemaPlus
import org.opendc.trace.Trace
import java.io.File
import java.nio.file.Paths

/**
 * Factory that creates a [TraceSchema].
 *
 * This factory allows users to include a schema that references a trace in a `model.json` file.
 */
public class TraceSchemaFactory : SchemaFactory {
    override fun create(parentSchema: SchemaPlus, name: String, operand: Map<String, Any>): Schema {
        val base = operand[ModelHandler.ExtraOperand.BASE_DIRECTORY.camelName] as File?
        val pathParam = requireNotNull(operand["path"]) { "Trace path not specified" } as String
        val path = if (base != null) File(base, pathParam).toPath() else Paths.get(pathParam)

        val format = requireNotNull(operand["format"]) { "Trace format not specified" } as String
        val create = operand.getOrDefault("create", false) as Boolean

        val trace = if (create) Trace.create(path, format) else Trace.open(path, format)
        return TraceSchema(trace)
    }
}
