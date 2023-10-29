/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.server.rest.error;

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.opendc.web.proto.ProtocolError;

/**
 * An [ExceptionMapper] for [MissingKotlinParameterException] thrown by Jackson.
 */
@Provider
public final class MissingKotlinParameterExceptionMapper implements ExceptionMapper<MissingKotlinParameterException> {
    @Override
    public Response toResponse(MissingKotlinParameterException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ProtocolError(
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        "Field " + exception.getParameter().getName() + " is missing from body."))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
