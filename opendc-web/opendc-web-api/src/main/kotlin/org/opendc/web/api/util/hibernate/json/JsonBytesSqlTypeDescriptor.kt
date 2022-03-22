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

import org.hibernate.type.descriptor.ValueBinder
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaTypeDescriptor
import org.hibernate.type.descriptor.sql.BasicBinder
import java.io.UnsupportedEncodingException
import java.sql.*

/**
 * A [AbstractJsonSqlTypeDescriptor] that stores the JSON as UTF-8 encoded bytes.
 */
internal object JsonBytesSqlTypeDescriptor : AbstractJsonSqlTypeDescriptor() {
    private val CHARSET = Charsets.UTF_8

    override fun getSqlType(): Int {
        return Types.BINARY
    }

    override fun <X> getBinder(javaTypeDescriptor: JavaTypeDescriptor<X>): ValueBinder<X> {
        return object : BasicBinder<X>(javaTypeDescriptor, this) {
            override fun doBind(st: PreparedStatement, value: X, index: Int, options: WrapperOptions) {
                st.setBytes(index, toJsonBytes(javaTypeDescriptor.unwrap(value, String::class.java, options)))
            }

            override fun doBind(st: CallableStatement, value: X, name: String, options: WrapperOptions) {
                st.setBytes(name, toJsonBytes(javaTypeDescriptor.unwrap(value, String::class.java, options)))
            }
        }
    }

    override fun extractJson(rs: ResultSet, name: String): Any? {
        return fromJsonBytes(rs.getBytes(name))
    }

    override fun extractJson(statement: CallableStatement, index: Int): Any? {
        return fromJsonBytes(statement.getBytes(index))
    }

    override fun extractJson(statement: CallableStatement, name: String): Any? {
        return fromJsonBytes(statement.getBytes(name))
    }

    private fun toJsonBytes(jsonValue: String): ByteArray? {
        return try {
            jsonValue.toByteArray(CHARSET)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
    }

    private fun fromJsonBytes(jsonBytes: ByteArray?): String? {
        return if (jsonBytes == null) {
            null
        } else try {
            String(jsonBytes, CHARSET)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
    }
}
