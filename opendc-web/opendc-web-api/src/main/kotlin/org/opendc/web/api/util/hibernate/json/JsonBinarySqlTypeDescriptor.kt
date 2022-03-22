package org.opendc.web.api.util.hibernate.json

import com.fasterxml.jackson.databind.JsonNode
import org.hibernate.type.descriptor.ValueBinder
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaTypeDescriptor
import org.hibernate.type.descriptor.sql.BasicBinder
import java.sql.CallableStatement
import java.sql.PreparedStatement

/**
 * A [AbstractJsonSqlTypeDescriptor] that stores the JSON as binary (JSONB).
 */
internal object JsonBinarySqlTypeDescriptor : AbstractJsonSqlTypeDescriptor() {
    override fun <X> getBinder(typeDescriptor: JavaTypeDescriptor<X>): ValueBinder<X> {
        return object : BasicBinder<X>(typeDescriptor, this) {
            override fun doBind(st: PreparedStatement, value: X, index: Int, options: WrapperOptions) {
                st.setObject(index, typeDescriptor.unwrap(value, JsonNode::class.java, options), sqlType)
            }

            override fun doBind(st: CallableStatement, value: X, name: String, options: WrapperOptions) {
                st.setObject(name, typeDescriptor.unwrap(value, JsonNode::class.java, options), sqlType)
            }
        }
    }
}
