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
import org.hibernate.type.AbstractSingleColumnStandardBasicType
import org.hibernate.type.BasicType
import org.hibernate.usertype.DynamicParameterizedType
import java.util.*
import javax.enterprise.inject.spi.CDI

/**
 * A [BasicType] that contains JSON.
 */
class JsonType(objectMapper: ObjectMapper) : AbstractSingleColumnStandardBasicType<Any>(JsonSqlTypeDescriptor, JsonTypeDescriptor(objectMapper)), DynamicParameterizedType {
    /**
     * No-arg constructor for Hibernate to instantiate.
     */
    constructor() : this(CDI.current().select(ObjectMapper::class.java).get())

    override fun getName(): String = "json"

    override fun registerUnderJavaType(): Boolean = true

    override fun setParameterValues(parameters: Properties) {
        (javaTypeDescriptor as JsonTypeDescriptor).setParameterValues(parameters)
    }
}
