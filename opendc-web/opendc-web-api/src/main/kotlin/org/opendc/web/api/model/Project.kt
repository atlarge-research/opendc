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

package org.opendc.web.api.model

import java.time.Instant
import javax.persistence.*

/**
 * A project in OpenDC encapsulates all the datacenter designs and simulation runs for a set of users.
 */
@Entity
@Table(name = "projects")
@NamedQueries(
    value = [
        NamedQuery(
            name = "Project.findAll",
            query = """
        SELECT a
        FROM ProjectAuthorization a
        WHERE a.key.userId = :userId
    """
        ),
        NamedQuery(
            name = "Project.allocatePortfolio",
            query = """
        UPDATE Project p
        SET p.portfoliosCreated = :oldState + 1, p.updatedAt = :now
        WHERE p.id = :id AND p.portfoliosCreated = :oldState
    """
        ),
        NamedQuery(
            name = "Project.allocateTopology",
            query = """
        UPDATE Project p
        SET p.topologiesCreated = :oldState + 1, p.updatedAt = :now
        WHERE p.id = :id AND p.topologiesCreated = :oldState
    """
        ),
        NamedQuery(
            name = "Project.allocateScenario",
            query = """
        UPDATE Project p
        SET p.scenariosCreated = :oldState + 1, p.updatedAt = :now
        WHERE p.id = :id AND p.scenariosCreated = :oldState
    """
        )
    ]
)
class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long,

    @Column(nullable = false)
    var name: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
) {
    /**
     * The instant at which the project was updated.
     */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt

    /**
     * The portfolios belonging to this project.
     */
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "project", orphanRemoval = true)
    @OrderBy("id ASC")
    val portfolios: MutableSet<Portfolio> = mutableSetOf()

    /**
     * The number of portfolios created for this project (including deleted portfolios).
     */
    @Column(name = "portfolios_created", nullable = false)
    var portfoliosCreated: Int = 0

    /**
     * The topologies belonging to this project.
     */
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "project", orphanRemoval = true)
    @OrderBy("id ASC")
    val topologies: MutableSet<Topology> = mutableSetOf()

    /**
     * The number of topologies created for this project (including deleted topologies).
     */
    @Column(name = "topologies_created", nullable = false)
    var topologiesCreated: Int = 0

    /**
     * The scenarios belonging to this project.
     */
    @OneToMany(mappedBy = "project", orphanRemoval = true)
    val scenarios: MutableSet<Scenario> = mutableSetOf()

    /**
     * The number of scenarios created for this project (including deleted scenarios).
     */
    @Column(name = "scenarios_created", nullable = false)
    var scenariosCreated: Int = 0

    /**
     * The users authorized to access the project.
     */
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "project", orphanRemoval = true)
    val authorizations: MutableSet<ProjectAuthorization> = mutableSetOf()

    /**
     * Return a string representation of this project.
     */
    override fun toString(): String = "Project[id=$id,name=$name]"
}
