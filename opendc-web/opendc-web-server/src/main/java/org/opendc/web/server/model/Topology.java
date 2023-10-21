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

package org.opendc.web.server.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.Type;
import org.opendc.web.proto.Room;

/**
 * A datacenter design in OpenDC.
 */
@Entity
@Table(
        name = "topologies",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_topologies_number",
                    columnNames = {"project_id", "number"})
        },
        indexes = {@Index(name = "ux_topologies_number", columnList = "project_id, number")})
@NamedQueries({
    @NamedQuery(name = "Topology.findByProject", query = "SELECT t FROM Topology t WHERE t.project.id = :projectId"),
    @NamedQuery(
            name = "Topology.findOneByProject",
            query = "SELECT t FROM Topology t WHERE t.project.id = :projectId AND t.number = :number")
})
public class Topology extends PanacheEntity {
    /**
     * The {@link Project} to which the topology belongs.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    public Project project;

    /**
     * Unique number of the topology for the project.
     */
    @Column(nullable = false)
    public int number;

    /**
     * The name of the topology.
     */
    @Column(nullable = false)
    public String name;

    /**
     * The instant at which the topology was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    /**
     * The instant at which the topology was updated.
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /**
     * Datacenter design in JSON
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    public List<Room> rooms;

    /**
     * Construct a {@link Topology} object.
     */
    public Topology(Project project, int number, String name, Instant createdAt, List<Room> rooms) {
        this.project = project;
        this.number = number;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.rooms = rooms;
    }

    /**
     * JPA constructor
     */
    protected Topology() {}

    /**
     * Find all [Topology]s that belong to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @return The query of topologies that belong to the specified project.
     */
    public static PanacheQuery<Topology> findByProject(long projectId) {
        return find("#Topology.findByProject", Parameters.with("projectId", projectId));
    }

    /**
     * Find the [Topology] with the specified [number] belonging to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @param number The number of the topology.
     * @return The topology or `null` if it does not exist.
     */
    public static Topology findByProject(long projectId, int number) {
        return find(
                        "#Topology.findOneByProject",
                        Parameters.with("projectId", projectId).and("number", number))
                .firstResult();
    }
}
