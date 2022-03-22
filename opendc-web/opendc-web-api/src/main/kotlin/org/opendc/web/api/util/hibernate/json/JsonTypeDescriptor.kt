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

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.HibernateException
import org.hibernate.annotations.common.reflection.XProperty
import org.hibernate.annotations.common.reflection.java.JavaXMember
import org.hibernate.engine.jdbc.BinaryStream
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor
import org.hibernate.type.descriptor.java.BlobTypeDescriptor
import org.hibernate.type.descriptor.java.DataHelper
import org.hibernate.type.descriptor.java.MutableMutabilityPlan
import org.hibernate.usertype.DynamicParameterizedType
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.sql.Blob
import java.sql.SQLException
import java.util.*

/**
 * An [AbstractTypeDescriptor] implementation for Hibernate JSON type.
 */
internal class JsonTypeDescriptor(private val objectMapper: ObjectMapper) : AbstractTypeDescriptor<Any>(Any::class.java, JsonMutabilityPlan(objectMapper)), DynamicParameterizedType {
    private var type: Type? = null

    override fun setParameterValues(parameters: Properties) {
        val xProperty = parameters[DynamicParameterizedType.XPROPERTY] as XProperty
        type = if (xProperty is JavaXMember) {
            val x = xProperty as JavaXMember
            x.javaType
        } else {
            (parameters[DynamicParameterizedType.PARAMETER_TYPE] as DynamicParameterizedType.ParameterType).returnedClass
        }
    }

    override fun areEqual(one: Any?, another: Any?): Boolean {
        return when {
            one === another -> true
            one == null || another == null -> false
            one is String && another is String -> one == another
            one is Collection<*> && another is Collection<*> -> Objects.equals(one, another)
            else -> areJsonEqual(one, another)
        }
    }

    override fun toString(value: Any?): String {
        return objectMapper.writeValueAsString(value)
    }

    override fun fromString(string: String): Any? {
        return objectMapper.readValue(string, objectMapper.typeFactory.constructType(type))
    }

    override fun <X> unwrap(value: Any?, type: Class<X>, options: WrapperOptions): X? {
        if (value == null) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return when {
            String::class.java.isAssignableFrom(type) -> toString(value)
            BinaryStream::class.java.isAssignableFrom(type) || ByteArray::class.java.isAssignableFrom(type) -> {
                val stringValue = if (value is String) value else toString(value)
                BinaryStreamImpl(DataHelper.extractBytes(ByteArrayInputStream(stringValue.toByteArray())))
            }
            Blob::class.java.isAssignableFrom(type) -> {
                val stringValue = if (value is String) value else toString(value)
                BlobTypeDescriptor.INSTANCE.fromString(stringValue)
            }
            Any::class.java.isAssignableFrom(type) -> toJsonType(value)
            else -> throw unknownUnwrap(type)
        } as X
    }

    override fun <X> wrap(value: X?, options: WrapperOptions): Any? {
        if (value == null) {
            return null
        }

        var blob: Blob? = null
        if (Blob::class.java.isAssignableFrom(value.javaClass)) {
            blob = options.lobCreator.wrap(value as Blob?)
        } else if (ByteArray::class.java.isAssignableFrom(value.javaClass)) {
            blob = options.lobCreator.createBlob(value as ByteArray?)
        } else if (InputStream::class.java.isAssignableFrom(value.javaClass)) {
            val inputStream = value as InputStream
            blob = try {
                options.lobCreator.createBlob(inputStream, inputStream.available().toLong())
            } catch (e: IOException) {
                throw unknownWrap(value.javaClass)
            }
        }

        val stringValue: String = try {
            if (blob != null) String(DataHelper.extractBytes(blob.binaryStream)) else value.toString()
        } catch (e: SQLException) {
            throw HibernateException("Unable to extract binary stream from Blob", e)
        }

        return fromString(stringValue)
    }

    private class JsonMutabilityPlan(private val objectMapper: ObjectMapper) : MutableMutabilityPlan<Any>() {
        override fun deepCopyNotNull(value: Any): Any {
            return objectMapper.treeToValue(objectMapper.valueToTree(value), value.javaClass)
        }
    }

    private fun readObject(value: String): Any {
        return objectMapper.readTree(value)
    }

    private fun areJsonEqual(one: Any, another: Any): Boolean {
        return readObject(objectMapper.writeValueAsString(one)) == readObject(objectMapper.writeValueAsString(another))
    }

    private fun toJsonType(value: Any?): Any {
        return try {
            readObject(objectMapper.writeValueAsString(value))
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }
    }
}
