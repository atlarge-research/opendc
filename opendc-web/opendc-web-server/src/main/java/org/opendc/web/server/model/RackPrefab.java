/*
 * Copyright (c) 2024 AtLarge Research
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
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.Type;
import org.opendc.web.proto.topology.Rack;

/**
 * A rack prefab in OpenDC.
 */
@Entity
@Table(
        name = "rack_prefabs",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_rack_prefabs_number",
                    columnNames = {"project_id", "number"})
        },
        indexes = {@Index(name = "ux_rack_prefabs_number", columnList = "project_id, number")})
@NamedQueries({
    @NamedQuery(name = "RackPrefab.findByProject", query = "SELECT r FROM RackPrefab r WHERE r.project.id = :projectId"),
    @NamedQuery(
            name = "RackPrefab.findOneByProject",
            query = "SELECT r FROM RackPrefab r WHERE r.project.id = :projectId AND r.number = :number")
})
public class RackPrefab extends PanacheEntityBase {
    /**
     * The main ID of a rack prefab.
     */
    @Id
    @SequenceGenerator(name = "rackPrefabSeq", sequenceName = "rack_prefab_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "rackPrefabSeq")
    public Long id;

    /**
     * The {@link Project} to which the rack prefab belongs.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    public Project project;

    /**
     * Unique number of the rack prefab for the project.
     */
    @Column(nullable = false)
    public int number;

    /**
     * The name of the rack prefab.
     */
    @Column(nullable = false)
    public String name;

    /**
     * The instant at which the rack prefab was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    /**
     * The instant at which the rack prefab was updated.
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /**
     * The rack design in JSON.
     */
    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(JsonType.class)
    public Rack rack;

    /**
     * Construct a {@link RackPrefab} object.
     */
    public RackPrefab(Project project, int number, String name, Instant createdAt, Rack rack) {
        this.project = project;
        this.number = number;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.rack = rack;
    }

    /**
     * JPA constructor
     */
    protected RackPrefab() {}

    /**
     * Find all [RackPrefab]s that belong to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @return The query of rack prefabs that belong to the specified project.
     */
    public static PanacheQuery<RackPrefab> findByProject(long projectId) {
        return find("#RackPrefab.findByProject", Parameters.with("projectId", projectId));
    }

    /**
     * Find the [RackPrefab] with the specified [number] belonging to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @param number The number of the rack prefab.
     * @return The rack prefab or `null` if it does not exist.
     */
    public static RackPrefab findByProject(long projectId, int number) {
        return find(
                        "#RackPrefab.findOneByProject",
                        Parameters.with("projectId", projectId).and("number", number))
                .firstResult();
    }
}
