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
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.intellij.lang.annotations.Language
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Grant
import org.opendc.web.dispatcher.NO_EXIT_CODE
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.launcher.PeakMemory
import java.time.Instant
import java.util.UUID

/** Lifecycle of one dispatched execution. */
enum class ExecutionState {
    /** Written down, waiting for the platform to have room. */
    QUEUED,

    /** Handed to the platform, which has not started it yet. */
    SUBMITTED,

    /** The platform saw it start. */
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    ;

    val isTerminal: Boolean
        get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
}

/** Lifecycle of one `(scenario, seed)` run, and of its part in each execution that carried it. */
enum class UnitState {
    /** No execution is carrying it. Never the state of an execution's own record of it. */
    QUEUED,

    /** A live execution is carrying it. */
    CARRIED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    ;

    val isTerminal: Boolean
        get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
}

/** Which of the platform span columns are filled in. */
enum class SpanKind { NOT_STARTED, STARTED, RAN }

/** Whether peak memory was measured. */
enum class PeakMemoryKind { MEASURED, UNMEASURED }

/** What the platform has said about when an execution ran. */
sealed interface ObservedSpan {
    data object NotStarted : ObservedSpan

    /** It started and has not been seen to end. */
    data class Started(val at: Instant) : ObservedSpan

    data class Ran(
        val startedAt: Instant,
        val endedAt: Instant,
    ) : ObservedSpan
}

/**
 * One `(scenario, seed)` run of a submitted experiment, written at submit. A unit outlives the
 * executions that carry it: retrying puts it in a new one without losing where it has been.
 */
@Entity
@Table(name = "run_units")
class RunUnit : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    lateinit var experiment: Experiment

    var scenarioIndex: Int = 0

    var seed: Long = 0

    @Enumerated(EnumType.STRING)
    var state: UnitState = UnitState.QUEUED

    /** Written once at submit from the document's workload, so the progress denominator never moves. */
    var totalTasks: Int = 0

    var completedTasks: Int = 0

    /** What this run was estimated to cost when it was admitted, which its submitter holds in reserve. */
    var quotedSeconds: Double = 0.0

    companion object : PanacheCompanion<RunUnit> {
        @Language("JPAQL")
        private const val RESERVED_BY = """
            SELECT COALESCE(SUM(u.quotedSeconds), 0.0) FROM RunUnit u
            WHERE u.experiment.submittedBy.id = ?1
              AND u.state IN (org.opendc.web.server.model.UnitState.QUEUED, org.opendc.web.server.model.UnitState.CARRIED)
        """

        @Language("JPAQL")
        private const val BY_EXPERIMENT = """
            SELECT u FROM RunUnit u
            WHERE u.experiment.id = ?1
            ORDER BY u.scenarioIndex, u.seed
        """

        @Language("JPAQL")
        private const val QUEUED_WORK = """
            SELECT u FROM RunUnit u
            WHERE u.state = org.opendc.web.server.model.UnitState.QUEUED
            ORDER BY u.experiment.id, u.scenarioIndex, u.seed
        """

        @Language("JPAQL")
        private const val QUEUED_OF_EXPERIMENT = """
            SELECT u FROM RunUnit u
            WHERE u.experiment.id = ?1
              AND u.state = org.opendc.web.server.model.UnitState.QUEUED
            ORDER BY u.scenarioIndex, u.seed
        """

        fun findByExperiment(experimentId: Long): List<RunUnit> = list(BY_EXPERIMENT, experimentId)

        /** Everything submitted that no execution is carrying yet. */
        fun findQueued(): List<RunUnit> = list(QUEUED_WORK)

        /** The queued units of one experiment, locked, so a claim and a cancel cannot both take them. */
        fun lockQueued(experimentId: Long): List<RunUnit> =
            find(QUEUED_OF_EXPERIMENT, experimentId).withLock(LockModeType.PESSIMISTIC_WRITE).list()

        /** The quotes of everything [userId] submitted that has not finished yet. */
        fun reservedBy(userId: Long): Double =
            getEntityManager()
                .createQuery(RESERVED_BY, Double::class.javaObjectType)
                .setParameter(1, userId)
                .singleResult
    }
}

/**
 * One attempt at running a bag of units together.
 *
 * [publicId] is what the platform is asked about, so a restarted server can cancel and reconcile
 * work it did not start. [state] names the variant, and the schema checks every other column
 * against it.
 */
