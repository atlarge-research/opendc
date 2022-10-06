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
import org.opendc.web.proto.OperationalPhenomena
import org.opendc.web.server.util.hibernate.json.JsonType
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/**
 * A single scenario to be explored by the simulator.
 */
@TypeDef(name = "json", typeClass = JsonType::class)
@Entity
@Table(
    name = "scenarios",
    uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "number"])],
    indexes = [Index(name = "fn_scenarios_number", columnList = "project_id, number")]
)
@NamedQueries(
    value = [
        NamedQuery(
            name = "Scenario.findAll",
            query = "SELECT s FROM Scenario s WHERE s.project.id = :projectId"
        ),
        NamedQuery(
            name = "Scenario.findAllForPortfolio",
            query = """
        SELECT s
        FROM Scenario s
        JOIN Portfolio p ON p.id = s.portfolio.id AND p.number = :number
        WHERE s.project.id = :projectId
    """
        ),
        NamedQuery(
            name = "Scenario.findOne",
            query = "SELECT s FROM Scenario s WHERE s.project.id = :projectId AND s.number = :number"
        )
    ]
)
class Scenario(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long,

    /**
     * Unique number of the scenario for the project.
     */
    @Column(nullable = false)
    val number: Int,

    @Column(nullable = false, updatable = false)
    val name: String,

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,

    @ManyToOne(optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    val portfolio: Portfolio,

    @Embedded
    val workload: Workload,

    @ManyToOne(optional = false)
    val topology: Topology,

    @Type(type = "json")
    @Column(columnDefinition = "jsonb", nullable = false, updatable = false)
    val phenomena: OperationalPhenomena,

    @Column(name = "scheduler_name", nullable = false, updatable = false)
    val schedulerName: String
) {
    /**
     * The [Job] associated with the scenario.
     */
    @OneToOne(cascade = [CascadeType.ALL])
    lateinit var job: Job

    /**
     * Return a string representation of this scenario.
     */
    override fun toString(): String = "Scenario[id=$id,name=$name,project=${project.id},portfolio=${portfolio.id}]"
}
