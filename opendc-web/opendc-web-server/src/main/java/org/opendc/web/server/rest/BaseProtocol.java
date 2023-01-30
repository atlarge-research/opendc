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

import org.opendc.web.server.model.Trace;
import org.opendc.web.server.model.Workload;

/**
 * DTO-conversions for the base protocol.
 */
public final class BaseProtocol {
    /**
     * Private constructor to prevent instantiation of class.
     */
    private BaseProtocol() {}

    /**
     * Convert a {@link Workload} entity into a DTO.
     */
    public static org.opendc.web.proto.Workload toDto(Workload workload) {
        return new org.opendc.web.proto.Workload(toDto(workload.trace), workload.samplingFraction);
    }

    /**
     * Convert a {@link Trace] entity into a {@link org.opendc.web.proto.Trace} DTO.
     */
    public static org.opendc.web.proto.Trace toDto(Trace trace) {
        return new org.opendc.web.proto.Trace(trace.id, trace.name, trace.type);
    }
}
