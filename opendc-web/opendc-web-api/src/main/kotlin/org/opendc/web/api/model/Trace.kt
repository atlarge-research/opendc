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

package org.opendc.web.api.model

import javax.persistence.*

/**
 * A workload trace available for simulation.
 *
 * @param id The unique identifier of the trace.
 * @param name The name of the trace.
 * @param type The type of trace.
 */
@Entity
@Table(name = "traces")
@NamedQueries(
    value = [
        NamedQuery(
            name = "Trace.findAll",
            query = "SELECT t FROM Trace t"
        ),
    ]
)
class Trace(
    @Id
    val id: String,

    @Column(nullable = false, updatable = false)
    val name: String,

    @Column(nullable = false, updatable = false)
    val type: String,
) {
    /**
     * Return a string representation of this trace.
     */
    override fun toString(): String = "Trace[id=$id,name=$name,type=$type]"
}
