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

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import org.opendc.web.proto.JobState;

/**
 * A simulation job to be run by the simulator.
 */
@Entity
@Table(name = "jobs")
@NamedQueries({
    @NamedQuery(
            name = "Job.updateOne",
            query =
                    """
                UPDATE Job j
                SET j.state = :newState, j.updatedAt = :updatedAt, j.runtime = :runtime, j.results = :results
                WHERE j.id = :id AND j.state = :oldState
            """)
})
public class Job extends PanacheEntity {
    @OneToOne(optional = false, mappedBy = "job", fetch = FetchType.EAGER)
    @JoinColumn(name = "scenario_id", nullable = false)
    public Scenario scenario;

    @Column(name = "created_by", nullable = false, updatable = false)
    public String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    /**
     * The number of simulation runs to perform.
     */
    @Column(nullable = false, updatable = false)
    public int repeats;

    /**
     * The instant at which the job was updated.
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = createdAt;

    /**
     * The state of the job.
     */
    @Column(nullable = false)
    public JobState state = JobState.PENDING;

    /**
     * The runtime of the job (in seconds).
     */
    @Column(nullable = false)
    public int runtime = 0;

    /**
     * Experiment results in JSON
     */
    @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonType")
    @Column(columnDefinition = "jsonb")
    public Map<String, ?> results = null;

    /**
     * Construct a {@link Job} instance.
     */
    public Job(Scenario scenario, String createdBy, Instant createdAt, int repeats) {
        this.createdBy = createdBy;
        this.scenario = scenario;
        this.createdAt = createdAt;
        this.repeats = repeats;
    }

    /**
     * JPA constructor
     */
    protected Job() {}

    /**
     * Find {@link Job}s in the specified {@link JobState}.
     *
     * @param state The state of the jobs to find.
     * @return The list of jobs that are in the specified state.
     */
    public static List<Job> findByState(JobState state) {
        return find("state", state).list();
    }

    /**
     * Atomically update this job.
     *
     * @param newState The new state to enter into.
     * @param time The time at which the update occurs.
     * @param results The results to possible set.
     * @return <code>true</code> when the update succeeded`, <code>false</code> when there was a conflict.
     */
    public boolean updateAtomically(JobState newState, Instant time, int runtime, Map<String, ?> results) {
        long count = update(
                "#Job.updateOne",
                Parameters.with("id", id)
                        .and("oldState", state)
                        .and("newState", newState)
                        .and("updatedAt", time)
                        .and("runtime", runtime)
                        .and("results", results));
        return count > 0;
    }
}
