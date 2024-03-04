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
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Type;
import org.opendc.web.proto.Targets;

/**
 * A portfolio is the composition of multiple scenarios.
 */
@Entity
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_portfolios_number",
                    columnNames = {"project_id", "number"})
        },
        indexes = {@Index(name = "ux_portfolios_number", columnList = "project_id, number")})
@NamedQueries({
    @NamedQuery(name = "Portfolio.findByProject", query = "SELECT p FROM Portfolio p WHERE p.project.id = :projectId"),
    @NamedQuery(
            name = "Portfolio.findOneByProject",
            query = "SELECT p FROM Portfolio p WHERE p.project.id = :projectId AND p.number = :number")
})
public class Portfolio extends PanacheEntityBase {

    /**
     * The main ID of a project.
     * The value starts at 6 to account for the other 5 projects already made by the loading script.
     */
    @Id
    @SequenceGenerator(name = "portfolioSeq", sequenceName = "portfolio_id_seq", allocationSize = 1, initialValue = 4)
    @GeneratedValue(generator = "portfolioSeq")
    public Long id;

    /**
     * The {@link Project} this portfolio belongs to.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    public Project project;

    /**
     * Unique number of the portfolio for the project.
     */
    @Column(nullable = false)
    public int number;

    /**
     * The name of this portfolio.
     */
    @Column(nullable = false)
    public String name;

    /**
     * The portfolio targets (metrics, repetitions).
     */
    @Column(columnDefinition = "jsonb", nullable = false, updatable = false)
    @Type(JsonType.class)
    public Targets targets;

    /**
     * The scenarios in this portfolio.
     */
    @OneToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "portfolio",
            orphanRemoval = true)
    @OrderBy("id ASC")
    public Set<Scenario> scenarios = new HashSet<>();

    /**
     * Construct a {@link Portfolio} object.
     */
    public Portfolio(Project project, int number, String name, Targets targets) {
        this.project = project;
        this.number = number;
        this.name = name;
        this.targets = targets;
    }

    /**
     * JPA constructor
     */
    protected Portfolio() {}

    /**
     * Find all {@link Portfolio}s that belong to the specified project
     *
     * @param projectId The unique identifier of the project.
     * @return The query of portfolios that belong to the specified project.
     */
    public static PanacheQuery<Portfolio> findByProject(long projectId) {
        return find("#Portfolio.findByProject", Parameters.with("projectId", projectId));
    }

    /**
     * Find the {@link Portfolio} with the specified <code>number</code> belonging to the specified project.
     *
     * @param projectId The unique identifier of the project.
     * @param number The number of the scenario.
     * @return The portfolio or <code>null</code> if it does not exist.
     */
    public static Portfolio findByProject(long projectId, int number) {
        return find(
                        "#Portfolio.findOneByProject",
                        Parameters.with("projectId", projectId).and("number", number))
                .firstResult();
    }
}
