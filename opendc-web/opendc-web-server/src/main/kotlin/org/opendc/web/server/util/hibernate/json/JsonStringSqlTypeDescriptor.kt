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

package org.opendc.web.server.util.hibernate.json

import org.hibernate.type.descriptor.ValueBinder
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaTypeDescriptor
import org.hibernate.type.descriptor.sql.BasicBinder
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * A [AbstractJsonSqlTypeDescriptor] that stores the JSON as string (VARCHAR).
 */
internal object JsonStringSqlTypeDescriptor : AbstractJsonSqlTypeDescriptor() {
    override fun getSqlType(): Int = Types.VARCHAR

    override fun <X> getBinder(typeDescriptor: JavaTypeDescriptor<X>): ValueBinder<X> {
        return object : BasicBinder<X>(typeDescriptor, this) {
            override fun doBind(st: PreparedStatement, value: X, index: Int, options: WrapperOptions) {
                st.setString(index, typeDescriptor.unwrap(value, String::class.java, options))
            }

            override fun doBind(st: CallableStatement, value: X, name: String, options: WrapperOptions) {
                st.setString(name, typeDescriptor.unwrap(value, String::class.java, options))
            }
        }
    }

    override fun extractJson(rs: ResultSet, name: String): Any? {
        return rs.getString(name)
    }

    override fun extractJson(statement: CallableStatement, index: Int): Any? {
        return statement.getString(index)
    }

    override fun extractJson(statement: CallableStatement, name: String): Any? {
        return statement.getString(name)
    }
}
