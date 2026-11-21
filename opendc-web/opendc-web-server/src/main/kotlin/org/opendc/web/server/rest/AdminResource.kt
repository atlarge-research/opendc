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

package org.opendc.web.server.rest

import jakarta.annotation.security.RolesAllowed
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language
import org.jboss.resteasy.reactive.RestResponse
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.server.auth.Roles
import org.opendc.web.server.execution.ExecutionConfig
import org.opendc.web.server.execution.Settlement
import org.opendc.web.server.model.AccountState
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.ExecutionState
import org.opendc.web.server.model.ExecutionUnit
import org.opendc.web.server.model.HandleKind
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.Submission
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.logKey
import java.time.Duration
import java.time.Instant

/** How many times its estimated makespan a running execution may take before it is flagged as overdue. */
private const val STRAGGLER_FACTOR = 2.0

private const val MILLIS_PER_SECOND = 1000.0

private val LIVE = listOf(ExecutionState.QUEUED, ExecutionState.SUBMITTED, ExecutionState.RUNNING)

@Language("JPAQL")
private const val EXECUTIONS_IN = """
    SELECT e FROM Execution e
    JOIN FETCH e.experiment x
    JOIN FETCH x.project
    WHERE e.state IN ?1
    ORDER BY e.createdAt DESC
"""

/** Accounts whose handle or display name contains a pattern, with `!` escaping the pattern's wildcards. */
@Language("JPAQL")
private const val ACCOUNTS_MATCHING = """
    SELECT u FROM UserAccount u
    WHERE LOWER(u.handle) LIKE ?1 ESCAPE '!' OR LOWER(u.displayName) LIKE ?1 ESCAPE '!'
    ORDER BY u.createdAt DESC
"""

/**
 * What an operator looks at. The rows carry the names an operator needs without opening the
 * experiments themselves: there is no bypass of project membership.
 */
