/*
 * Copyright (c) 2026 AtLarge Research
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

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.util.UUID

/**
 * A project-scoped document row, storing its canonical body as jsonb next to a content hash. [id]
 * never leaves the server; the API carries [publicId], so identifiers cannot be walked from outside.
 */
@MappedSuperclass
abstract class SpecDocument : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var publicId: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    lateinit var project: Project

    lateinit var name: String

    lateinit var createdAt: Instant

    lateinit var updatedAt: Instant
}

@Entity
@Table(name = "topology_templates")
class TopologyTemplate : SpecDocument() {
    @JdbcTypeCode(SqlTypes.JSON)
    lateinit var topology: String

    lateinit var topologyHash: String

    @JdbcTypeCode(SqlTypes.JSON)
    var layout: String? = null

    companion object : PanacheCompanion<TopologyTemplate> {
        fun findByProject(projectId: Long): List<TopologyTemplate> = list("project.id = ?1 order by createdAt", projectId)

        fun findByPublicId(publicId: UUID): TopologyTemplate? = find("publicId = ?1", publicId).firstResult()
    }
}

/** Whether an experiment is still being written or has been frozen and queued. */
enum class ExperimentStage {
    DRAFT,
    SUBMITTED,
}

sealed interface Submission {
    data object Draft : Submission

    data class Submitted(
        val at: Instant,
        val by: UserAccount,
    ) : Submission
}

@Entity
@Table(name = "experiments")
class Experiment : SpecDocument() {
    @JdbcTypeCode(SqlTypes.JSON)
    lateinit var spec: String

    lateinit var specHash: String

    var scenarioCount: Int = 0

    var estimatedSimulationSeconds: Double = 0.0

    var estimatedBudgetSeconds: Double = 0.0

    @Enumerated(EnumType.STRING)
    var stage: ExperimentStage = ExperimentStage.DRAFT

    /** When the stage became [ExperimentStage.SUBMITTED], which a check constraint ties it to. */
    var submittedAt: Instant? = null

    /** Who submitted it, and so who pays for its runs. Set with [submittedAt]. */
    @ManyToOne(fetch = FetchType.LAZY)
    var submittedBy: UserAccount? = null

    val isDraft: Boolean
        get() = stage == ExperimentStage.DRAFT

    val submission: Submission
        get() =
            when (stage) {
                ExperimentStage.DRAFT -> Submission.Draft
                ExperimentStage.SUBMITTED ->
                    Submission.Submitted(
                        at = checkNotNull(submittedAt) { "a submitted experiment records when" },
                        by = checkNotNull(submittedBy) { "a submitted experiment records by whom" },
                    )
            }

    fun markSubmitted(
        by: UserAccount,
        at: Instant,
    ) {
        stage = ExperimentStage.SUBMITTED
        submittedBy = by
        submittedAt = at
    }

    companion object : PanacheCompanion<Experiment> {
        @Language("JPAQL")
        private const val OF_PROJECT = """
            SELECT x FROM Experiment x
            WHERE x.project.id = ?1
            ORDER BY x.createdAt DESC, x.id DESC
        """

        @Language("JPAQL")
        private const val OF_MEMBER = """
            SELECT x FROM Experiment x
            WHERE x.project.id IN (
                SELECT m.project.id FROM ProjectMember m
                WHERE m.user.id = ?1
            )
            ORDER BY x.createdAt DESC, x.id DESC
        """

        /** A project's experiments, newest first. */
        fun ofProject(projectId: Long): PanacheQuery<Experiment> = find(OF_PROJECT, projectId)

        /** The experiments of every project [userId] is a member of, newest first. */
        fun ofMember(userId: Long): PanacheQuery<Experiment> = find(OF_MEMBER, userId)

        fun findByPublicId(publicId: UUID): Experiment? = find("publicId = ?1", publicId).firstResult()

        /** The experiments of one project, by id only, so deleting the project flushes no rows that point at it. */
        fun publicIdsOfProject(projectId: Long): List<UUID> =
            getEntityManager()
                .createQuery("SELECT x.publicId FROM Experiment x WHERE x.project.id = ?1", UUID::class.java)
                .setParameter(1, projectId)
                .resultList
    }
}
