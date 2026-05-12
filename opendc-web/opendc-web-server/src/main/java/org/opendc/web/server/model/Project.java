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

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A project in OpenDC encapsulates all the datacenter designs and simulation runs for a set of users.
 */
@Entity
@Table(name = "projects")
@NamedQueries({
    @NamedQuery(
            name = "Project.findByUser",
            query =
                    """
            SELECT a
            FROM ProjectAuthorization a
            WHERE a.key.userId = :userId
        """),
    @NamedQuery(
            name = "Project.allocateExperiment",
            query =
                    """
            UPDATE Project p
            SET p.experimentsCreated = :oldState + 1, p.updatedAt = :now
            WHERE p.id = :id AND p.experimentsCreated = :oldState
        """),
    @NamedQuery(
            name = "Project.allocateTopology",
            query =
                    """
            UPDATE Project p
            SET p.topologiesCreated = :oldState + 1, p.updatedAt = :now
            WHERE p.id = :id AND p.topologiesCreated = :oldState
        """),
    @NamedQuery(
            name = "Project.allocateScenario",
            query =
                    """
            UPDATE Project p
            SET p.scenariosCreated = :oldState + 1, p.updatedAt = :now
            WHERE p.id = :id AND p.scenariosCreated = :oldState
        """),
    @NamedQuery(
            name = "Project.allocateRackPrefab",
            query =
                    """
            UPDATE Project p
            SET p.rackPrefabsCreated = :oldState + 1, p.updatedAt = :now
            WHERE p.id = :id AND p.rackPrefabsCreated = :oldState
        """),
    @NamedQuery(
            name = "Project.allocateMachinePrefab",
            query =
                    """
            UPDATE Project p
            SET p.machinePrefabsCreated = :oldState + 1, p.updatedAt = :now
            WHERE p.id = :id AND p.machinePrefabsCreated = :oldState
        """)
})
public class Project extends PanacheEntityBase {

    /**
     * The main ID of a project.
     * The value starts at 6 to account for the other 5 projects already made by the loading script.
     */
    @Id
    @SequenceGenerator(name = "projectSeq", sequenceName = "project_id_seq", allocationSize = 1, initialValue = 7)
    @GeneratedValue(generator = "projectSeq")
    public Long id;

    /**
     * The name of the project.
     */
    @Column(nullable = false)
    public String name;

    /**
     * The instant at which the project was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    /**
     * The instant at which the project was updated.
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /**
     * The experiments belonging to this project.
     */
    @OneToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "project",
            orphanRemoval = true)
    @OrderBy("id ASC")
    public Set<Experiment> experiments = new HashSet<>();

    /**
     * The number of experiments created for this project (including deleted experiments).
     */
    @Column(name = "experiments_created", nullable = false)
    public int experimentsCreated = 0;

    /**
     * The topologies belonging to this project.
     */
    @OneToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "project",
            orphanRemoval = true)
    @OrderBy("id ASC")
    public Set<Topology> topologies = new HashSet<>();

    /**
     * The number of topologies created for this project (including deleted topologies).
     */
    @Column(name = "topologies_created", nullable = false)
    public int topologiesCreated = 0;

    /**
     * The scenarios belonging to this project.
     */
    @OneToMany(mappedBy = "project", orphanRemoval = true)
    public Set<Scenario> scenarios = new HashSet<>();

    /**
     * The number of scenarios created for this project (including deleted scenarios).
     */
    @Column(name = "scenarios_created", nullable = false)
    public int scenariosCreated = 0;

    /**
     * The rack prefabs belonging to this project.
     */
    @OneToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "project",
            orphanRemoval = true)
    @OrderBy("id ASC")
    public Set<RackPrefab> rackPrefabs = new HashSet<>();

    /**
     * The number of rack prefabs created for this project (including deleted rack prefabs).
     */
    @Column(name = "rack_prefabs_created", nullable = false)
    public int rackPrefabsCreated = 0;

    /**
     * The machine prefabs belonging to this project.
     */
    @OneToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "project",
            orphanRemoval = true)
    @OrderBy("id ASC")
    public Set<MachinePrefab> machinePrefabs = new HashSet<>();

    /**
     * The number of machine prefabs created for this project (including deleted machine prefabs).
     */
    @Column(name = "machine_prefabs_created", nullable = false)
    public int machinePrefabsCreated = 0;

    /**
     * The users authorized to access the project.
     */
    @OneToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "project",
            orphanRemoval = true)
    public Set<ProjectAuthorization> authorizations = new HashSet<>();

    /**
     * Construct a {@link Project} object.
     */
    public Project(String name, Instant createdAt) {
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /**
     * JPA constructor
     */
    protected Project() {}

    /**
     * Allocate the next experiment number for the specified [project].
     *
     * @param time The time at which the new experiment is created.
     */
    public int allocateExperiment(Instant time) {
        for (int i = 0; i < 4; i++) {
            long count = update(
                    "#Project.allocateExperiment",
                    Parameters.with("id", id)
                            .and("oldState", experimentsCreated)
                            .and("now", time));
            if (count > 0) {
                return experimentsCreated + 1;
            } else {
                Panache.getEntityManager().refresh(this);
            }
        }

        throw new IllegalStateException("Failed to allocate next experiment");
    }

    /**
     * Allocate the next topology number for the specified [project].
     *
     * @param time The time at which the new topology is created.
     */
    public int allocateTopology(Instant time) {
        for (int i = 0; i < 4; i++) {
            long count = update(
                    "#Project.allocateTopology",
                    Parameters.with("id", id).and("oldState", topologiesCreated).and("now", time));
            if (count > 0) {
                return topologiesCreated + 1;
            } else {
                Panache.getEntityManager().refresh(this);
            }
        }

        throw new IllegalStateException("Failed to allocate next topology");
    }

    /**
     * Allocate the next scenario number for the specified [project].
     *
     * @param time The time at which the new scenario is created.
     */
    public int allocateScenario(Instant time) {
        for (int i = 0; i < 4; i++) {
            long count = update(
                    "#Project.allocateScenario",
                    Parameters.with("id", id).and("oldState", scenariosCreated).and("now", time));
            if (count > 0) {
                return scenariosCreated + 1;
            } else {
                Panache.getEntityManager().refresh(this);
            }
        }

        throw new IllegalStateException("Failed to allocate next scenario");
    }

    /**
     * Allocate the next rack prefab number for the specified [project].
     *
     * @param time The time at which the new rack prefab is created.
     */
    public int allocateRackPrefab(Instant time) {
        for (int i = 0; i < 4; i++) {
            long count = update(
                    "#Project.allocateRackPrefab",
                    Parameters.with("id", id)
                            .and("oldState", rackPrefabsCreated)
                            .and("now", time));
            if (count > 0) {
                return rackPrefabsCreated + 1;
            } else {
                Panache.getEntityManager().refresh(this);
            }
        }

        throw new IllegalStateException("Failed to allocate next rack prefab");
    }

    /**
     * Allocate the next machine prefab number for the specified [project].
     *
     * @param time The time at which the new machine prefab is created.
     */
    public int allocateMachinePrefab(Instant time) {
        for (int i = 0; i < 4; i++) {
            long count = update(
                    "#Project.allocateMachinePrefab",
                    Parameters.with("id", id)
                            .and("oldState", machinePrefabsCreated)
                            .and("now", time));
            if (count > 0) {
                return machinePrefabsCreated + 1;
            } else {
                Panache.getEntityManager().refresh(this);
            }
        }

        throw new IllegalStateException("Failed to allocate next machine prefab");
    }
}