@Path("admin")
@RolesAllowed(Roles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AdminResource(
    private val settlement: Settlement,
    private val dispatcher: Dispatcher,
    private val store: ObjectStore,
    private val config: ExecutionConfig,
) {
    /** Executions newest first, live ones when no state is asked for. */
    @GET
    @Path("executions")
    fun executions(
        @QueryParam("state") states: List<String>,
        @QueryParam("limit") @DefaultValue(Window.DEFAULT_LIMIT) limit: Int,
        @QueryParam("offset") @DefaultValue(Window.DEFAULT_OFFSET) offset: Int,
    ): Page<AdminExecution> {
        val wanted = if (states.isEmpty()) LIVE else states.map(::stateOf)
        val now = Instant.now()
        val page = Execution.find(EXECUTIONS_IN, wanted).within(Window.of(offset, limit))
        val units = ExecutionUnit.findByExecutions(page.map { it.id }).groupBy { it.execution.id }
        // Counted on its own: a count of the fetching query would fetch associations it never selects.
        return Page(page.map { it.toAdmin(now, units[it.id].orEmpty()) }, Execution.count("state IN ?1", wanted))
    }

    @GET
    @Path("executions/{id}")
    fun execution(
        @PathParam("id") id: String,
    ): AdminExecutionDetail {
        val execution = Execution.findByPublicId(publicId(id, "Execution")) ?: throw notFound("Execution")
        val retryable = if (execution.state.isTerminal) ExecutionUnit.findRetryable(execution.id).size else 0
        val units = ExecutionUnit.findByExecution(execution.id)
        return AdminExecutionDetail(
            execution = execution.toAdmin(Instant.now(), units),
            units = units.map { it.toCarried() },
            retryableUnits = retryable,
        )
    }

    /** What the launcher wrote, collected when the execution ended. */
    @GET
    @Path("executions/{id}/logs")
    @Produces(TEXT_PLAIN_UTF8)
    fun logs(
        @PathParam("id") id: String,
    ): Response {
        val execution = Execution.findByPublicId(publicId(id, "Execution")) ?: throw notFound("Execution")
        val key = logKey(execution.experiment.publicId, execution.publicId)
        if (!store.exists(key)) {
            throw notFound("Log")
        }
        return Response.ok(StreamingOutput { out -> store.open(key).use { it.copyTo(out) } }).build()
    }

    /** Runs what failed in an execution again, past the automatic attempt cap. */
    @POST
    @Path("executions/{id}/retry")
    @Transactional
    fun retry(
        @PathParam("id") id: String,
    ): RestResponse<List<AdminExecution>> {
        val now = Instant.now()
        val queued = settlement.resubmit(publicId(id, "Execution")).map { it.toAdmin(now, ExecutionUnit.findByExecution(it.id)) }
        return RestResponse.ResponseBuilder.create<List<AdminExecution>>(201).entity(queued).build()
    }

    @GET
    @Path("capacity")
    fun capacity(): PlatformCapacity {
        val capacity = dispatcher.capacity()
        val slot = dispatcher.slot()
        return PlatformCapacity(
            dispatcher = dispatcher.name,
            totalCores = capacity.totalCores,
            allocatedCores = capacity.allocatedCores,
            totalMemoryMb = capacity.totalMemoryMb,
            allocatedMemoryMb = capacity.allocatedMemoryMb,
            slotCores = slot.cores,
            slotMemoryMb = slot.memoryMb,
            slotTimeCap =
                when (val cap = slot.timeCap) {
                    TimeCap.Unlimited -> TimeCapWire.Unlimited
                    is TimeCap.Limited -> TimeCapWire.Limited(cap.seconds)
                },
        )
    }

    /** Accounts whose handle or name contains [q], newest first. Read-only: plans change in the database. */
    @GET
    @Path("users")
    fun users(
        @QueryParam("q") @DefaultValue("") q: String,
        @QueryParam("limit") @DefaultValue(Window.DEFAULT_LIMIT) limit: Int,
        @QueryParam("offset") @DefaultValue(Window.DEFAULT_OFFSET) offset: Int,
    ): Page<AdminAccount> {
        val pattern = "%${q.trim().lowercase().replace("!", "!!").replace("%", "!%").replace("_", "!_")}%"
        val matching = UserAccount.find(ACCOUNTS_MATCHING, pattern)
        return Page(matching.within(Window.of(offset, limit)).map { it.toAdmin() }, matching.count())
    }

    private fun Execution.toAdmin(
        now: Instant,
        units: List<ExecutionUnit>,
    ): AdminExecution =
        AdminExecution(
            id = publicId.toString(),
            experimentId = experiment.publicId.toString(),
            experimentName = experiment.name,
            projectName = experiment.project.name,
            owner =
                when (val submission = experiment.submission) {
                    Submission.Draft -> ""
                    is Submission.Submitted -> submission.by.handle
                },
            dispatcher = dispatcher,
            attempt = attempt,
            cores = parallelism,
            memoryRequestMb = memoryRequestMb,
            timeLimitSeconds = timeLimitSeconds,
            unitCount = units.size,
            scenarios = units.map { it.unit.scenarioIndex }.distinct().sorted(),
            estimatedMakespanSeconds = estimatedMakespanSeconds,
            createdAt = createdAt.toString(),
            phase = phase(now),
        )

    private fun Execution.phase(now: Instant): ExecutionPhase =
        when (state) {
            ExecutionState.QUEUED -> ExecutionPhase.Queued
            ExecutionState.SUBMITTED ->
                ExecutionPhase.Submitted(
                    checkNotNull(submittedAt) { "a submitted execution records when" }.toString(),
                )
            ExecutionState.RUNNING -> {
                val started = checkNotNull(platformStartedAt) { "a running execution has a platform start" }
                val elapsed = Duration.between(started, now).toMillis() / MILLIS_PER_SECOND
                val straggler = elapsed > config.packing().startupSeconds() + STRAGGLER_FACTOR * estimatedMakespanSeconds
                ExecutionPhase.Running(started.toString(), elapsed, straggler)
            }
            ExecutionState.SUCCEEDED, ExecutionState.FAILED, ExecutionState.CANCELLED ->
                ExecutionPhase.Ended(
                    state = state.name.lowercase(),
                    settledAt = checkNotNull(settledAt) { "an ended execution records when it was settled" }.toString(),
                    reason = checkNotNull(exitReason) { "an ended execution records why" }.toWire(),
                    message = exitMessage,
                )
        }
}

private fun stateOf(raw: String): ExecutionState =
    ExecutionState.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: throw invalidDocument(
            "Unknown execution state",
            listOf(DocumentIssue("state", "is not one of ${ExecutionState.entries.joinToString { it.name.lowercase() }}")),
        )

private fun ExecutionUnit.toCarried(): CarriedUnit =
    CarriedUnit(
        scenarioIndex = unit.scenarioIndex,
        seed = unit.seed,
        estimatedSeconds = estimatedSeconds,
        estimatedPeakMemoryMb = estimatedPeakMemoryMb,
        outcome = outcome(),
    )

private fun UserAccount.toAdmin(): AdminAccount =
    AdminAccount(
        subject = subject,
        handle =
            when (handleKind) {
                HandleKind.PROVISIONAL -> HandleWire.Provisional
                HandleKind.CHOSEN -> HandleWire.Chosen(handle)
            },
        displayName = displayName,
        plan = planTier.toWire(),
        isAdmin = isAdmin,
        status =
            when (state) {
                AccountState.ACTIVE -> AccountStatus.Active
                AccountState.DEACTIVATED ->
                    AccountStatus.Deactivated(
                        checkNotNull(deactivatedAt) { "a deactivated account records when" }.toString(),
                    )
            },
        createdAt = createdAt.toString(),
        projectCount = ProjectMember.count("user.id = ?1", id).toInt(),
    )

@Serializable
data class AdminExecution(
    val id: String,
    val experimentId: String,
    val experimentName: String,
    val projectName: String,
    val owner: String,
    val dispatcher: String,
    val attempt: Int,
    val cores: Int,
    val memoryRequestMb: Int,
    val timeLimitSeconds: Int,
    val unitCount: Int,
    val scenarios: List<Int>,
    val estimatedMakespanSeconds: Double,
    val createdAt: String,
    val phase: ExecutionPhase,
)

/** Where an execution is in its life, with only the timestamps that state guarantees. */
@Serializable
sealed interface ExecutionPhase {
    @Serializable
    @SerialName("queued")
    data object Queued : ExecutionPhase

    @Serializable
    @SerialName("submitted")
    data class Submitted(val submittedAt: String) : ExecutionPhase

    @Serializable
    @SerialName("running")
    data class Running(
        val startedAt: String,
        val elapsedSeconds: Double,
        val straggler: Boolean,
    ) : ExecutionPhase

    @Serializable
    @SerialName("ended")
    data class Ended(
        val state: String,
        val settledAt: String,
        val reason: ExitReasonWire,
        val message: String,
    ) : ExecutionPhase
}

@Serializable
data class AdminExecutionDetail(
    val execution: AdminExecution,
    val units: List<CarriedUnit>,
    val retryableUnits: Int,
)

@Serializable
data class CarriedUnit(
    val scenarioIndex: Int,
    val seed: Long,
    val estimatedSeconds: Double,
    val estimatedPeakMemoryMb: Double,
    val outcome: CarriedOutcome,
)

@Serializable
data class PlatformCapacity(
    val dispatcher: String,
    val totalCores: Int,
    val allocatedCores: Int,
    val totalMemoryMb: Double,
    val allocatedMemoryMb: Double,
    val slotCores: Int,
    val slotMemoryMb: Double,
    val slotTimeCap: TimeCapWire,
)

@Serializable
sealed interface TimeCapWire {
    @Serializable
    @SerialName("unlimited")
    data object Unlimited : TimeCapWire

    @Serializable
    @SerialName("limited")
    data class Limited(val seconds: Int) : TimeCapWire
}

@Serializable
data class AdminAccount(
    val subject: String,
    val handle: HandleWire,
    val displayName: String,
    val plan: PlanWire,
    val isAdmin: Boolean,
    val status: AccountStatus,
    val createdAt: String,
    val projectCount: Int,
)

@Serializable
sealed interface AccountStatus {
    @Serializable
    @SerialName("active")
    data object Active : AccountStatus

    @Serializable
    @SerialName("deactivated")
    data class Deactivated(val at: String) : AccountStatus
}
