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
import org.opendc.web.proto.JobState
import java.time.Instant
import javax.persistence.*

/**
 * A simulation job to be run by the simulator.
 */
@TypeDef(name = "json", typeClass = JsonType::class)
@Entity
@Table(name = "jobs")
@NamedQueries(
    value = [
        NamedQuery(
            name = "Job.findAll",
            query = "SELECT j FROM Job j WHERE j.state = :state"
        ),
        NamedQuery(
            name = "Job.updateOne",
            query = """
                UPDATE Job j
                SET j.state = :newState, j.updatedAt = :updatedAt, j.results = :results
                WHERE j.id = :id AND j.state = :oldState
            """
        )
    ]
)
class Job(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long,

    @OneToOne(optional = false, mappedBy = "job", fetch = FetchType.EAGER)
    @JoinColumn(name = "scenario_id", nullable = false)
    val scenario: Scenario,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    /**
     * The number of simulation runs to perform.
     */
    @Column(nullable = false, updatable = false)
    val repeats: Int
) {
    /**
     * The instant at which the job was updated.
     */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt

    /**
     * The state of the job.
     */
    @Column(nullable = false)
    var state: JobState = JobState.PENDING

    /**
     * Experiment results in JSON
     */
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    var results: Map<String, Any>? = null

    /**
     * Return a string representation of this job.
     */
    override fun toString(): String = "Job[id=$id,scenario=${scenario.id},state=$state]"
}
