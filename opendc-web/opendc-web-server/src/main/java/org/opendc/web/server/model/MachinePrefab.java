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
import org.opendc.web.proto.topology.Machine;

/**
 * A machine prefab in OpenDC.
 */
@Entity
@Table(
        name = "machine_prefabs",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_machine_prefabs_number",
                    columnNames = {"project_id", "number"})
        },
        indexes = {@Index(name = "ux_machine_prefabs_number", columnList = "project_id, number")})
@NamedQueries({
    @NamedQuery(
            name = "MachinePrefab.findByProject",
            query = "SELECT m FROM MachinePrefab m WHERE m.project.id = :projectId"),
    @NamedQuery(
            name = "MachinePrefab.findOneByProject",
            query = "SELECT m FROM MachinePrefab m WHERE m.project.id = :projectId AND m.number = :number")
})
public class MachinePrefab extends PanacheEntityBase {
    /**
     * The main ID of a machine prefab.
     */
    @Id
    @SequenceGenerator(name = "machinePrefabSeq", sequenceName = "machine_prefab_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "machinePrefabSeq")
    public Long id;

    /**
     * The {@link Project} to which the machine prefab belongs.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    public Project project;

    /**
     * Unique number of the machine prefab for the project.
     */
    @Column(nullable = false)
    public int number;

    /**
     * The name of the machine prefab.
     */
    @Column(nullable = false)
    public String name;

    /**
     * The instant at which the machine prefab was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    /**
     * The instant at which the machine prefab was updated.
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /**
     * The machine design in JSON.
     */
    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(JsonType.class)
    public Machine machine;

    /**
     * Construct a {@link MachinePrefab} object.
     */
    public MachinePrefab(Project project, int number, String name, Instant createdAt, Machine machine) {
        this.project = project;
        this.number = number;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.machine = machine;
    }

    /**
     * JPA constructor
     */
    protected MachinePrefab() {}

    /**
     * Find all [MachinePrefab]s that belong to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @return The query of machine prefabs that belong to the specified project.
     */
    public static PanacheQuery<MachinePrefab> findByProject(long projectId) {
        return find("#MachinePrefab.findByProject", Parameters.with("projectId", projectId));
    }

    /**
     * Find the [MachinePrefab] with the specified [number] belonging to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @param number The number of the machine prefab.
     * @return The machine prefab or `null` if it does not exist.
     */
    public static MachinePrefab findByProject(long projectId, int number) {
        return find(
                        "#MachinePrefab.findOneByProject",
                        Parameters.with("projectId", projectId).and("number", number))
                .firstResult();
    }
}
