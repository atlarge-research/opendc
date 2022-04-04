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

package org.opendc.web.api.rest.error

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.opendc.web.proto.ProtocolError
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

/**
 * An [ExceptionMapper] for [MissingKotlinParameterException] thrown by Jackson.
 */
@Provider
class MissingKotlinParameterExceptionMapper : ExceptionMapper<MissingKotlinParameterException> {
    override fun toResponse(exception: MissingKotlinParameterException): Response {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ProtocolError(Response.Status.BAD_REQUEST.statusCode, "Field '${exception.parameter.name}' is missing from body."))
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}