@Entity
@Table(name = "executions")
class Execution : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var publicId: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    lateinit var experiment: Experiment

    /** What this attempt carried, and how each unit of it ended. */
    @OneToMany(mappedBy = "execution")
    var carried: MutableList<ExecutionUnit> = mutableListOf()

    @Enumerated(EnumType.STRING)
    var state: ExecutionState = ExecutionState.QUEUED

    var attempt: Int = 1

    /** The hash of the launcher's telemetry credential, absent until [state] says the work was handed over. */
    var tokenHash: String? = null

    lateinit var dispatcher: String

    var parallelism: Int = 1

    var heapMb: Int = 0

    var memoryRequestMb: Int = 0

    var timeLimitSeconds: Int = 0

    var estimatedMakespanSeconds: Double = 0.0

    var runtimeMultiplier: Double = 1.0

    var memoryMultiplier: Double = 1.0

    lateinit var createdAt: Instant

    var submittedAt: Instant? = null

    @Enumerated(EnumType.STRING)
    var platformSpan: SpanKind = SpanKind.NOT_STARTED

    var platformStartedAt: Instant? = null

    var platformEndedAt: Instant? = null

    var settledAt: Instant? = null

    var exitCode: Int? = null

    @Enumerated(EnumType.STRING)
    var exitReason: ExitReason? = null

    var exitMessage: String = ""

    @Enumerated(EnumType.STRING)
    var peakMemoryKind: PeakMemoryKind = PeakMemoryKind.UNMEASURED

    var peakResidentMb: Double? = null

    var peakLiveHeapMb: Double? = null

    /** What the platform was asked to give this attempt. */
    val grant: Grant get() = Grant(parallelism, heapMb, memoryRequestMb, timeLimitSeconds)

    /** What the platform said about when it ran. */
    val span: ObservedSpan
        get() =
            when (platformSpan) {
                SpanKind.NOT_STARTED -> ObservedSpan.NotStarted
                SpanKind.STARTED -> ObservedSpan.Started(checkNotNull(platformStartedAt))
                SpanKind.RAN -> ObservedSpan.Ran(checkNotNull(platformStartedAt), checkNotNull(platformEndedAt))
            }

    val peakMemory: PeakMemory
        get() =
            when (peakMemoryKind) {
                PeakMemoryKind.UNMEASURED -> PeakMemory.Unmeasured
                PeakMemoryKind.MEASURED -> PeakMemory.Measured(checkNotNull(peakResidentMb), checkNotNull(peakLiveHeapMb))
            }

    /** What the process exited with, or [NO_EXIT_CODE] while it has not or where nothing was recorded. */
    val reportedExitCode: Int get() = exitCode ?: NO_EXIT_CODE

    /** Hands the work to the platform: out of the queue, with a fresh credential, returned once. */
    fun submit(at: Instant): String {
        state = ExecutionState.SUBMITTED
        submittedAt = at
        return grantToken()
    }

    /** Puts work the platform could not take back in the queue, keeping its attempt and its units. */
    fun release() {
        state = ExecutionState.QUEUED
        submittedAt = null
        tokenHash = null
    }

    /** Records the platform starting it. Only an execution not yet known to have started moves. */
    fun start(at: Instant) {
        if (state != ExecutionState.QUEUED && state != ExecutionState.SUBMITTED) {
            return
        }
        if (state == ExecutionState.QUEUED) {
            submittedAt = at
        }
        state = ExecutionState.RUNNING
        platformSpan = SpanKind.STARTED
        platformStartedAt = at
    }

    /**
     * Records how it ended. A span observed to start and then reported as never started keeps what was
     * observed: the platform forgetting a start is not evidence that nothing ran.
     */
    fun settle(
        state: ExecutionState,
        outcome: ExitOutcome,
        at: Instant,
    ) {
        if (this.state == ExecutionState.QUEUED) {
            submittedAt = at
        }
        this.state = state
        this.exitReason = outcome.reason
        this.exitCode = outcome.exitCode
        this.exitMessage = outcome.message
        this.settledAt = at
        when (val span = outcome.span) {
            is PlatformSpan.Ran -> {
                platformSpan = SpanKind.RAN
                platformStartedAt = span.startedAt
                platformEndedAt = span.endedAt
            }
            PlatformSpan.NotStarted -> {}
        }
        when (val peak = outcome.peakMemory) {
            is PeakMemory.Measured -> {
                peakMemoryKind = PeakMemoryKind.MEASURED
                peakResidentMb = peak.residentMb
                peakLiveHeapMb = peak.liveHeapMb
            }
            PeakMemory.Unmeasured -> {}
        }
    }

    /**
     * Mints the launcher's telemetry credential and returns it the one time it can be read. A fresh
     * one every time the work goes out, so a token cannot outlive the attempt it was minted for.
     */
    private fun grantToken(): String {
        val token = newSecret(EXECUTION_TOKEN_PREFIX, TOKEN_BYTES)
        tokenHash = sha256Hex(token)
        return token
    }

    companion object : PanacheCompanion<Execution> {
        @Language("JPAQL")
        private const val BY_EXPERIMENT = """
            SELECT e FROM Execution e
            WHERE e.experiment.id = ?1
            ORDER BY e.createdAt, e.id
        """

        @Language("JPAQL")
        private const val ON_PLATFORM = """
            SELECT e FROM Execution e
            WHERE e.state IN (
                org.opendc.web.server.model.ExecutionState.SUBMITTED,
                org.opendc.web.server.model.ExecutionState.RUNNING
            )
            ORDER BY e.createdAt, e.id
        """

        @Language("JPAQL")
        private const val WAITING = """
            SELECT e FROM Execution e
            WHERE e.state = org.opendc.web.server.model.ExecutionState.QUEUED
            ORDER BY e.createdAt, e.id
        """

        @Language("JPAQL")
        private const val ON_PLATFORM_OF_EXPERIMENT = """
            SELECT e.publicId FROM Execution e
            WHERE e.experiment.id = ?1
              AND e.state IN (
                org.opendc.web.server.model.ExecutionState.SUBMITTED,
                org.opendc.web.server.model.ExecutionState.RUNNING
              )
        """

        @Language("JPAQL")
        private const val ON_PLATFORM_OF_PROJECT = """
            SELECT e.publicId FROM Execution e
            WHERE e.experiment.project.id = ?1
              AND e.state IN (
                org.opendc.web.server.model.ExecutionState.SUBMITTED,
                org.opendc.web.server.model.ExecutionState.RUNNING
              )
        """

        @Language("JPAQL")
        private const val HELD_OF_EXPERIMENT = """
            SELECT new org.opendc.web.server.model.HeldCores(e.experiment.submittedBy.id, e.platformStartedAt, e.parallelism)
            FROM Execution e
            WHERE e.experiment.id = ?1
              AND e.platformSpan = org.opendc.web.server.model.SpanKind.STARTED
        """

        @Language("JPAQL")
        private const val HELD_OF_PROJECT = """
            SELECT new org.opendc.web.server.model.HeldCores(e.experiment.submittedBy.id, e.platformStartedAt, e.parallelism)
            FROM Execution e
            WHERE e.experiment.project.id = ?1
              AND e.platformSpan = org.opendc.web.server.model.SpanKind.STARTED
        """

        fun findByExperiment(experimentId: Long): List<Execution> = list(BY_EXPERIMENT, experimentId)

        /** The cores running executions of one experiment hold, read without loading them, as below. */
        fun heldByExperiment(experimentId: Long): List<HeldCores> =
            getEntityManager().createQuery(HELD_OF_EXPERIMENT, HeldCores::class.java).setParameter(1, experimentId).resultList

        fun heldByProject(projectId: Long): List<HeldCores> =
            getEntityManager().createQuery(HELD_OF_PROJECT, HeldCores::class.java).setParameter(1, projectId).resultList

        /** Executions this server believes a platform is holding. */
        fun findOnPlatform(): List<Execution> = list(ON_PLATFORM)

        /** Executions written down and waiting for room, oldest first. */
        fun findWaiting(): List<Execution> = list(WAITING)

        /**
         * The executions of one experiment a platform holds, by id only. Read without loading them,
         * because a caller about to delete the experiment cannot flush rows that still point at it.
         */
        fun onPlatformOfExperiment(experimentId: Long): List<UUID> =
            getEntityManager().createQuery(ON_PLATFORM_OF_EXPERIMENT, UUID::class.java).setParameter(1, experimentId).resultList

        /** The executions of every experiment of one project a platform holds, by id only, as above. */
        fun onPlatformOfProject(projectId: Long): List<UUID> =
            getEntityManager().createQuery(ON_PLATFORM_OF_PROJECT, UUID::class.java).setParameter(1, projectId).resultList

        fun findByPublicId(publicId: UUID): Execution? = find("publicId = ?1", publicId).firstResult()

        /** The execution behind [publicId], locked for the rest of the transaction. */
        fun lockByPublicId(publicId: UUID): Execution? =
            find("publicId = ?1", publicId).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult()

        /** The execution [token] was minted for, or none if it was never minted or has been replaced. */
        fun findByToken(token: String): Execution? = find("tokenHash = ?1", sha256Hex(token)).firstResult()

        /** Enough entropy that guessing one is not a strategy, and short enough to fit a header. */
        private const val TOKEN_BYTES = 24
    }
}

