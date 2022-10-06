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

package org.opendc.web.server.model

import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.opendc.web.proto.Room
import org.opendc.web.server.util.hibernate.json.JsonType
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/**
 * A datacenter design in OpenDC.
 */
@TypeDef(name = "json", typeClass = JsonType::class)
@Entity
@Table(
    name = "topologies",
    uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "number"])],
    indexes = [Index(name = "fn_topologies_number", columnList = "project_id, number")]
)
@NamedQueries(
    value = [
        NamedQuery(
            name = "Topology.findAll",
            query = "SELECT t FROM Topology t WHERE t.project.id = :projectId"
        ),
        NamedQuery(
            name = "Topology.findOne",
            query = "SELECT t FROM Topology t WHERE t.project.id = :projectId AND t.number = :number"
        )
    ]
)
class Topology(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long,

    /**
     * Unique number of the topology for the project.
     */
    @Column(nullable = false)
    val number: Int,

    @Column(nullable = false)
    val name: String,

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    /**
     * Datacenter design in JSON
     */
    @Type(type = "json")
    @Column(columnDefinition = "jsonb", nullable = false)
    var rooms: List<Room> = emptyList()
) {
    /**
     * The instant at which the topology was updated.
     */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt

    /**
     * Return a string representation of this topology.
     */
    override fun toString(): String = "Topology[id=$id,name=$name,project=${project.id}]"
}
