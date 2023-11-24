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

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
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
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "scenario_id", foreignKey = @ForeignKey(name = "fk_jobs_scenario"), nullable = false)
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
    public Instant updatedAt;

    /**
     * The state of the job.
     */
    @Type(PostgreSQLEnumType.class)
    @Column(nullable = false, columnDefinition = "enum")
    @Enumerated(EnumType.STRING)
    public JobState state = JobState.PENDING;

    /**
     * The runtime of the job (in seconds).
     */
    @Column(nullable = false)
    public int runtime = 0;

    /**
     * Experiment results in JSON
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    public Map<String, ?> results = null;

    /**
     * Construct a {@link Job} instance.
     */
    public Job(Scenario scenario, String createdBy, Instant createdAt, int repeats) {
        this.createdBy = createdBy;
        this.scenario = scenario;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
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
     * @return A query for jobs that are in the specified state.
     */
    public static PanacheQuery<Job> findByState(JobState state) {
        return find("state", state);
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
        Panache.getEntityManager().refresh(this);
        return count > 0;
    }

    /**
     * Determine whether the job is allowed to transition to <code>newState</code>.
     *
     * @param newState The new state to transition to.
     * @return <code>true</code> if the transition to the new state is legal, <code>false</code> otherwise.
     */
    public boolean canTransitionTo(JobState newState) {
        // Note that we always allow transitions from the state
        return newState == this.state
                || switch (this.state) {
                    case PENDING -> newState == JobState.CLAIMED;
                    case CLAIMED -> newState == JobState.RUNNING || newState == JobState.FAILED;
                    case RUNNING -> newState == JobState.FINISHED || newState == JobState.FAILED;
                    case FINISHED, FAILED -> false;
                };
    }
}
