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

package org.opendc.web.server.rest;

import java.util.List;
import java.util.stream.Stream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.opendc.web.server.model.Trace;

/**
 * A resource representing the workload traces available in the OpenDC instance.
 */
@Produces("application/json")
@Path("/traces")
public final class TraceResource {
    /**
     * Obtain all available traces.
     */
    @GET
    public List<org.opendc.web.proto.Trace> getAll() {
        Stream<Trace> entities = Trace.streamAll();
        return entities.map(TraceResource::toDto).toList();
    }

    /**
     * Obtain trace information by identifier.
     */
    @GET
    @Path("{id}")
    public org.opendc.web.proto.Trace get(@PathParam("id") String id) {
        Trace trace = Trace.findById(id);

        if (trace == null) {
            throw new WebApplicationException("Trace not found", 404);
        }

        return toDto(trace);
    }

    /**
     * Convert a {@link Trace] entity into a {@link org.opendc.web.proto.Trace} DTO.
     */
    public static org.opendc.web.proto.Trace toDto(Trace trace) {
        return new org.opendc.web.proto.Trace(trace.id, trace.name, trace.type);
    }
}
