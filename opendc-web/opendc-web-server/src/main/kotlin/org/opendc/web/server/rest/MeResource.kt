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

import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.auth.changeProfile
import org.opendc.web.server.auth.deactivate
import org.opendc.web.server.model.BudgetPeriod
import org.opendc.web.server.model.BudgetWindow
import org.opendc.web.server.model.HandleKind
import org.opendc.web.server.model.Invoice
import org.opendc.web.server.model.PlanTier
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.SimulationCap
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.traces.TraceDisposal
import java.time.Instant

/** The caller's own account: the only endpoints an account still choosing its handle may use. */
@Path("me")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MeResource(
    private val identity: Identity,
    private val disposal: TraceDisposal,
) {
    @GET
    fun me(): UserProfile = identity.account().toProfile()

    @PUT
    @Path("profile")
    @Transactional
    fun updateProfile(change: ProfileChange): UserProfile {
        val account = identity.account()
        changeProfile(account, change.handle, change.displayName, disposal::discard)
        return account.toProfile()
    }

    /** Signs the caller out for good; see [deactivate] for what that leaves behind. */
    @DELETE
    @Transactional
    fun deactivateAccount(): Response {
        deactivate(identity.account(), Instant.now())
        return Response.noContent().build()
    }

    /** The caller's invoices, newest first. Read-only: they are issued outside this server. */
    @GET
    @Path("billing")
    fun billing(): Billing =
        Billing(
            invoices =
                Invoice.findByUser(identity.currentUser().id).map {
                    InvoiceWire(it.publicId.toString(), it.issuedAt.toString(), it.amountEur.toDouble(), it.paid)
                },
        )
}

private fun UserAccount.toProfile(): UserProfile =
    UserProfile(
        displayName = displayName,
        handle =
            when (handleKind) {
                HandleKind.PROVISIONAL -> HandleWire.Provisional
                HandleKind.CHOSEN -> HandleWire.Chosen(handle)
            },
        plan = planTier.toWire(),
        isAdmin = isAdmin,
        projectCount = ProjectMember.count("user.id = ?1", id).toInt(),
        budgets = budgetsOf(this, Instant.now()),
    )

/** Where an account stands in each window. One with no windows, like anonymous mode's, is not metered. */
private fun budgetsOf(
    user: UserAccount,
    now: Instant,
): List<BudgetWindowWire> {
    val windows = BudgetWindow.findByUser(user.id)
    if (windows.isEmpty()) {
        return emptyList()
    }
    val reserved = RunUnit.reservedBy(user.id)
    return windows.map { window ->
        BudgetWindowWire(
            period =
                when (window.period) {
                    BudgetPeriod.SESSION -> BudgetPeriodWire.SESSION
                    BudgetPeriod.WEEK -> BudgetPeriodWire.WEEK
                },
            usedSeconds = window.usedAt(now),
            reservedSeconds = reserved,
            cap = window.cap,
            resetsAt = (if (now.isBefore(window.resetsAt)) window.resetsAt else now.plus(window.period.length)).toString(),
        )
    }
}

@Serializable
data class ProfileChange(
    val handle: String,
    val displayName: String,
)

/** The name other people see, or that there is none yet: a first sign-in has to choose one. */
@Serializable
sealed interface HandleWire {
    @Serializable
    @SerialName("provisional")
    data object Provisional : HandleWire

    @Serializable
    @SerialName("chosen")
    data class Chosen(val name: String) : HandleWire
}

@Serializable
enum class PlanWire {
    @SerialName("free")
    FREE,

    @SerialName("education")
    EDUCATION,

    @SerialName("enterprise")
    ENTERPRISE,
}

fun PlanTier.toWire(): PlanWire =
    when (this) {
        PlanTier.FREE -> PlanWire.FREE
        PlanTier.EDUCATION -> PlanWire.EDUCATION
        PlanTier.ENTERPRISE -> PlanWire.ENTERPRISE
    }

/** The windows an account is metered over: a weekly allowance, and the session a sitting is charged to. */
@Serializable
enum class BudgetPeriodWire {
    @SerialName("session")
    SESSION,

    @SerialName("week")
    WEEK,
}

@Serializable
data class BudgetWindowWire(
    val period: BudgetPeriodWire,
    val usedSeconds: Double,
    val reservedSeconds: Double,
    val cap: SimulationCap,
    val resetsAt: String,
)

@Serializable
data class UserProfile(
    val displayName: String,
    val handle: HandleWire,
    val plan: PlanWire,
    val isAdmin: Boolean,
    val projectCount: Int,
    val budgets: List<BudgetWindowWire>,
)

@Serializable
data class InvoiceWire(
    val id: String,
    val issuedAt: String,
    val amountEur: Double,
    val paid: Boolean,
)

@Serializable
data class Billing(
    val invoices: List<InvoiceWire>,
)