/** The cores a running execution holds, since when, and whose budget pays for them. */
data class HeldCores(
    val payerId: Long,
    val since: Instant,
    val cores: Int,
)

/** How one unit of an execution ended, as recorded on its row. */
sealed interface UnitVerdict {
    data class Succeeded(val elapsedSeconds: Double) : UnitVerdict

    data class Failed(
        val reason: ExitReason,
        val message: String,
    ) : UnitVerdict

    data object Cancelled : UnitVerdict
}

/**
 * One unit's part in one execution: what it was expected to cost there, and how it ended there.
 *
 * [state] names the variant: a carried unit has no reason yet, a succeeded one took a measured time,
 * and a failed or cancelled one carries the reason.
 */
@Entity
@Table(name = "execution_units")
class ExecutionUnit : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    lateinit var execution: Execution

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    lateinit var unit: RunUnit

    var estimatedSeconds: Double = 0.0

    var estimatedPeakMemoryMb: Double = 0.0

    @Enumerated(EnumType.STRING)
    var state: UnitState = UnitState.CARRIED

    @Enumerated(EnumType.STRING)
    var exitReason: ExitReason? = null

    var exitMessage: String = ""

    var elapsedSeconds: Double? = null

    fun record(verdict: UnitVerdict) {
        when (verdict) {
            is UnitVerdict.Succeeded -> {
                state = UnitState.SUCCEEDED
                exitReason = ExitReason.OK
                elapsedSeconds = verdict.elapsedSeconds
            }
            is UnitVerdict.Failed -> {
                state = UnitState.FAILED
                exitReason = verdict.reason
                exitMessage = verdict.message
            }
            UnitVerdict.Cancelled -> {
                state = UnitState.CANCELLED
                exitReason = ExitReason.CANCELLED
            }
        }
    }

    companion object : PanacheCompanion<ExecutionUnit> {
        // The execution and the unit are fetched with the row because every caller reads across all
        // three: the status fold reads a unit's progress beside how its latest attempt ended.
        @Language("JPAQL")
        private const val BY_EXPERIMENT = """
            SELECT eu FROM ExecutionUnit eu
            JOIN FETCH eu.execution e
            JOIN FETCH eu.unit u
            WHERE e.experiment.id = ?1
            ORDER BY e.createdAt, e.id
        """

        @Language("JPAQL")
        private const val BY_EXECUTION = """
            SELECT eu FROM ExecutionUnit eu
            JOIN FETCH eu.unit u
            WHERE eu.execution.id = ?1
            ORDER BY u.scenarioIndex, u.seed
        """

        @Language("JPAQL")
        private const val BY_EXECUTIONS = """
            SELECT eu FROM ExecutionUnit eu
            JOIN FETCH eu.unit u
            WHERE eu.execution.id IN ?1
            ORDER BY u.scenarioIndex, u.seed
        """

        // A unit failed here and nowhere since: no later attempt carries it.
        @Language("JPAQL")
        private const val RETRYABLE = """
            SELECT eu FROM ExecutionUnit eu
            JOIN FETCH eu.unit u
            WHERE eu.execution.id = ?1
              AND eu.state = org.opendc.web.server.model.UnitState.FAILED
              AND u.state = org.opendc.web.server.model.UnitState.FAILED
              AND NOT EXISTS (
                SELECT later FROM ExecutionUnit later
                WHERE later.unit = u AND later.id > eu.id
              )
            ORDER BY u.scenarioIndex, u.seed
        """

        /** Every attempt's record of every unit of an experiment, oldest attempt first. */
        fun findByExperiment(experimentId: Long): List<ExecutionUnit> = list(BY_EXPERIMENT, experimentId)

        fun findByExecution(executionId: Long): List<ExecutionUnit> = list(BY_EXECUTION, executionId)

        /** Every unit of several executions at once, for a page of them. */
        fun findByExecutions(executionIds: List<Long>): List<ExecutionUnit> =
            if (executionIds.isEmpty()) emptyList() else list(BY_EXECUTIONS, executionIds)

        /** The units [executionId] was the last attempt for and that ended failed. */
        fun findRetryable(executionId: Long): List<ExecutionUnit> = list(RETRYABLE, executionId)
    }
}
