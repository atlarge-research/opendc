package org.opendc.web.api.util.hibernate.json

import org.hibernate.type.descriptor.ValueBinder
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaTypeDescriptor
import org.hibernate.type.descriptor.sql.BasicBinder
import java.sql.*

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
