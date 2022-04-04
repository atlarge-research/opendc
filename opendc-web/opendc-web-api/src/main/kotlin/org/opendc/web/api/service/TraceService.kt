/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.web.api.service

import org.opendc.web.api.repository.TraceRepository
import org.opendc.web.proto.Trace
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Service for managing [Trace]s.
 */
@ApplicationScoped
class TraceService @Inject constructor(private val repository: TraceRepository) {
    /**
     * Obtain all available workload traces.
     */
    fun findAll(): List<Trace> {
        return repository.findAll().map { it.toUserDto() }
    }

    /**
     * Obtain a workload trace by identifier.
     */
    fun findById(id: String): Trace? {
        return repository.findOne(id)?.toUserDto()
    }
}
