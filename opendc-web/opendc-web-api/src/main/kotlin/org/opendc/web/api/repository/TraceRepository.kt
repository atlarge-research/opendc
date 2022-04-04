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

package org.opendc.web.api.repository

import org.opendc.web.api.model.Trace
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.persistence.EntityManager

/**
 * A repository to manage [Trace] entities.
 */
@ApplicationScoped
class TraceRepository @Inject constructor(private val em: EntityManager) {
    /**
     * Find all workload traces in the database.
     *
     * @return The list of available workload traces.
     */
    fun findAll(): List<Trace> {
        return em.createNamedQuery("Trace.findAll", Trace::class.java).resultList
    }

    /**
     * Find the [Trace] with the specified [id].
     *
     * @param id The unique identifier of the trace.
     * @return The trace or `null` if it does not exist.
     */
    fun findOne(id: String): Trace? {
        return em.find(Trace::class.java, id)
    }
}
