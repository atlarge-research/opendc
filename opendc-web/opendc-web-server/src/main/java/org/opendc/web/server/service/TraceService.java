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

package org.opendc.web.server.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.opendc.web.server.model.Trace;

/**
 * Service for managing {@link Trace}s.
 */
@ApplicationScoped
public final class TraceService {
    /**
     * Obtain all available workload traces.
     */
    public List<org.opendc.web.proto.Trace> findAll() {
        List<Trace> entities = Trace.listAll();
        return entities.stream().map(TraceService::toUserDto).toList();
    }

    /**
     * Obtain a workload trace by identifier.
     */
    public org.opendc.web.proto.Trace findById(String id) {
        return toUserDto(Trace.findById(id));
    }

    /**
     * Convert a {@link Trace] entity into a {@link org.opendc.web.proto.Trace} DTO.
     */
    public static org.opendc.web.proto.Trace toUserDto(Trace trace) {
        return new org.opendc.web.proto.Trace(trace.id, trace.name, trace.type);
    }
}
