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

import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import jakarta.enterprise.context.RequestScoped
import org.opendc.web.server.model.AccountState
import org.opendc.web.server.model.HandleKind
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.rest.forbidden
import org.opendc.web.server.rest.notAuthenticated

/**
 * The kinds of caller. A role says what a caller is, not what state its account is in, since a failed
 * role check cannot say why and the frontend has to tell a deactivated account from a signed-out one.
 */
object Roles {
    /** Any person's account. What every endpoint requires unless it says otherwise. */
    const val USER = "user"

    /** An active account flagged as an administrator. */
    const val ADMIN = "admin"

    /** A launcher reporting on the execution its token was minted for. */
    const val EXECUTION = "execution"
}

/** The account a security identity acts as, by key: entities do not travel between transactions. */
const val ACCOUNT_ATTRIBUTE = "opendc.account"

/** The execution a launcher's identity reports on. */
const val EXECUTION_ATTRIBUTE = "opendc.execution"

fun QuarkusSecurityIdentity.Builder.actingAs(account: UserAccount): QuarkusSecurityIdentity.Builder {
    setAnonymous(false)
    setPrincipal(QuarkusPrincipal(account.handle))
    addRole(Roles.USER)
    if (account.isAdmin && account.state == AccountState.ACTIVE) {
        addRole(Roles.ADMIN)
    }
    addAttribute(ACCOUNT_ATTRIBUTE, account.id)
    return this
}

/** The account a request acts as, loaded once and checked for the states a role cannot express. */
@RequestScoped
class Identity(private val security: SecurityIdentity) {
    private val loaded by lazy(LazyThreadSafetyMode.NONE) { load() }

    /** Any account that is not deactivated, including one still choosing its handle. Only `/me` uses this. */
    fun account(): UserAccount = loaded

    /** An account that has chosen its handle, which is what every endpoint but `/me` acts as. */
    fun currentUser(): UserAccount {
        if (loaded.handleKind == HandleKind.PROVISIONAL) {
            throw forbidden("Choose a handle first")
        }
        return loaded
    }

    private fun load(): UserAccount {
        val id = security.getAttribute<Long>(ACCOUNT_ATTRIBUTE) ?: throw notAuthenticated()
        val account = UserAccount.findById(id) ?: throw notAuthenticated()
        if (account.state == AccountState.DEACTIVATED) {
            throw forbidden("This account has been deactivated")
        }
        return account
    }
}
