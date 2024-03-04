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

package org.opendc.web.server.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Type;
import org.opendc.web.proto.OperationalPhenomena;

/**
 * A single scenario to be explored by the simulator.
 */
@Entity
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_scenarios_number",
                    columnNames = {"project_id", "number"})
        },
        indexes = {@Index(name = "ux_scenarios_number", columnList = "project_id, number")})
@NamedQueries({
    @NamedQuery(name = "Scenario.findByProject", query = "SELECT s FROM Scenario s WHERE s.project.id = :projectId"),
    @NamedQuery(
            name = "Scenario.findByPortfolio",
            query =
                    """
                SELECT s
                FROM Scenario s
                JOIN Portfolio p ON p.id = s.portfolio.id AND p.number = :number
                WHERE s.project.id = :projectId
            """),
    @NamedQuery(
            name = "Scenario.findOneByProject",
            query = "SELECT s FROM Scenario s WHERE s.project.id = :projectId AND s.number = :number")
})
public class Scenario extends PanacheEntityBase {
    /**
     * The main ID of a Scenario.
     * The value starts at 3 to account for the other 2 scenarios already made by the loading script.
     */
    @Id
    @SequenceGenerator(name = "scenarioSeq", sequenceName = "scenario_id_seq", allocationSize = 1, initialValue = 3)
    @GeneratedValue(generator = "scenarioSeq")
    public Long id;

    /**
     * The {@link Project} to which this scenario belongs.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_scenarios_project"))
    public Project project;

    /**
     * The {@link Portfolio} to which this scenario belongs.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_scenarios_portfolio"))
    public Portfolio portfolio;

    /**
     * Unique number of the scenario for the project.
     */
    @Column(nullable = false)
    public int number;

    /**
     * The name of the scenario.
     */
    @Column(nullable = false, updatable = false)
    public String name;

    /**
     * Workload details of the scenario.
     */
    @Embedded
    public Workload workload;

    /**
     * Topology details of the scenario.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "topology_id", nullable = false, foreignKey = @ForeignKey(name = "fk_scenarios_topology"))
    public Topology topology;

    /**
     * Operational phenomena activated in the scenario.
     *         @Column(columnDefinition = "jsonb", nullable = false, updatable = false)
     *     @Type(JsonType.class)
     */
    @Column(columnDefinition = "jsonb", nullable = false, updatable = false)
    @Type(JsonType.class)
    public OperationalPhenomena phenomena;

    /**
     * The name of the VM scheduler used in the scenario.
     */
    @Column(name = "scheduler_name", nullable = false, updatable = false)
    public String schedulerName;

    /**
     * The {@link Job} associated with the scenario.
     */
    @OneToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "scenario",
            fetch = FetchType.LAZY)
    public List<Job> jobs = new ArrayList<>();

    /**
     * Construct a {@link Scenario} object.
     */
    public Scenario(
            Project project,
            Portfolio portfolio,
            int number,
            String name,
            Workload workload,
            Topology topology,
            OperationalPhenomena phenomena,
            String schedulerName) {
        this.project = project;
        this.portfolio = portfolio;
        this.number = number;
        this.name = name;
        this.workload = workload;
        this.topology = topology;
        this.phenomena = phenomena;
        this.schedulerName = schedulerName;
    }

    /**
     * JPA constructor
     */
    protected Scenario() {}

    /**
     * Find all {@link Scenario}s that belong to the specified project
     *
     * @param projectId The unique identifier of the project.
     * @return The query of scenarios that belong to the specified project.
     */
    public static PanacheQuery<Scenario> findByProject(long projectId) {
        return find("#Scenario.findByProject", Parameters.with("projectId", projectId));
    }

    /**
     * Find all {@link Scenario}s that belong to the specified portfolio.
     *
     * @param projectId The unique identifier of the project.
     * @param number The number of the portfolio.
     * @return The query of scenarios that belong to the specified project and portfolio..
     */
    public static PanacheQuery<Scenario> findByPortfolio(long projectId, int number) {
        return find(
                "#Scenario.findByPortfolio",
                Parameters.with("projectId", projectId).and("number", number));
    }

    /**
     * Find the {@link Scenario} with the specified <code>number</code> belonging to the specified project.
     *
     * @param projectId The unique identifier of the project.
     * @param number The number of the scenario.
     * @return The scenario or <code>null</code> if it does not exist.
     */
    public static Scenario findByProject(long projectId, int number) {
        return find(
                        "#Scenario.findOneByProject",
                        Parameters.with("projectId", projectId).and("number", number))
                .firstResult();
    }
}
