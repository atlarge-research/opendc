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

import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.PostgreSQL81Dialect
import org.hibernate.internal.SessionImpl
import org.hibernate.type.descriptor.ValueBinder
import org.hibernate.type.descriptor.ValueExtractor
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaTypeDescriptor
import org.hibernate.type.descriptor.sql.BasicBinder
import org.hibernate.type.descriptor.sql.BasicExtractor
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor
import java.sql.*

/**
 * A [SqlTypeDescriptor] that automatically selects the correct implementation for the database dialect.
 */
internal object JsonSqlTypeDescriptor : SqlTypeDescriptor {

    override fun getSqlType(): Int = Types.OTHER

    override fun canBeRemapped(): Boolean = true

    override fun <X> getExtractor(javaTypeDescriptor: JavaTypeDescriptor<X>): ValueExtractor<X> {
        return object : BasicExtractor<X>(javaTypeDescriptor, this) {
            private var delegate: AbstractJsonSqlTypeDescriptor? = null

            override fun doExtract(rs: ResultSet, name: String, options: WrapperOptions): X {
                return javaTypeDescriptor.wrap(delegate(options).extractJson(rs, name), options)
            }

            override fun doExtract(statement: CallableStatement, index: Int, options: WrapperOptions): X {
                return javaTypeDescriptor.wrap(delegate(options).extractJson(statement, index), options)
            }

            override fun doExtract(statement: CallableStatement, name: String, options: WrapperOptions): X {
                return javaTypeDescriptor.wrap(delegate(options).extractJson(statement, name), options)
            }

            private fun delegate(options: WrapperOptions): AbstractJsonSqlTypeDescriptor {
                var delegate = delegate
                if (delegate == null) {
                    delegate = resolveSqlTypeDescriptor(options)
                    this.delegate = delegate
                }
                return delegate
            }
        }
    }

    override fun <X> getBinder(javaTypeDescriptor: JavaTypeDescriptor<X>): ValueBinder<X> {
        return object : BasicBinder<X>(javaTypeDescriptor, this) {
            private var delegate: ValueBinder<X>? = null

            override fun doBind(st: PreparedStatement, value: X, index: Int, options: WrapperOptions) {
                delegate(options).bind(st, value, index, options)
            }

            override fun doBind(st: CallableStatement, value: X, name: String, options: WrapperOptions) {
                delegate(options).bind(st, value, name, options)
            }

            private fun delegate(options: WrapperOptions): ValueBinder<X> {
                var delegate = delegate
                if (delegate == null) {
                    delegate = checkNotNull(resolveSqlTypeDescriptor(options).getBinder(javaTypeDescriptor))
                    this.delegate = delegate
                }
                return delegate
            }
        }
    }

    /**
     * Helper method to resolve the appropriate [SqlTypeDescriptor] based on the [WrapperOptions].
     */
    private fun resolveSqlTypeDescriptor(options: WrapperOptions): AbstractJsonSqlTypeDescriptor {
        val session = options as? SessionImpl
        return when (session?.jdbcServices?.dialect) {
            is PostgreSQL81Dialect -> JsonBinarySqlTypeDescriptor
            is H2Dialect -> JsonBytesSqlTypeDescriptor
            else -> JsonStringSqlTypeDescriptor
        }
    }
}
