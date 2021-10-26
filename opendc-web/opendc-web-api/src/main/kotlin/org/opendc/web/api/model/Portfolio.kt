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

import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.opendc.web.api.util.hibernate.json.JsonType
import org.opendc.web.proto.Targets
import javax.persistence.*

/**
 * A portfolio is the composition of multiple scenarios.
 */
@TypeDef(name = "json", typeClass = JsonType::class)
@Entity
@Table(
    name = "portfolios",
    uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "number"])],
    indexes = [Index(name = "fn_portfolios_number", columnList = "project_id, number")]
)
@NamedQueries(
    value = [
        NamedQuery(
            name = "Portfolio.findAll",
            query = "SELECT p FROM Portfolio p WHERE p.project.id = :projectId"
        ),
        NamedQuery(
            name = "Portfolio.findOne",
            query = "SELECT p FROM Portfolio p WHERE p.project.id = :projectId AND p.number = :number"
        )
    ]
)
class Portfolio(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long,

    /**
     * Unique number of the portfolio for the project.
     */
    @Column(nullable = false)
    val number: Int,

    @Column(nullable = false)
    val name: String,

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,

    /**
     * The portfolio targets (metrics, repetitions).
     */
    @Type(type = "json")
    @Column(columnDefinition = "jsonb", nullable = false, updatable = false)
    val targets: Targets,
) {
    /**
     * The scenarios in this portfolio.
     */
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "portfolio", orphanRemoval = true)
    @OrderBy("id ASC")
    val scenarios: MutableSet<Scenario> = mutableSetOf()

    /**
     * Return a string representation of this portfolio.
     */
    override fun toString(): String = "Job[id=$id,name=$name,project=${project.id},targets=$targets]"
}
