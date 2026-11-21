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

package org.opendc.web.server.auth

import io.quarkus.narayana.jta.QuarkusTransaction
import org.opendc.web.server.model.AccessToken
import org.opendc.web.server.model.HandleKind
import org.opendc.web.server.model.PlanTier
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.ProjectRole
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.rest.DocumentIssue
import org.opendc.web.server.rest.conflict
import org.opendc.web.server.rest.validName
import org.opendc.web.server.service.openWindows
import java.time.Instant

/**
 * The account behind an OIDC [subject], created on first sight with a placeholder handle. Two first
 * requests can race to create it; the loser's insert fails on the unique subject and reads the winner's.
 */
fun signedIn(
    subject: String,
    admins: Set<String>,
    now: Instant,
): UserAccount {
    val attempt = { QuarkusTransaction.requiringNew().call { known(subject, admins) ?: provisionAccount(subject, admins, now) } }
    return try {
        attempt()
    } catch (e: RuntimeException) {
        QuarkusTransaction.requiringNew().call { known(subject, admins) } ?: throw e
    }
}

private fun known(
    subject: String,
    admins: Set<String>,
): UserAccount? {
    val account = UserAccount.findBySubject(subject) ?: return null
    if (subject in admins) {
        account.isAdmin = true
    }
    return account
}

fun provisionAccount(
    subject: String,
    admins: Set<String>,
    now: Instant,
): UserAccount {
    val account = UserAccount()
    account.subject = subject
    account.handle = placeholderHandle(subject)
    account.handleKind = HandleKind.PROVISIONAL
    account.displayName = account.handle
    account.planTier = PlanTier.FREE
    account.isAdmin = subject in admins
    account.createdAt = now
    account.persist()
    UserAccount.flush()
    openWindows(account, now)
    return account
}

/**
 * Sets the handle and display name. A chosen handle prefixes the account's traces, so it may change
 * only while none is finished; unfinished uploads are discarded rather than left with the old prefix.
 */
fun changeProfile(
    account: UserAccount,
    handle: String,
    displayName: String,
    discard: (Trace) -> Unit,
) {
    val chosen = validHandle(handle)
    val name = validName(displayName, "Display", path = "displayName")
    if (chosen != account.handle) {
        val owned = Trace.findOwnedBy(account.id)
        if (account.handleKind == HandleKind.CHOSEN && owned.any { it.isComplete() }) {
            throw conflict("Your handle prefixes traces you own", listOf(DocumentIssue("handle", "is in use by your traces")))
        }
        val holder = UserAccount.findByHandle(chosen)
        if (holder != null && holder.id != account.id) {
            throw conflict("That handle is taken", listOf(DocumentIssue("handle", "is taken")))
        }
        owned.forEach(discard)
        account.handle = chosen
    }
    account.handleKind = HandleKind.CHOSEN
    account.displayName = name
}

/**
 * Signs an account out for good: its tokens are revoked and it leaves every shared project, while its
 * solo projects and running work are left alone. The sole owner of a shared project must hand it over first.
 */
fun deactivate(
    account: UserAccount,
    now: Instant,
) {
    if (account.subject == IMPLICIT_SUBJECT) {
        throw conflict("The local account of anonymous mode cannot be deactivated")
    }
    val memberships = ProjectMember.findByUser(account.id)
    val shared = memberships.filter { ProjectMember.count("project.id = ?1", it.project.id) > 1 }
    val stranded =
        shared.filter {
            it.role == ProjectRole.OWNER && ProjectMember.count("project.id = ?1 AND role = ?2", it.project.id, ProjectRole.OWNER) == 1L
        }
    if (stranded.isNotEmpty()) {
        throw conflict(
            "You are the only owner of projects other people work in",
            stranded.map { DocumentIssue("projects", it.project.name) },
        )
    }
    AccessToken.delete("user.id = ?1", account.id)
    shared.forEach { it.delete() }
    account.deactivate(now)
}
