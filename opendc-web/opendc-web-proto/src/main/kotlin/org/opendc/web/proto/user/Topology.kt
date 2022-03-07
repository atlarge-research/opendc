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

package org.opendc.web.proto.user

import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.opendc.web.proto.Room
import java.time.Instant
import javax.validation.constraints.NotBlank

/**
 * Model for an OpenDC topology.
 */
public data class Topology(
    val id: Long,
    val number: Int,
    val project: Project,
    val name: String,
    val rooms: List<Room>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /**
     * Create a new topology for a project.
     */
    @Schema(name = "Topology.Create")
    public data class Create(
        @field:NotBlank(message = "Name must not be empty")
        val name: String,
        val rooms: List<Room>
    )

    /**
     * Update an existing topology.
     */
    @Schema(name = "Topology.Update")
    public data class Update(val rooms: List<Room>)

    /**
     * A summary view of a [Topology] provided for nested relations.
     *
     * @param id The unique identifier of the topology.
     * @param number The number of the topology for the project.
     * @param name The name of the topology.
     * @param createdAt The instant at which the topology was created.
     * @param updatedAt The instant at which the topology was updated.
     */
    @Schema(name = "Topology.Summary")
    public data class Summary(
        val id: Long,
        val number: Int,
        val name: String,
        val createdAt: Instant,
        val updatedAt: Instant,
    )
}
