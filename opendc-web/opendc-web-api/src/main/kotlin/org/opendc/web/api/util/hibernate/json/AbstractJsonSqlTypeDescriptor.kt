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

package org.opendc.web.api.util.hibernate.json

import org.hibernate.type.descriptor.ValueExtractor
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaTypeDescriptor
import org.hibernate.type.descriptor.sql.BasicExtractor
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor
import java.sql.CallableStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Abstract implementation of a [SqlTypeDescriptor] for Hibernate JSON type.
 */
internal abstract class AbstractJsonSqlTypeDescriptor : SqlTypeDescriptor {

    override fun getSqlType(): Int {
        return Types.OTHER
    }

    override fun canBeRemapped(): Boolean {
        return true
    }

    override fun <X> getExtractor(typeDescriptor: JavaTypeDescriptor<X>): ValueExtractor<X> {
        return object : BasicExtractor<X>(typeDescriptor, this) {
            override fun doExtract(rs: ResultSet, name: String, options: WrapperOptions): X {
                return typeDescriptor.wrap(extractJson(rs, name), options)
            }

            override fun doExtract(statement: CallableStatement, index: Int, options: WrapperOptions): X {
                return typeDescriptor.wrap(extractJson(statement, index), options)
            }

            override fun doExtract(statement: CallableStatement, name: String, options: WrapperOptions): X {
                return typeDescriptor.wrap(extractJson(statement, name), options)
            }
        }
    }

    open fun extractJson(rs: ResultSet, name: String): Any? {
        return rs.getObject(name)
    }

    open fun extractJson(statement: CallableStatement, index: Int): Any? {
        return statement.getObject(index)
    }

    open fun extractJson(statement: CallableStatement, name: String): Any? {
        return statement.getObject(name)
    }
}
